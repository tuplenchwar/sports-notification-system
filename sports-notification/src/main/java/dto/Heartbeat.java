package dto;

import java.util.List;

public class Heartbeat {
    private String id;
    private String connectionUrl;
    private List<Broker> brokers;
    private List<Subscriber> subscribers;
    private Broker leaderBroker;

    public Heartbeat() {
    }

    public Heartbeat(String id, String connectionUrl, List<Broker> brokers, List<Subscriber> subscribers, Broker leaderBroker) {
        this.id = id;
        this.connectionUrl = connectionUrl;
        this.brokers = brokers;
        this.subscribers = subscribers;
        this.leaderBroker = leaderBroker;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }

    public List<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public Broker getLeaderBroker() {
        return leaderBroker;
    }

    public void setLeaderBroker(Broker leaderBroker) {
        this.leaderBroker = leaderBroker;
    }
}
