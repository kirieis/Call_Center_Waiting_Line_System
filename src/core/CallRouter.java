package core;

import model.Call;
import java.util.List;

public class CallRouter {
    private PriorityCallQueue queue;
    private AgingAlgorithm aging;

    public CallRouter() {
        this.queue = new PriorityCallQueue();
        this.aging = new AgingAlgorithm(60000, 5);
    }

    public void addCall(Call call) {
        // addCall skeleton
    }

    public Call processNext() {
        return null;
    }

    public void applyAging() {
        // applyAging skeleton
    }

    public List<Call> getQueueSnapshot() {
        return null;
    }
}
