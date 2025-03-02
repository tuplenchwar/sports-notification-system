package com.sportsnotification.broker;

import com.sportsnotification.dto.Packet;
import com.sportsnotification.dto.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageProcessor implements Runnable {

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
                    System.out.println("Processing message: " + message.getMessage());
                    List<Subscriber> subscribers = topicsSubscriber.get(message.getTopic());
                    if (subscribers != null) {
                        for (Subscriber subscriber : subscribers) {
                            System.out.println("Sending message to subscriber: " + subscriber.getConnectionUrl());
                            ResponseEntity<String> response = restTemplate.postForEntity(subscriber.getConnectionUrl() + "/subscriber/receive", message, String.class);
                            if(response.getStatusCode().is2xxSuccessful()) {
                                System.out.println("Message sent successfully");
                            } else {
                                System.out.println("Failed to send message");
                            }
                        }
                    }
                    messages.poll();
                    brokerService.replicateMessageToAllBrokers(messages);
                }
                Thread.sleep(1000); // Adjust delay as needed
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
