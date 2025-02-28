package com.sportsnotification.broker;

import com.sportsnotification.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

@Profile("broker")
@RestController
@RequestMapping("/broker")
public class BrokerController {

    @Autowired
    private BrokerService brokerService;

    @GetMapping("/gettopics")
    public ConcurrentSkipListSet<String> getAllTopics() {
        return brokerService.getAllTopics();
    }

    @PostMapping("/register-publisher")
    public void registerPublisher(@RequestBody Publisher publisher) {
        brokerService.registerPublisher(publisher);
    }

    @PostMapping("/register-subscriber")
    public void registerSubscriber(@RequestBody Subscriber subscriber) {
        brokerService.registerSubscriber(subscriber);
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

    @PutMapping("/subscribe")
    public void subscribeToTopic(@RequestBody Subscriber subscriber) {
        brokerService.subscribeToTopic(subscriber);
    }

    @PutMapping("/unsubscribe")
    public void unsubscribeToTopic(@RequestBody Subscriber subscriber) {
        brokerService.unsubscribeToTopic(subscriber);
    }

    @PutMapping("/update-leader")
    public void updateLeader(@RequestBody Broker newleader) {
        brokerService.updateLeader(newleader);
    }

    @PutMapping("/update-brokers")
    public void  updateBrokerList(@RequestBody List<Broker> brokers) {
        brokerService.updateBrokers(brokers);
    }

    @GetMapping("/brokers-list")
    public List<Broker> getBrokersList() {
        return brokerService.getBrokersList();
    }
    
}