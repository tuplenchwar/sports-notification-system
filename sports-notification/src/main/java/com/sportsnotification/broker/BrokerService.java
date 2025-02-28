package com.sportsnotification.broker;

import com.sportsnotification.dto.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BrokerService {
    private final CopyOnWriteArrayList<Broker> brokers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Publisher> publishers = new CopyOnWriteArrayList<>();
    private final ConcurrentSkipListSet<String> topics = new ConcurrentSkipListSet<>();
    @Getter
    private final ConcurrentHashMap<String, List<Subscriber>> topicsSubscriber = new ConcurrentHashMap<>(); // Broadcast thread
    @Getter
    private final ConcurrentLinkedQueue<Packet> messages = new ConcurrentLinkedQueue<>();

    @Autowired
    @Lazy
    private BrokerRegistration brokerRegistration;

    @Autowired
    private RestTemplate restTemplate;

    private Thread messageProcessingThread;

    public ConcurrentSkipListSet<String> getAllTopics() {
        return topics;
    }

    public void registerPublisher(Publisher publisher) {
        if (publisher.getConnectionUrl() == null) {
            throw new IllegalArgumentException("Connection URL cannot be null");
        }
        publishers.add(publisher);
    }

    public void registerSubscriber(Subscriber subscriber) {
        if (subscriber.getConnectionUrl() == null) {
            throw new IllegalArgumentException("Connection URL cannot be null");
        }
        subscribers.add(subscriber);
    }

    public ResponseEntity<String>  publishMessage(Packet message) {
        if (message.getTopic() == null || message.getMessage() == null) {
            throw new IllegalArgumentException("Topic and Message cannot be null");
        }

        if(!topics.contains(message.getTopic())) {
            topics.add(message.getTopic());
            replicateTopicsToAllBrokers(topics);
        }
        messages.add(message);
        replicateMessageToAllBrokers(messages);

        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Message successfully published.");
    }

    public ResponseEntity<String> updateMessages(ConcurrentLinkedQueue<Packet> messageQueue) {
            this.messages.clear();
            this.messages.addAll(messageQueue);
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Message successfully published.");
    }

    public void subscribeToTopic(Subscriber subscriber) {
        if (subscriber.getTopic() == null) {
            throw new IllegalArgumentException("Topic cannot be null");
        }
        if (topicsSubscriber.containsKey(subscriber.getTopic())) {
            topicsSubscriber.get(subscriber.getTopic()).add(subscriber);
        } else {
            List<Subscriber> subscribers = new ArrayList<>();
            subscribers.add(subscriber);
            topicsSubscriber.put(subscriber.getTopic(), subscribers);
        }
    }

    public void unsubscribeToTopic(Subscriber subscriber) {
        if (subscriber.getTopic() == null) {
            throw new IllegalArgumentException("Topic cannot be null");
        }
        if (topicsSubscriber.containsKey(subscriber.getTopic())) {
            topicsSubscriber.get(subscriber.getTopic()).remove(subscriber.getConnectionUrl());
        }
    }

    public void updateLeader(Broker newLeader) {
        Broker currentBroker = brokerRegistration.getCurrentBroker();
        if (newLeader.getId()== currentBroker.getId()) {
            currentBroker.setLeader(true);
            System.out.println("I am the leader");
            startMessageProcessingThread();
        } else {
            System.out.println("Leader is: " + newLeader.getId());
        }
    }

    public void updateBrokers(List<Broker> brokers) {
        this.brokers.clear();
        this.brokers.addAll(brokers);
    }

    public List<Broker> getBrokersList() {
        return brokers;
    }

    public void setBrokerList(List<Broker> brokers) {
        this.brokers.clear();
        this.brokers.addAll(brokers);
    }

    public ConcurrentLinkedQueue<Packet> getMessagesQueue(){
        return messages;
    }

    public ConcurrentHashMap<String, List<Subscriber>> getTopicsSubscriberMap(){
        return topicsSubscriber;
    }

    public ResponseEntity<String> replicateMessageToAllBrokers(ConcurrentLinkedQueue<Packet> messages) {
        for (Broker broker : brokers) {
            if (!broker.isLeader()) {
                String brokenUrl = broker.getConnectionUrl();
                // send messages to broker
                restTemplate.postForObject(brokenUrl + "/broker/replicatemessages", messages, String.class);
            }
        }
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Message replicate successfully.");
    }

    public ResponseEntity<String> replicateTopicsToAllBrokers(ConcurrentSkipListSet<String> topics) {
        for (Broker broker : brokers) {
            if (!broker.isLeader()) {
                String brokenUrl = broker.getConnectionUrl();
                // send topics to broker
                restTemplate.postForObject(brokenUrl + "/broker/replicatetopics", topics, String.class);

            }
        }
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Topic replicate successfully.");
    }

    public ResponseEntity<String> updateTopics(ConcurrentSkipListSet<String> topics) {
            this.topics.clear();
            this.topics.addAll(topics);
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Topic replicate successfully.");
    }

    private void startMessageProcessingThread() {
        if (messageProcessingThread != null && messageProcessingThread.isAlive()) {
            return; // Prevent multiple threads
        }

        MessageProcessor messageProcessor = new MessageProcessor(this, restTemplate);
        messageProcessingThread = new Thread(messageProcessor);
        messageProcessingThread.setDaemon(true);
        messageProcessingThread.start();
    }
}