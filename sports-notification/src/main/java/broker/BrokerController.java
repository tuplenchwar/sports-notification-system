package broker;

import dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("broker")
@RestController
@RequestMapping("/broker")
public class BrokerController {

    @Autowired
    private BrokerApplication brokerApplication;

    @GetMapping("/gettopics")
    public List<String> getAllTopics() {
        return brokerApplication.getAllTopics();
    }

    @PostMapping("/register-publisher")
    public void registerPublisher(@RequestBody Publisher publisher) {
        brokerApplication.registerPublisher(publisher);
    }

    @PostMapping("/register-subscriber")
    public void registerSubscriber(@RequestBody Subscriber subscriber) {
        brokerApplication.registerSubscriber(subscriber);
    }

    @PostMapping("/publish")
    public void publishMessage(@RequestBody Packet message) {
        brokerApplication.publishMessage(message);
    }

    @PutMapping("/replicatemessage")
    public void updateMessages(@RequestBody Packet message) {
        brokerApplication.updateMessages(message);
    }

    @PutMapping("/subscribe")
    public void subscribeToTopic(@RequestBody Subscriber subscriber) {
        brokerApplication.subscribeToTopic(subscriber);
    }

    @PutMapping("/unsubscribe")
    public void unsubscribeToTopic(@RequestBody Subscriber subscriber) {
        brokerApplication.unsubscribeToTopic(subscriber);
    }

    @PostMapping("/ack")
    public String acknowledgeMessage(@RequestBody AckPayload ackPayload) {
        return brokerApplication.acknowledgeMessage(ackPayload);
    }
}