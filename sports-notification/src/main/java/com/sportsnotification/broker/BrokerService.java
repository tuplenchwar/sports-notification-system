package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.Packet;
import com.sportsnotification.dto.Publisher;
import com.sportsnotification.dto.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BrokerService {
    private final CopyOnWriteArrayList<Broker> brokers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Publisher> publishers = new CopyOnWriteArrayList<>();
    private final ConcurrentSkipListSet<String> topics = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<String, List<Subscriber>> topicsSubscriber = new ConcurrentHashMap<>(); // Broadcast thread
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

    public ResponseEntity<String>  registerSubscriber(Subscriber subscriber) {
        if(!isSubscriberAlreadyExist(subscriber.getConnectionUrl())) {
            if (subscriber.getConnectionUrl() == null) {
                throw new IllegalArgumentException("Connection URL cannot be null");
            }
            subscribers.add(subscriber);
            System.out.println("Registered Subscriber Id : " + subscriber.getId());
            replicateSubscribersToAllBrokers(subscribers);
            // Return HTTP 200 OK status with a success message
            return ResponseEntity.ok("Subscriber registered successfully.");
        }
        return ResponseEntity.ok("Subscriber Already Registered.");
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
        System.out.println("I am the leader - I recieved a message and I will process it - " + message.getMessage());
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

    public ResponseEntity<String> subscribeToTopic(Subscriber subscriber) {
        if(isSubscriberValid(subscriber.getConnectionUrl())) {
            if (subscriber.getTopic() == null) {
                throw new IllegalArgumentException("Topic cannot be null");
            }
            if (topicsSubscriber.containsKey(subscriber.getTopic())) {
                System.out.println("Subscriber Id : " + subscriber.getId());
                System.out.println("Subscribed to topic: " + subscriber.getTopic());
                topicsSubscriber.get(subscriber.getTopic()).add(subscriber);
            } else {
                List<Subscriber> subscribers = new ArrayList<>();
                subscribers.add(subscriber);
                System.out.println("Subscriber Id : " + subscriber.getId());
                System.out.println("Subscribed to topic: " + subscriber.getTopic());
                topicsSubscriber.put(subscriber.getTopic(), subscribers);
            }
        }
        else {
            System.out.println("Invalid Subscriber Id : " + subscriber.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 if subscriber is not valid
        }
        replicateTopicsToSubscribersToAllBrokers(topicsSubscriber);
        return ResponseEntity.ok("Subscriber subscribe to topic successfully.");
    }

    public ResponseEntity<String> unsubscribeToTopic(Subscriber subscriber) {
        if(isSubscriberValid(subscriber.getConnectionUrl())) {
            if (subscriber.getTopic() == null) {
                throw new IllegalArgumentException("Topic cannot be null");
            }
            if (topicsSubscriber.containsKey(subscriber.getTopic())) {
                List<Subscriber> subscribers = topicsSubscriber.get(subscriber.getTopic());
                for (Subscriber sub : subscribers) {
                    if (Objects.equals(sub.getConnectionUrl(), subscriber.getConnectionUrl())) {
                        subscribers.remove(sub);
                        break;
                    }
                }
                System.out.println("Subscriber Id removed: " + subscriber.getConnectionUrl());
                topicsSubscriber.put(subscriber.getTopic(), subscribers);
            }
        }
        else {
            System.out.println("Invalid Subscriber Id : " + subscriber.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 if subscriber is not valid
        }
        replicateTopicsToSubscribersToAllBrokers(topicsSubscriber);
        return ResponseEntity.ok("Subscriber unsubscribe to topic successfully.");
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

    public ResponseEntity<String> replicateSubscribersToAllBrokers(CopyOnWriteArrayList<Subscriber> subscribers) {
        for (Broker broker : brokers) {
            if (!broker.isLeader()) {
                String brokenUrl = broker.getConnectionUrl();
                // send subscribers to broker
                restTemplate.postForObject(brokenUrl + "/broker/replicatesubscribers", subscribers, String.class);
            }
        }
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Subscriber replicate successfully.");
    }

    public ResponseEntity<String> replicateTopicsToSubscribersToAllBrokers(ConcurrentHashMap<String, List<Subscriber>> topicsToSubscribers) {
        for (Broker broker : brokers) {
            if (!broker.isLeader()) {
                String brokenUrl = broker.getConnectionUrl();
                // send topics to subscribers to broker
                restTemplate.postForObject(brokenUrl + "/broker/replicatetopicstosubscribers", topicsToSubscribers, String.class);
            }
        }
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Topic to Subscriber replicate successfully.");
    }

    public ResponseEntity<String> updateTopics(ConcurrentSkipListSet<String> topics) {
            this.topics.clear();
            this.topics.addAll(topics);
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Topic replicate successfully.");
    }

    public ResponseEntity<String> updateSubscribers(CopyOnWriteArrayList<Subscriber> subscribers) {
            this.subscribers.clear();
            this.subscribers.addAll(subscribers);
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Subscriber replicate successfully.");
    }

    public ResponseEntity<String> updateTopicsToSubscribers(ConcurrentHashMap<String, List<Subscriber>> topicsToSubscribers) {
            this.topicsSubscriber.clear();
            this.topicsSubscriber.putAll(topicsToSubscribers);
        // Return HTTP 200 OK status with a success message
        return ResponseEntity.ok("Topic to Subscriber replicate successfully.");
    }

    public boolean isSubscriberValid(String subscriberConnectionURL) {
        for (Subscriber subscriber : subscribers) {
            if (subscriber.getConnectionUrl().equals(subscriberConnectionURL)) {
                return true;
            }
        }
        return false;
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

    private boolean isSubscriberAlreadyExist(String subscriberConnectionURL) {
        for (Subscriber subscriber : subscribers) {
            if (subscriber.getConnectionUrl().equals(subscriberConnectionURL)) {
                return true;
            }
        }
        return false;
    }

    public ConcurrentHashMap<String, List<Subscriber>> getTopicsSubscriber() {
        return this.topicsSubscriber;
    }

    public ConcurrentLinkedQueue<Packet> getMessages() {
        return this.messages;
    }
}