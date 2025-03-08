package com.sportsnotification.broker;

import com.sportsnotification.dto.Packet;
import com.sportsnotification.dto.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    @Autowired
    private final BrokerService brokerService;
    private final RestTemplate restTemplate;

    public MessageProcessor(BrokerService brokerService, RestTemplate restTemplate) {
        this.brokerService = brokerService;
        this.restTemplate = restTemplate;
    }

    @Override
    public void run() {
        while (true) {
            try {
                ConcurrentLinkedQueue<Packet> messages = brokerService.getMessagesQueue();
                ConcurrentHashMap<String, List<Subscriber>> topicsSubscriber = brokerService.getTopicsSubscriberMap();

                if (!messages.isEmpty()) {
                    Packet message = messages.peek();
                    logger.info("Processing message: {}", message.getMessage());
                    List<Subscriber> subscribers = topicsSubscriber.get(message.getTopic());

                    if (subscribers != null) {
                        for (Subscriber subscriber : subscribers) {
                            logger.info("Sending message to subscriber: {}", subscriber.getConnectionUrl());
                            ResponseEntity<String> response = restTemplate.postForEntity(subscriber.getConnectionUrl() + "/subscriber/receive", message, String.class);

                            if (response.getStatusCode().is2xxSuccessful()) {
                                logger.info("Message sent successfully to: {}", subscriber.getConnectionUrl());
                            } else {
                                logger.warn("Failed to send message to: {} with status code: {}", subscriber.getConnectionUrl(), response.getStatusCode());
                            }
                        }
                    }
                    messages.poll();
                    brokerService.replicateMessageToAllBrokers(messages);
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("Error processing messages: {}", e.getMessage(), e);
            }
        }
    }
}