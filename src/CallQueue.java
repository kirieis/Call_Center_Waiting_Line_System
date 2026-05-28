import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class CallQueue {
    private final PriorityQueue<Call> waitingCalls;

    public CallQueue() {
        waitingCalls = new PriorityQueue<>(new Comparator<Call>() {
            @Override
            public int compare(Call first, Call second) {
                if (first.getPriorityPoint() != second.getPriorityPoint()) {
                    return second.getPriorityPoint() - first.getPriorityPoint();
                }

                return first.getOrderNumber() - second.getOrderNumber();
            }
        });
    }

    public void addCall(Call call) {
        waitingCalls.add(call);
    }

    public Call getNextCall() {
        return waitingCalls.poll();
    }

    public boolean isEmpty() {
        return waitingCalls.isEmpty();
    }

    public void showWaitingCalls() {
        if (waitingCalls.isEmpty()) {
            System.out.println("No waiting calls.");
            return;
        }

        List<Call> sortedCalls = new ArrayList<>(waitingCalls);
        sortedCalls.sort(waitingCalls.comparator());

        System.out.println("\nWaiting calls:");
        for (int i = 0; i < sortedCalls.size(); i++) {
            System.out.println((i + 1) + ". " + sortedCalls.get(i));
        }
    }
}
