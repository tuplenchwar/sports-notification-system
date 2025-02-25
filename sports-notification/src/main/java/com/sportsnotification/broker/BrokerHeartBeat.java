package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.Heartbeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Profile("broker")
@Component
public class BrokerHeartBeat {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coordinator.url:http://localhost:8080}")
    private String coordinatorUrl;

    @Autowired
    private BrokerRegistration brokerRegistration; // To access the currentBroker

    @Scheduled(fixedRate = 3000)
    public void sendHeartbeat() {
        Broker currentBroker = brokerRegistration.getCurrentBroker();
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setBrokers(currentBroker);
        heartbeat.setHeartBeatTimestamp(System.currentTimeMillis());
        if (currentBroker != null) {
            restTemplate.postForLocation(coordinatorUrl + "/coordinator/heartbeat", heartbeat);
            System.out.println("Heartbeat sent at " + new Date());
        }
    }
}
