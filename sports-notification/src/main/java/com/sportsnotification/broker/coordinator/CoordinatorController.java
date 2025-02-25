package coordinator;

import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import dto.*;

@Profile("coordinator")
@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {
    private final List<Broker> brokers = new ArrayList<>();
    private final Broker leaderBroker = new Broker();
    private final ConcurrentHashMap<Integer, Timestamp> heartbeatMap = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public Broker register(@RequestBody Broker broker) {

        if (brokers.isEmpty()) {
            leaderBroker.setConnectionUrl(broker.getConnectionUrl());
            leaderBroker.setPort(broker.getPort());
            brokers.add(broker);
        } else {
            brokers.add(broker);
            return leaderBroker;
        }
        return leaderBroker;
    }

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody Broker broker) {
        if (!heartbeatMap.containsKey(broker.getId())) {
            heartbeatMap.put(broker.getId(), new Timestamp(System.currentTimeMillis()));
        } else {
            heartbeatMap.put(broker.getId(), new Timestamp(System.currentTimeMillis()));
        }
    }

    @GetMapping("/brokers")
    public List<Broker> getBrokers() {
        return brokers;
    }

    @GetMapping("/leader")
    public Broker getLeader() {
        return leaderBroker;
    }
}

