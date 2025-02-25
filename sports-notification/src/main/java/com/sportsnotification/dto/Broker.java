package com.sportsnotification.broker.dto;

public class Broker {
    private int id;
    private String connectionUrl;
    private int port;
    private boolean isLeaderl;

    public Broker() {
    }
    
    public Broker(int id, String connectionUrl, int port, boolean isLeader) {
        this.id = id;
        this.connectionUrl = connectionUrl;
        this.port = port;
        this.isLeaderl = isLeader;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isLeaderl() {
        return isLeaderl;
    }

    public void setLeaderl(boolean leaderl) {
        isLeaderl = leaderl;
    }
}