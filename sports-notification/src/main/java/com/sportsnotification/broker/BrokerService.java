package com.sportsnotification.broker;

import com.sportsnotification.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;

@Service
public class BrokerService {
    private final List<Broker> brokers = new ArrayList<>();
    private final List<Subscriber> subscribers = new ArrayList<>();
    private final List<Publisher> publishers = new ArrayList<>();
    private final List<String> topics = new ArrayList<>();
    private final ConcurrentHashMap<String, List<Subscriber>> topicsSubscriber = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> messageIdTomessage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, List<MsgToSub>> messageIdToSub = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<Integer, List<String>>> acknowledgments = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private BrokerRegistration brokerRegistration;

    public List<String> getAllTopics() {
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

    public void publishMessage(Packet message) {
        if (message.getTopic() == null || message.getMessage() == null) {
            throw new IllegalArgumentException("Topic and Message cannot be null");
        }

        messageIdTomessage.put(message.getMid(), message.getMessage());

        if (!topics.contains(message.getTopic())) {
            topics.add(message.getTopic());
        }

        List<Integer> subscriberIds = new ArrayList<>();
        for (Subscriber s : topicsSubscriber.get(message.getTopic())) {
            subscriberIds.add(Integer.parseInt(s.getId()));
        }

        MsgToSub msgToSub = new MsgToSub(message.getMid(), subscriberIds);

        if (messageIdToSub.containsKey(message.getMid())) {
            messageIdToSub.get(message.getMid()).add(msgToSub);
        } else {
            List<MsgToSub> msgToSubList = new ArrayList<>();
            msgToSubList.add(msgToSub);
            messageIdToSub.put(message.getMid(), msgToSubList);
        }
    }

    public void updateMessages(Packet message) {
        if (message.getTopic() == null || message.getMessage() == null) {
            throw new IllegalArgumentException("Topic and Message cannot be null");
        }
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

    public String acknowledgeMessage(AckPayload ackPayload) {
        String topic = ackPayload.getTopic();
        int messageId = ackPayload.getMessageId();
        String subscriber = ackPayload.getSubscriberId();

        // Store acknowledgment in memory
        acknowledgments.get(topic).get(messageId).add(subscriber);

        // If all subscribers acknowledged, queue for batch deletion
        if (acknowledgments.get(topic).get(messageId).size() == getSubscriberCount(topic)) {
            queueMessageForDeletion(topic, messageId);
            acknowledgments.get(topic).remove(messageId);
        }
        return "✅ Acknowledgment received for message [" + topic + "] from subscriber: " + subscriber;
    }

    public void updateLeader(Broker newLeader) {
        Broker currentBroker = brokerRegistration.getCurrentBroker();
        if (newLeader.getId()== currentBroker.getId()) {
            currentBroker.setLeader(true);
            System.out.println("I am the leader");
        } else {
            System.out.println("Leader is: " + newLeader.getId());
        }
    }

    private int getSubscriberCount(String topic) {
        return topicsSubscriber.get(topic).size();
    }

    private void queueMessageForDeletion(String topic, int messageId) {
        // Implement the logic for queuing the message for deletion
    }
}