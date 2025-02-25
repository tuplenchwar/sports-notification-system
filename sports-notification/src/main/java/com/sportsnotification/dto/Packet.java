package com.sportsnotification.dto;

public class Packet {
    private int id;
    private String topic;
    private String message;
    private String pid; // producer id
    private Integer mid; // message id
    private String timestamp;

    public Packet() {
    }

    public Packet(int id, String topic, String message, String pid, Integer mid, String timestamp) {
        this.id = id;
        this.topic = topic;
        this.message = message;
        this.pid = pid;
        this.mid = mid;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}


