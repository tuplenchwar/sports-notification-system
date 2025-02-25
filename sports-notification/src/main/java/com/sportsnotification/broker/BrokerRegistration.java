package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

import javax.annotation.PostConstruct;

@Profile("broker")
@Component
public class BrokerRegistration {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coordinator.url:http://localhost:8080}")
    private String coordinatorUrl;

    @Getter
    private Broker currentBroker;

    @Value("${server.port}")
    private Integer brokerPort;

    @PostConstruct
    public void registerBroker() {
        UUID uuid = UUID.randomUUID();
        int brokerId = uuid.hashCode() & 0x7fffffff;
        currentBroker = new Broker();
        currentBroker.setId(brokerId);
        currentBroker.setPort(brokerPort);
        currentBroker.setConnectionUrl("http://localhost:" + brokerPort);

        // Send the registration request
        Broker leader = restTemplate.postForObject(coordinatorUrl + "/coordinator/register", currentBroker, Broker.class);
        System.out.println("Leader broker: " + leader);
    }

}

