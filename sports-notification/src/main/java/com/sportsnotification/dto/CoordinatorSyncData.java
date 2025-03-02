package com.sportsnotification.dto;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CoordinatorSyncData {
    private List<Broker> brokers;
    private Broker leaderBroker;
    private ConcurrentHashMap<Integer, Long> brokerHeartbeatMap;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CoordinatorSyncData)) return false;
        final CoordinatorSyncData other = (CoordinatorSyncData) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$brokers = this.getBrokers();
        final Object other$brokers = other.getBrokers();
        if (this$brokers == null ? other$brokers != null : !this$brokers.equals(other$brokers)) return false;
        final Object this$leaderBroker = this.getLeaderBroker();
        final Object other$leaderBroker = other.getLeaderBroker();
        if (this$leaderBroker == null ? other$leaderBroker != null : !this$leaderBroker.equals(other$leaderBroker))
            return false;
        final Object this$brokerHeartbeatMap = this.getBrokerHeartbeatMap();
        final Object other$brokerHeartbeatMap = other.getBrokerHeartbeatMap();
        if (this$brokerHeartbeatMap == null ? other$brokerHeartbeatMap != null : !this$brokerHeartbeatMap.equals(other$brokerHeartbeatMap))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CoordinatorSyncData;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $brokers = this.getBrokers();
        result = result * PRIME + ($brokers == null ? 43 : $brokers.hashCode());
        final Object $leaderBroker = this.getLeaderBroker();
        result = result * PRIME + ($leaderBroker == null ? 43 : $leaderBroker.hashCode());
        final Object $brokerHeartbeatMap = this.getBrokerHeartbeatMap();
        result = result * PRIME + ($brokerHeartbeatMap == null ? 43 : $brokerHeartbeatMap.hashCode());
        return result;
    }

    public String toString() {
        return "CoordinatorSyncData(brokers=" + this.getBrokers() + ", leaderBroker=" + this.getLeaderBroker() + ", brokerHeartbeatMap=" + this.getBrokerHeartbeatMap() + ")";
    }
}
