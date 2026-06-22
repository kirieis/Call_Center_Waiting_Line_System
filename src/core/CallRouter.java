package core;

import model.Call;
import model.CallStatus;
import java.util.List;

/**
 * Call Router - Central call flow manager.
 * 
 * Functions:
 * - Receives new calls -> adds to both PriorityQueue and CircularQueue
 * - Processes next call (dequeue from PriorityQueue)
 * - Applies Aging Algorithm to increase priority for long-waiting calls
 * - Provides snapshots of queue status
 */
public class CallRouter {

    private PriorityCallQueue priorityQueue;
    private CircularCallQueue circularQueue;
    private AgingAlgorithm aging;

    /**
     * Initializes CallRouter with default queues.
     */
    public CallRouter() {
        this.priorityQueue = new PriorityCallQueue();
        this.circularQueue = new CircularCallQueue();
        this.aging = new AgingAlgorithm();
    }

    /**
     * Initializes CallRouter with custom CircularQueue capacity.
     */
    public CallRouter(int circularCapacity) {
        this.priorityQueue = new PriorityCallQueue();
        this.circularQueue = new CircularCallQueue(circularCapacity);
        this.aging = new AgingAlgorithm();
    }

    /**
     * Initializes CallRouter with an existing PriorityQueue (from CallProcessor).
     */
    public CallRouter(PriorityCallQueue existingQueue, int circularCapacity) {
        this.priorityQueue = existingQueue;
        this.circularQueue = new CircularCallQueue(circularCapacity);
        this.aging = new AgingAlgorithm();
    }

    /**
     * Adds a new call to the system.
     * The call is enqueued to both PriorityQueue (for priority processing)
     * and CircularQueue (to cap the total number of waiting calls).
     * 
     * @param call the call to add
     */
    public void addCall(Call call) {
        call.setStatus(CallStatus.WAITING);
        priorityQueue.enqueue(call);
        circularQueue.enqueue(call);
    }

    /**
     * Processes the highest priority call.
     * Dequeues from PriorityQueue, sets status to PROCESSING.
     * 
     * @return the processed call, or null if queue is empty
     */
    public Call processNext() {
        if (priorityQueue.isEmpty()) {
            return null;
        }
        Call call = priorityQueue.dequeue();
        call.setStatus(CallStatus.PROCESSING);
        circularQueue.remove(call);
        return call;
    }

    /**
     * Increments the wait time of all currently waiting calls in the priority queue
     * and rebuilds the heap to update their priorities.
     * 
     * @param ms elapsed time in milliseconds to add to waitTime of each call
     */
    public void incrementWaitingCallsWaitTime(int ms) {
        for (Call call : priorityQueue.getInternalList()) {
            call.incrementWaitTime(ms);
        }
        priorityQueue.rebuildHeap();
    }

    /**
     * Applies Aging Algorithm to all waiting calls.
     * After waitTime increment, rebuilds heap to update priorities.
     */
    public void applyAging() {
        List<Call> waitingCalls = priorityQueue.getInternalList();
        aging.applyAging(waitingCalls);
        priorityQueue.rebuildHeap();
    }

    /**
     * Gets priority queue snapshot (sorted by descending priority).
     */
    public List<Call> getQueueSnapshot() {
        return priorityQueue.toList();
    }

    /**
     * Gets circular queue snapshot (FIFO order).
     */
    public List<Call> getCircularQueueSnapshot() {
        return circularQueue.toList();
    }

    /**
     * Gets the current PriorityCallQueue.
     */
    public PriorityCallQueue getPriorityQueue() {
        return priorityQueue;
    }

    /**
     * Sets a new PriorityCallQueue (used when loading from CallProcessor).
     */
    public void setPriorityQueue(PriorityCallQueue queue) {
        this.priorityQueue = queue;
    }

    /**
     * Gets the current CircularCallQueue.
     */
    public CircularCallQueue getCircularQueue() {
        return circularQueue;
    }

    /**
     * Gets the current AgingAlgorithm.
     */
    public AgingAlgorithm getAgingAlgorithm() {
        return aging;
    }

    /**
     * Checks if the queues are empty.
     */
    public boolean isQueueEmpty() {
        return priorityQueue.isEmpty();
    }

    /**
     * Gets the total number of waiting calls in PriorityQueue.
     */
    public int getQueueSize() {
        return priorityQueue.size();
    }
}
