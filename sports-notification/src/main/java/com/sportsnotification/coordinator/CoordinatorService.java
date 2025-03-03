package com.sportsnotification.coordinator;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.CoordinatorSyncData;
import com.sportsnotification.dto.Heartbeat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import javax.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Profile("coordinator")
@Service
@EnableAsync
public class CoordinatorService {
    private final List<Broker> brokers = new ArrayList<>();
    private Broker leaderBroker = new Broker();
    private final ConcurrentHashMap<Integer, Long> brokerHeartbeatMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final RestTemplate restTemplate = new RestTemplate();

    //DS for coordinator
    private boolean isPrimary = false;

    //Run locally
    @Value("${isLocal:false}")
    private boolean isLocal;


    @PostConstruct
    public void init() {
        if(isLocal) {
            startHeartbeatMonitor(); // Keeps track of broker health
        }else{
            determinePrimary(); // Assigns role (primary/secondary)
            startHeartbeatMonitor(); // Keeps track of broker health
            if (isPrimary) {
                waitForSecondaryAndSync(); // Sync with secondary only if primary
            }
        }
    }

    public List<Broker> register(Broker broker) {
        if (brokers.isEmpty()) {
            broker.setLeader(true);
            leaderBroker = broker;
        } else {
            broker.setLeader(false);
        }
        brokers.add(broker);
        sendBrokersListToAllBrokersAsync(broker);
        return brokers;
    }

    public void heartbeat(Heartbeat heartbeat) {
        brokerHeartbeatMap.put(heartbeat.getBrokers().getId(), System.currentTimeMillis());
    }

    public Broker getLeader() {
        return leaderBroker;
    }

    public List<Broker> getBrokers(){
        return brokers;
    }

    public void syncData(CoordinatorSyncData syncData) {
        if (!isPrimary) { // Ensure only the secondary updates its state
            this.brokers.clear();
            this.brokers.addAll(syncData.getBrokers());

            this.leaderBroker = syncData.getLeaderBroker();
            this.brokerHeartbeatMap.clear();
            this.brokerHeartbeatMap.putAll(syncData.getBrokerHeartbeatMap());

            System.out.println("Secondary coordinator data is synced.");
        }
    }

    public boolean isPrimaryCoordinator() {
        return isPrimary;
    }

    private void startHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            List<Integer> brokersToRemove = new ArrayList<>();

            for (Map.Entry<Integer, Long> entry : brokerHeartbeatMap.entrySet()) {
                int brokerId = entry.getKey();
                long lastHeartbeat = entry.getValue();

                if ((currentTime - lastHeartbeat) > 5000) { // 5 seconds timeout
                    brokersToRemove.add(brokerId);
                }
            }

            for (Integer brokerId : brokersToRemove) {
                Broker broker = removeBrokerById(brokerId);
                brokerHeartbeatMap.remove(brokerId);
                // removeBrokerById(brokerId);
                if (broker != null && broker.isLeader()) {
                    electNewLeader();
                } else {
                    sendBrokersListToAllBrokersAsync(broker);
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
                    restTemplate.put(brokerUrl, newLeader, Broker.class);
                    System.out.println("New leader broker: " + broker.getId());
                    sendBrokersListToAllBrokersAsync(null);
                } catch (Exception e) {
                    System.err.println("Failed to notify broker: " + broker.getId());
                }
        }
    }

    private void determinePrimary() {
        String instanceId = getInstanceId();
        if (instanceId == null) {
            System.err.println("Failed to fetch instance ID.");
            return;
        }

        String instanceTag = getInstanceTag(instanceId);
        if ("Primary-Coordinator".equalsIgnoreCase(instanceTag)) {
            isPrimary = true;
        } else if ("Secondary-Coordinator".equalsIgnoreCase(instanceTag)) {
            isPrimary = false;
        }

        System.out.println("Running on instance " + instanceId + " with tag: " + instanceTag);
        System.out.println("isPrimary: " + isPrimary);
    }

    private String getInstanceId() {
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                return scanner.nextLine();
            }
        } catch (Exception e) {
            System.err.println("Error retrieving instance ID: " + e.getMessage());
            return null;
        }
    }

    private String getInstanceTag(String instanceId) {
        try (Ec2Client ec2Client = Ec2Client.create()) {
            DescribeInstancesResponse response = ec2Client.describeInstances(
                    DescribeInstancesRequest.builder().instanceIds(instanceId).build()
            );

            List<Instance> instances = response.reservations().get(0).instances();
            for (Instance instance : instances) {
                for (Tag tag : instance.tags()) {
                    if ("Name".equalsIgnoreCase(tag.key())) {
                        return tag.value();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving instance tag: " + e.getMessage());
        }
        return null;
    }

    private void waitForSecondaryAndSync() {
        scheduler.scheduleAtFixedRate(() -> {
            String secondaryIp = getSecondaryCoordinatorIp();
            if (secondaryIp != null) {
                startSyncingWithSecondary(secondaryIp);
            } else {
                System.err.println("No secondary coordinator found.");
            }
        }, 0, 5, TimeUnit.SECONDS); // Check every 5 seconds
    }

    private String getSecondaryCoordinatorIp() {
        try (Ec2Client ec2Client = Ec2Client.create()) {
            DescribeInstancesResponse response = ec2Client.describeInstances(DescribeInstancesRequest.builder().build());

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    for (Tag tag : instance.tags()) {
                        if ("Name".equalsIgnoreCase(tag.key()) && "Secondary-Coordinator".equalsIgnoreCase(tag.value())) {
                            return instance.publicIpAddress(); // Return the Secondary Coordinator's IP
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving Secondary Coordinator IP: " + e.getMessage());
        }
        return null;
    }

    private void startSyncingWithSecondary(String secondaryIp) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String secondaryUrl = "http://" + secondaryIp + ":8080/coordinator/sync-data";
                CoordinatorSyncData syncData = new CoordinatorSyncData(brokers, leaderBroker, brokerHeartbeatMap);
                restTemplate.postForEntity(secondaryUrl, syncData, String.class);
                System.out.println("Synced data with secondary: " + secondaryIp);
            } catch (Exception e) {
                System.err.println("Failed to sync data with secondary: " + secondaryIp);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }


    @Async
    protected void sendBrokersListToAllBrokersAsync(Broker skipBroker) {
        for (Broker broker : brokers) {
            if (skipBroker != null && broker.getId() == skipBroker.getId()) {
                continue; // Skip notifying the broker immediately
            }
            try {
                    String brokerUrl = broker.getConnectionUrl() + "/broker/update-brokers";
                    restTemplate.put(brokerUrl, brokers, List.class);
                    System.out.println("Updated brokers list for broker: " + broker.getId());
                } catch (Exception e) {
                    System.err.println("Failed to update brokers list for broker: " + broker.getId());
            }
        }
    }

}
