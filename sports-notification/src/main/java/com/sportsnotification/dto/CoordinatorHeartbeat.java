package com.sportsnotification.dto;

public class CoordinatorHeartbeat {
    private Long heartbeatTimestamp;
    private String coordinatorURL;

    public CoordinatorHeartbeat() {
    }

    public CoordinatorHeartbeat(Long heartbeatTimestamp, String coordinatorURL) {
        this.heartbeatTimestamp = heartbeatTimestamp;
        this.coordinatorURL = coordinatorURL;
    }

    public Long getHeartbeatTimestamp() {
        return heartbeatTimestamp;
    }

    public void setHeartbeatTimestamp(Long heartbeatTimestamp) {
        this.heartbeatTimestamp = heartbeatTimestamp;
    }

    public String getCoordinatorURL() {
        return coordinatorURL;
    }

    public void setCoordinatorURL(String coordinatorURL) {
        this.coordinatorURL = coordinatorURL;
    }
}
