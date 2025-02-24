package coordinator;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("coordinator")
@RestController
@RequestMapping("/coordinator")
public class CoordinatorController {
    private final List<Broker> brokers = new ArrayList<>();
    private final Broker leaderBroker = new Broker();

    @PostMapping("/register")
    public Broker register(@RequestBody Broker broker) {

        if brokers.isEmpty()) {
            leaderBroker.setHost(broker.getHost());
            leaderBroker.setPort(broker.getPort());
            brokers.add(broker);
        }
        else {
            brokers.add(broker);
            return leaderBroker;
        }
        return leaderBroker;
    }

    @PostMapping("/heartbeat")
    public Heartbeat heartbeat(@RequestBody Broker broker) {
        return brokerService.heartbeat(broker);
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
