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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BrokerService {
    private static final Logger logger = LoggerFactory.getLogger(BrokerService.class);
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
        try {
            if (!isSubscriberAlreadyExist(subscriber.getConnectionUrl())) {
                if (subscriber.getConnectionUrl() == null) {
                    throw new IllegalArgumentException("Connection URL cannot be null");
                }
                subscribers.add(subscriber);
                logger.info("Registered Subscriber Id: {}", subscriber.getId());
                replicateSubscribersToAllBrokers(subscribers);
                return ResponseEntity.ok("Subscriber registered successfully.");
            }
            return ResponseEntity.ok("Subscriber Already Registered.");
        } catch (Exception e) {
            logger.error("Error registering subscriber: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error registering subscriber.");
        }
    }

    public ResponseEntity<String>  publishMessage(Packet message) {
        try {
            if (message.getTopic() == null || message.getMessage() == null) {
                throw new IllegalArgumentException("Topic and Message cannot be null");
            }

            if (!topics.contains(message.getTopic())) {
                topics.add(message.getTopic());
                replicateTopicsToAllBrokers(topics);
            }
            messages.add(message);
            logger.info("I am the leader - I received a message and I will process it - {}", message.getMessage());
            replicateMessageToAllBrokers(messages);

            return ResponseEntity.ok("Message successfully published.");
        } catch (Exception e) {
            logger.error("Error publishing message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error publishing message.");
        }
    }

    public ResponseEntity<String> updateMessages(ConcurrentLinkedQueue<Packet> messageQueue) {
        try {
            this.messages.clear();
            this.messages.addAll(messageQueue);
            return ResponseEntity.ok("Message successfully published.");
        } catch (Exception e) {
            logger.error("Error updating messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating messages.");
        }
    }

    public ResponseEntity<String> subscribeToTopic(Subscriber subscriber) {
        try {
            if (isSubscriberValid(subscriber.getConnectionUrl())) {
                if (subscriber.getTopic() == null) {
                    throw new IllegalArgumentException("Topic cannot be null");
                }
                if (topicsSubscriber.containsKey(subscriber.getTopic())) {
                    logger.info("Subscriber Id: {}", subscriber.getId());
                    logger.info("Subscribed to topic: {}", subscriber.getTopic());
                    topicsSubscriber.get(subscriber.getTopic()).add(subscriber);
                } else {
                    List<Subscriber> subscribers = new ArrayList<>();
                    subscribers.add(subscriber);
                    logger.info("Subscriber Id: {}", subscriber.getId());
                    logger.info("Subscribed to topic: {}", subscriber.getTopic());
                    topicsSubscriber.put(subscriber.getTopic(), subscribers);
                }
            } else {
                logger.warn("Invalid Subscriber Id: {}", subscriber.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            replicateTopicsToSubscribersToAllBrokers(topicsSubscriber);
            return ResponseEntity.ok("Subscriber subscribe to topic successfully.");
        } catch (Exception e) {
            logger.error("Error subscribing to topic: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error subscribing to topic.");
        }
    }

    public ResponseEntity<String> unsubscribeToTopic(Subscriber subscriber) {
        try {
            if (isSubscriberValid(subscriber.getConnectionUrl())) {
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
                    logger.info("Subscriber Id removed: {}", subscriber.getConnectionUrl());
                    topicsSubscriber.put(subscriber.getTopic(), subscribers);
                }
            } else {
                logger.warn("Invalid Subscriber Id: {}", subscriber.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            replicateTopicsToSubscribersToAllBrokers(topicsSubscriber);
            return ResponseEntity.ok("Subscriber unsubscribe to topic successfully.");
        } catch (Exception e) {
            logger.error("Error unsubscribing to topic: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error unsubscribing to topic.");
        }
    }

    public void updateLeader(Broker newLeader) {
        try {
            Broker currentBroker = brokerRegistration.getCurrentBroker();
            if (newLeader.getId() == currentBroker.getId()) {
                currentBroker.setLeader(true);
                logger.info("I am the leader");
                startMessageProcessingThread();
            } else {
                logger.info("Leader is: {}", newLeader.getId());
            }
        } catch (Exception e) {
            logger.error("Error updating leader: {}", e.getMessage());
        }
    }

    public void updateBrokers(List<Broker> brokers) {
        try {
            this.brokers.clear();
            this.brokers.addAll(brokers);
        } catch (Exception e) {
            logger.error("Error updating brokers: {}", e.getMessage());
        }
    }

    public List<Broker> getBrokersList() {
        return brokers;
    }

    public void setBrokerList(List<Broker> brokers) {
        try {
            this.brokers.clear();
            this.brokers.addAll(brokers);
        } catch (Exception e) {
            logger.error("Error setting broker list: {}", e.getMessage());
        }
    }
    public ConcurrentLinkedQueue<Packet> getMessagesQueue(){
        return messages;
    }

    public ConcurrentHashMap<String, List<Subscriber>> getTopicsSubscriberMap(){
        return topicsSubscriber;
    }

    public ResponseEntity<String> replicateMessageToAllBrokers(ConcurrentLinkedQueue<Packet> messages) {
        try {
            for (Broker broker : brokers) {
                if (!broker.isLeader()) {
                    String brokenUrl = broker.getConnectionUrl();
                    restTemplate.postForObject(brokenUrl + "/broker/replicatemessages", messages, String.class);
                }
            }
            return ResponseEntity.ok("Message replicate successfully.");
        } catch (Exception e) {
            logger.error("Error replicating messages to all brokers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating messages.");
        }
    }

    public ResponseEntity<String> replicateTopicsToAllBrokers(ConcurrentSkipListSet<String> topics) {
        try {
            for (Broker broker : brokers) {
                if (!broker.isLeader()) {
                    String brokenUrl = broker.getConnectionUrl();
                    restTemplate.postForObject(brokenUrl + "/broker/replicatetopics", topics, String.class);
                }
            }
            return ResponseEntity.ok("Topic replicate successfully.");
        } catch (Exception e) {
            logger.error("Error replicating topics to all brokers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating topics.");
        }
    }

    public ResponseEntity<String> replicateSubscribersToAllBrokers(CopyOnWriteArrayList<Subscriber> subscribers) {
        try {
            for (Broker broker : brokers) {
                if (!broker.isLeader()) {
                    String brokenUrl = broker.getConnectionUrl();
                    restTemplate.postForObject(brokenUrl + "/broker/replicatesubscribers", subscribers, String.class);
                }
            }
            return ResponseEntity.ok("Subscriber replicate successfully.");
        } catch (Exception e) {
            logger.error("Error replicating subscribers to all brokers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating subscribers.");
        }
    }

    public ResponseEntity<String> replicateTopicsToSubscribersToAllBrokers(ConcurrentHashMap<String, List<Subscriber>> topicsToSubscribers) {
        try {
            for (Broker broker : brokers) {
                if (!broker.isLeader()) {
                    String brokenUrl = broker.getConnectionUrl();
                    restTemplate.postForObject(brokenUrl + "/broker/replicatetopicstosubscribers", topicsToSubscribers, String.class);
                }
            }
            return ResponseEntity.ok("Topic to Subscriber replicate successfully.");
        } catch (Exception e) {
            logger.error("Error replicating topics to subscribers to all brokers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating topics to subscribers.");
        }
    }

    public ResponseEntity<String> updateTopics(ConcurrentSkipListSet<String> topics) {
        try {
            this.topics.clear();
            this.topics.addAll(topics);
            return ResponseEntity.ok("Topic replicate successfully.");
        } catch (Exception e) {
            logger.error("Error updating topics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating topics.");
        }
    }

    public ResponseEntity<String> updateSubscribers(CopyOnWriteArrayList<Subscriber> subscribers) {
        try {
            this.subscribers.clear();
            this.subscribers.addAll(subscribers);
            return ResponseEntity.ok("Subscriber replicate successfully.");
        } catch (Exception e) {
            logger.error("Error updating subscribers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating subscribers.");
        }
    }

    public ResponseEntity<String> updateTopicsToSubscribers(ConcurrentHashMap<String, List<Subscriber>> topicsToSubscribers) {
        try {
            this.topicsSubscriber.clear();
            this.topicsSubscriber.putAll(topicsToSubscribers);
            return ResponseEntity.ok("Topic to Subscriber replicate successfully.");
        } catch (Exception e) {
            logger.error("Error updating topics to subscribers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating topics to subscribers.");
        }
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