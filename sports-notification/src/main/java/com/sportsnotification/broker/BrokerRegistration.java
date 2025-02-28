package com.sportsnotification.broker;

import com.sportsnotification.dto.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

@Profile("broker")
@Component
public class BrokerRegistration {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coordinator.url:http://127.0.0.1:8080}")
    private String coordinatorUrl;

    @Getter
    private Broker currentBroker;

    @Value("${server.port}")
    private Integer brokerPort;

    @Autowired
    private BrokerService brokerService;

    private Thread messageProcessingThread;

    @PostConstruct
    public void registerBroker() {
        UUID uuid = UUID.randomUUID();
        int brokerId = uuid.hashCode() & 0x7fffffff;
        currentBroker = new Broker();
        currentBroker.setId(brokerId);
        currentBroker.setPort(brokerPort);
        currentBroker.setConnectionUrl("http://127.0.0.1:" + brokerPort);

        ResponseEntity<Broker[]> response = restTemplate.postForEntity(coordinatorUrl + "/coordinator/register", currentBroker, Broker[].class);
        Broker[] brokers = response.getBody();
        if (brokers != null) {
            List<Broker> brokerList = new ArrayList<>();
            for (Broker broker : brokers) {
                if (broker.getId() == currentBroker.getId() && broker.isLeader()) {
                    startMessageProcessingThread();
                }
                brokerList.add(broker);
            }
            brokerService.setBrokerList(brokerList);
        }
    }

    private void startMessageProcessingThread() {
        if (messageProcessingThread != null && messageProcessingThread.isAlive()) {
            return; // Prevent multiple threads
        }

        MessageProcessor messageProcessor = new MessageProcessor(brokerService, restTemplate);
        messageProcessingThread = new Thread(messageProcessor);
        messageProcessingThread.setDaemon(true);
        messageProcessingThread.start();
    }
}

