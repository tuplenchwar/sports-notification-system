package com.sportsnotification.dto;


import java.sql.Time;

public class Heartbeat {
    private String id;
    private Broker brokers;
    private long heartBeatTimestamp;

    public Heartbeat(String id, Broker brokers, long heartBeatTimestamp) {
        this.id = id;
        this.brokers = brokers;
        this.heartBeatTimestamp = heartBeatTimestamp;
    }

    public Heartbeat() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Broker getBrokers() {
        return brokers;
    }

    public void setBrokers(Broker brokers) {
        this.brokers = brokers;
    }

    public long getHeartBeatTimestamp() {
        return heartBeatTimestamp;
    }

    public void setHeartBeatTimestamp(long heartBeatTimestamp) {
        this.heartBeatTimestamp = heartBeatTimestamp;
    }
}
