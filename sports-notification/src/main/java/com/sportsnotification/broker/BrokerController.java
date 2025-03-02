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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Profile("broker")
@RestController
@RequestMapping("/broker")
public class BrokerController {

    @Autowired
    private BrokerService brokerService;

    @GetMapping("/gettopics")
    public ResponseEntity<ConcurrentSkipListSet<String>> getAllTopics(@RequestParam String subscriberConnectionURL) {
        if (!brokerService.isSubscriberValid(subscriberConnectionURL)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 if subscriber is not valid
        }
        return ResponseEntity.ok(brokerService.getAllTopics());
    }

    @PostMapping("/register-publisher")
    public void registerPublisher(@RequestBody Publisher publisher) {
        brokerService.registerPublisher(publisher);
    }

    @PostMapping("/register-subscriber")
    public ResponseEntity<String>  registerSubscriber(@RequestBody Subscriber subscriber) {
        return brokerService.registerSubscriber(subscriber);
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishMessage(@RequestBody Packet message) {
        return brokerService.publishMessage(message);
    }

    @PostMapping("/replicatemessages")
    public ResponseEntity<String> updateMessages(@RequestBody ConcurrentLinkedQueue<Packet> messageQueue) {
        return brokerService.updateMessages(messageQueue);
    }

    @PostMapping("/replicatetopics")
    public ResponseEntity<String> updateTopics(@RequestBody ConcurrentSkipListSet<String> topics) {
        return brokerService.updateTopics(topics);
    }

    @PostMapping("/replicatesubscribers")
    public ResponseEntity<String> updateSubscribers(@RequestBody CopyOnWriteArrayList<Subscriber> subscribers) {
        return brokerService.updateSubscribers(subscribers);
    }

    @PostMapping("/replicatetopicstosubscribers")
    public ResponseEntity<String> updateTopicsToSubscribers(@RequestBody ConcurrentHashMap<String, List<Subscriber>> topicsToSubscribers) {
        return brokerService.updateTopicsToSubscribers(topicsToSubscribers);
    }

    @PutMapping("/subscribe")
    public ResponseEntity<String> subscribeToTopic(@RequestBody Subscriber subscriber) {
        return brokerService.subscribeToTopic(subscriber);
    }

    @PutMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribeToTopic(@RequestBody Subscriber subscriber) {
        return brokerService.unsubscribeToTopic(subscriber);
    }

    @PutMapping("/update-leader")
    public void updateLeader(@RequestBody Broker newleader) {
        brokerService.updateLeader(newleader);
    }

    @PutMapping("/update-brokers")
    public void  updateBrokerList(@RequestBody List<Broker> brokers) {
        brokerService.updateBrokers(brokers);
    }

    @GetMapping("/brokers-list")//testing
    public List<Broker> getBrokersList() {
        return brokerService.getBrokersList();
    }
    
}