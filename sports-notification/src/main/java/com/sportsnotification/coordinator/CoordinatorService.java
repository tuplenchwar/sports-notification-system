package com.sportsnotification.coordinator;

import com.sportsnotification.dto.*;
import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.*;
import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.concurrent.*;

@Profile("coordinator")
@Service
public class CoordinatorService {
    @Getter
    private final List<Broker> brokers = new CopyOnWriteArrayList<>();
    private Broker leaderBroker = new Broker();
    private final ConcurrentHashMap<Integer, Long> heartbeatMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        startHeartbeatMonitor();
    }

    public Broker register(Broker broker) {
        if (brokers.isEmpty()) {
            broker.setLeader(true);
            leaderBroker = broker;
        } else {
            broker.setLeader(false);
        }
        brokers.add(broker);
        return leaderBroker;
    }

    public void heartbeat(Heartbeat heartbeat) {
        heartbeatMap.put(heartbeat.getBrokers().getId(), System.currentTimeMillis());
    }

    public Broker getLeader() {
        return leaderBroker;
    }

    public List<Broker> getBrokers(){
        return brokers;
    }

    private void startHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            List<Integer> brokersToRemove = new ArrayList<>();

            for (Map.Entry<Integer, Long> entry : heartbeatMap.entrySet()) {
                int brokerId = entry.getKey();
                long lastHeartbeat = entry.getValue();

                if ((currentTime - lastHeartbeat) > 5000) { // 5 seconds timeout
                    brokersToRemove.add(brokerId);
                }
            }

            for (Integer brokerId : brokersToRemove) {
                Broker broker = removeBrokerById(brokerId);
                heartbeatMap.remove(brokerId);

                if (broker != null && broker.isLeader()) {
                    electNewLeader();
                }
            }
        }, 0, 3, TimeUnit.SECONDS); // Check every 3 seconds
    }

    private Broker removeBrokerById(int brokerId) {
        for (Broker broker : brokers) {
            if (broker.getId() == brokerId) {
                brokers.remove(broker);
                return broker;
            }
        }
        return null;
    }

    private void electNewLeader() {
        Optional<Broker> newLeaderOpt = brokers.stream()
                .max(Comparator.comparingInt(Broker::getId));

        newLeaderOpt.ifPresent(newLeader -> {
            newLeader.setLeader(true);
            leaderBroker = newLeader;
            notifyBrokersAboutNewLeader(newLeader);
        });
    }

    private void notifyBrokersAboutNewLeader(Broker newLeader) {
        for (Broker broker : brokers) {
                try {
                    String brokerUrl = broker.getConnectionUrl() + "/broker/update-leader";
                    System.out.println("New leader broker: " + broker.getId());
                    restTemplate.postForObject(brokerUrl, newLeader, Broker.class);
                } catch (Exception e) {
                    System.err.println("Failed to notify broker: " + broker.getId());
                }
        }
    }
}
