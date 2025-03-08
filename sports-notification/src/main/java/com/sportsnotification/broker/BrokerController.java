package com.sportsnotification.broker;

import com.sportsnotification.dto.Broker;
import com.sportsnotification.dto.Packet;
import com.sportsnotification.dto.Publisher;
import com.sportsnotification.dto.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Profile("broker")
@RestController
@RequestMapping("/broker")
public class BrokerController {

    private static final Logger logger = LoggerFactory.getLogger(BrokerController.class);

    @Autowired
    private BrokerService brokerService;

    @GetMapping("/gettopics")
    public ResponseEntity<ConcurrentSkipListSet<String>> getAllTopics(@RequestParam String subscriberConnectionURL) {
        try {
            logger.info("Fetching all topics for subscriber: {}", subscriberConnectionURL);
            if (!brokerService.isSubscriberValid(subscriberConnectionURL)) {
                logger.warn("Invalid subscriber access attempt: {}", subscriberConnectionURL);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(brokerService.getAllTopics());
        } catch (Exception e) {
            logger.error("Error fetching topics for subscriber {}: {}", subscriberConnectionURL, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/register-publisher")
    public void registerPublisher(@RequestBody Publisher publisher) {
        try {
            logger.info("Registering publisher: {}", publisher);
            brokerService.registerPublisher(publisher);
        } catch (Exception e) {
            logger.error("Error registering publisher {}: {}", publisher, e.getMessage(), e);
        }
    }

    @PostMapping("/register-subscriber")
    public ResponseEntity<String> registerSubscriber(@RequestBody Subscriber subscriber) {
        try {
            logger.info("Registering subscriber: {}", subscriber);
            return brokerService.registerSubscriber(subscriber);
        } catch (Exception e) {
            logger.error("Error registering subscriber {}: {}", subscriber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error registering subscriber");
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody Packet message) {
        try {
            logger.info("Publishing message: {}", message);
            return brokerService.publishMessage(message);
        } catch (Exception e) {
            logger.error("Error publishing message {}: {}", message, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error publishing message");
        }
    }

    @PostMapping("/replicatemessages")
    public ResponseEntity<String> updateMessages(@RequestBody ConcurrentLinkedQueue<Packet> messageQueue) {
        try {
            logger.info("Replicating messages: {}", messageQueue);
            return brokerService.updateMessages(messageQueue);
        } catch (Exception e) {
            logger.error("Error replicating messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating messages");
        }
    }

    @PostMapping("/replicatetopics")
    public ResponseEntity<String> updateTopics(@RequestBody ConcurrentSkipListSet<String> topics) {
        try {
            logger.info("Replicating topics: {}", topics);
            return brokerService.updateTopics(topics);
        } catch (Exception e) {
            logger.error("Error replicating topics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating topics");
        }
    }

    @PostMapping("/replicatesubscribers")
    public ResponseEntity<String> updateSubscribers(@RequestBody CopyOnWriteArrayList<Subscriber> subscribers) {
        try {
            logger.info("Replicating subscribers: {}", subscribers);
            return brokerService.updateSubscribers(subscribers);
        } catch (Exception e) {
            logger.error("Error replicating subscribers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating subscribers");
        }
    }

    @PostMapping("/replicatetopicstosubscribers")
    public ResponseEntity<String> updateTopicsToSubscribers(@RequestBody ConcurrentHashMap<String, List<Subscriber>> topicsToSubscribers) {
        try {
            logger.info("Replicating topics to subscribers: {}", topicsToSubscribers);
            return brokerService.updateTopicsToSubscribers(topicsToSubscribers);
        } catch (Exception e) {
            logger.error("Error replicating topics to subscribers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error replicating topics to subscribers");
        }
    }

    @PutMapping("/subscribe")
    public ResponseEntity<String> subscribeToTopic(@RequestBody Subscriber subscriber) {
        try {
            logger.info("Subscribing to topic: {}", subscriber);
            return brokerService.subscribeToTopic(subscriber);
        } catch (Exception e) {
            logger.error("Error subscribing to topic: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error subscribing to topic");
        }
    }

    @PutMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribeToTopic(@RequestBody Subscriber subscriber) {
        try {
            logger.info("Unsubscribing from topic: {}", subscriber);
            return brokerService.unsubscribeToTopic(subscriber);
        } catch (Exception e) {
            logger.error("Error unsubscribing from topic: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error unsubscribing from topic");
        }
    }

    @PutMapping("/update-leader")
    public void updateLeader(@RequestBody Broker newLeader) {
        try {
            logger.info("Updating leader: {}", newLeader);
            brokerService.updateLeader(newLeader);
        } catch (Exception e) {
            logger.error("Error updating leader: {}", e.getMessage(), e);
        }
    }

    @PutMapping("/update-brokers")
    public void updateBrokerList(@RequestBody List<Broker> brokers) {
        try {
            logger.info("Updating broker list: {}", brokers);
            brokerService.updateBrokers(brokers);
        } catch (Exception e) {
            logger.error("Error updating broker list: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/brokers-list")
    public List<Broker> getBrokersList() {
        try {
            logger.info("Fetching brokers list");
            return brokerService.getBrokersList();
        } catch (Exception e) {
            logger.error("Error fetching brokers list: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
