package core;

import model.Call;
import java.util.List;

public class AgingAlgorithm {
    private long agingThresholdMs;
    private int agingBoost;

    public AgingAlgorithm(long threshold, int boost) {
        this.agingThresholdMs = threshold;
        this.agingBoost = boost;
    }

    public void applyAging(List<Call> calls) {
        // applyAging skeleton
    }

    public int calculateBoost(Call call) {
        return 0;
    }
}
