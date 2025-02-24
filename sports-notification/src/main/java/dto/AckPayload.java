package dto;

public class AckPayload {
    private final String topic;
    private final int messageId;
    private final String subscriberId;

    public AckPayload(String topic, int messageId, String subscriberId) {
        this.topic = topic;
        this.messageId = messageId;
        this.subscriberId = subscriberId;
    }

    public String getTopic() {
        return topic;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getSubscriberId() {
        return subscriberId;
    }
}
