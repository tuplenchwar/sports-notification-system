package com.sportsnotification.broker;

import com.sportsnotification.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Profile("broker")
@RestController
@RequestMapping("/broker")
public class BrokerController {

    @Autowired
    private BrokerService brokerService;

    @GetMapping("/gettopics")
    public List<String> getAllTopics() {
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
    public void publishMessage(@RequestBody Packet message) {
        brokerService.publishMessage(message);
    }

    @PutMapping("/replicatemessage")
    public void updateMessages(@RequestBody Packet message) {
        brokerService.updateMessages(message);
    }

    @PutMapping("/subscribe")
    public void subscribeToTopic(@RequestBody Subscriber subscriber) {
        brokerService.subscribeToTopic(subscriber);
    }

    @PutMapping("/unsubscribe")
    public void unsubscribeToTopic(@RequestBody Subscriber subscriber) {
        brokerService.unsubscribeToTopic(subscriber);
    }

    @PostMapping("/ack")
    public String acknowledgeMessage(@RequestBody AckPayload ackPayload) {
        return brokerService.acknowledgeMessage(ackPayload);
    }

    @PutMapping("/update-leader")
    public void updateLeader(@RequestBody Broker newleader) {
        brokerService.updateLeader(newleader);
    }
}