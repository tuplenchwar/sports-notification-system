package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.Heartbeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@Profile("broker")
@Component
public class BrokerHeartBeat {

    private static final Logger logger = LoggerFactory.getLogger(BrokerHeartBeat.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coordinator.url:docker logs -f coordinator}")
    private String coordinatorUrl;

    @Autowired
    private BrokerRegistration brokerRegistration;

    @Scheduled(fixedRate = 3000)
    public void sendHeartbeat() throws InterruptedException {
        try {
            Broker currentBroker = brokerRegistration.getCurrentBroker();
            if (currentBroker == null) {
                logger.warn("Current broker is null, skipping heartbeat.");
                return;
            }

            Heartbeat heartbeat = new Heartbeat();
            heartbeat.setBrokers(currentBroker);
            heartbeat.setHeartBeatTimestamp(System.currentTimeMillis());

            restTemplate.postForLocation(coordinatorUrl + "/coordinator/heartbeat", heartbeat);
            logger.info("Heartbeat sent at {} for broker: {}", new Date(), currentBroker);
        } catch (Exception e) {
            logger.warn("Unable to send heartbeat to Co-ordinator, retrying.... ");
            Thread.sleep(5000);
        }
    }
}
