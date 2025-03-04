package com.sportsnotification.dto;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CoordinatorSyncData {
    private List<Broker> brokers;
    private Broker leaderBroker;
    private ConcurrentHashMap<Integer, Long> brokerHeartbeatMap;

    public CoordinatorSyncData() {
    }
    
    public CoordinatorSyncData(List<Broker> brokers, Broker leaderBroker, ConcurrentHashMap<Integer, Long> brokerHeartbeatMap) {
        this.brokers = brokers;
        this.leaderBroker = leaderBroker;
        this.brokerHeartbeatMap = brokerHeartbeatMap;
    }

    public List<Broker> getBrokers() {
        return this.brokers;
    }

    public Broker getLeaderBroker() {
        return this.leaderBroker;
    }

    public ConcurrentHashMap<Integer, Long> getBrokerHeartbeatMap() {
        return this.brokerHeartbeatMap;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }

    public void setLeaderBroker(Broker leaderBroker) {
        this.leaderBroker = leaderBroker;
    }

    public void setBrokerHeartbeatMap(ConcurrentHashMap<Integer, Long> brokerHeartbeatMap) {
        this.brokerHeartbeatMap = brokerHeartbeatMap;
    }

    public String toString() {
        return "CoordinatorSyncData(brokers=" + this.getBrokers() + ", leaderBroker=" + this.getLeaderBroker() + ", brokerHeartbeatMap=" + this.getBrokerHeartbeatMap() + ")";
    }
}
