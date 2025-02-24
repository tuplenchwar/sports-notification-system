package dto;

import java.util.List;

public class MsgToSub {
    private int id;
    private List<Integer> subscriberIds;

    public MsgToSub() {
    }

    public MsgToSub(int id, List<Integer> subscriberIds) {
        this.id = id;
        this.subscriberIds = subscriberIds;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getSubscriberIds() {
        return subscriberIds;
    }

    public void setSubscriberIds(List<Integer> subscriberIds) {
        this.subscriberIds = subscriberIds;
    }
}
