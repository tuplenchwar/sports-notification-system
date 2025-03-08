package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Profile("broker")
@Component
public class BrokerRegistration {

    private static final Logger logger = LoggerFactory.getLogger(BrokerRegistration.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coordinator.url:http://127.0.0.1:8080}")
    private String coordinatorUrl;

    private Broker currentBroker;

    @Value("${server.port:8090}")
    private Integer brokerPort;

    @Value("${broker.url:http://127.0.0.1}")
    private String brokerUrl;

    @Autowired
    private BrokerService brokerService;

    @Value("${isLocal:false}")
    private boolean isLocal;

    private Thread messageProcessingThread;

    @PostConstruct
    public void registerBroker() {
        try {
            UUID uuid = UUID.randomUUID();
            int brokerId = uuid.hashCode() & 0x7fffffff;
            currentBroker = new Broker();
            currentBroker.setId(brokerId);
            currentBroker.setPort(brokerPort);
            currentBroker.setConnectionUrl(brokerUrl + ":" + brokerPort);

            logger.info("Registering broker with ID: {} at URL: {}", brokerId, currentBroker.getConnectionUrl());

            ResponseEntity<Broker[]> response = restTemplate.postForEntity(coordinatorUrl + "/coordinator/register", currentBroker, Broker[].class);
            Broker[] brokers = response.getBody();

            if (brokers != null) {
                CopyOnWriteArrayList<Broker> brokerList = new CopyOnWriteArrayList<>();
                for (Broker broker : brokers) {
                    if (broker.getId() == currentBroker.getId() && broker.isLeader()) {
                        logger.info("Broker {} is a leader. Starting message processing thread.", brokerId);
                        startMessageProcessingThread();
                    }
                    brokerList.add(broker);
                }
                brokerService.setBrokerList(brokerList);
                logger.info("Broker registration completed successfully.");
            }
        } catch (Exception e) {
            logger.error("Error registering broker: {}", e.getMessage(), e);
        }
    }

    private void startMessageProcessingThread() {
        try {
            if (messageProcessingThread != null && messageProcessingThread.isAlive()) {
                logger.warn("Message processing thread is already running.");
                return; // Prevent multiple threads
            }

            logger.info("Starting message processing thread.");
            MessageProcessor messageProcessor = new MessageProcessor(brokerService, restTemplate);
            messageProcessingThread = new Thread(messageProcessor);
            messageProcessingThread.setDaemon(true);
            messageProcessingThread.start();
        } catch (Exception e) {
            logger.error("Error starting message processing thread: {}", e.getMessage(), e);
        }
    }

    public Broker getCurrentBroker() {
        return this.currentBroker;
    }
}
