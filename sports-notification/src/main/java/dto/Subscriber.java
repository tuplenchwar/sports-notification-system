package dto;

public class Subscriber {
    private String id;
    private String topic;
    private String message;
    private String connectionUrl;

    public Subscriber() {
    }

    public Subscriber(String id, String topic, String message, String connectionUrl) {
        this.id = id;
        this.topic = topic;
        this.message = message;
        this.connectionUrl = connectionUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }
}
