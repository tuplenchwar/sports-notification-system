package com.sportsnotification.broker;

import com.sportsnotification.dto.*;
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
        currentBroker.setConnectionUrl("http://localhost:" + brokerPort);

        // Send the registration request
        Broker regResponse = restTemplate.postForObject(coordinatorUrl + "/coordinator/register", currentBroker, Broker.class);
        if(regResponse.isLeader()){
            startMessageProcessingThread();
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

