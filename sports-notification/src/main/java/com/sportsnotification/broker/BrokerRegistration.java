package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Profile("broker")
@Component
public class BrokerRegistration {

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
        UUID uuid = UUID.randomUUID();
        int brokerId = uuid.hashCode() & 0x7fffffff;
        currentBroker = new Broker();
        currentBroker.setId(brokerId);
        currentBroker.setPort(brokerPort);
        if(isLocal){
            currentBroker.setConnectionUrl( brokerUrl + ":" + brokerPort);
        }else {
            currentBroker.setConnectionUrl( brokerUrl + ":" + brokerPort);
        }

        ResponseEntity<Broker[]> response = restTemplate.postForEntity(coordinatorUrl + "/coordinator/register", currentBroker, Broker[].class);
        Broker[] brokers = response.getBody();
        if (brokers != null) {
            CopyOnWriteArrayList<Broker> brokerList = new CopyOnWriteArrayList<>();
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

    public Broker getCurrentBroker() {
        return this.currentBroker;
    }

}

