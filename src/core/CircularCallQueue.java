package core;

import model.Call;
import model.CallStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Circular Queue using a fixed-size array.
 * 
 * Caps the maximum number of waiting calls in the system.
 * When the queue is full, new calls will be marked as MISSED.
 * 
 * Uses front/rear pointers and modulo arithmetic to manage the queue.
 * 
 * Implements StandardQueue<Call>.
 */
public class CircularCallQueue implements StandardQueue<Call> {

    private Call[] elements;
    private int front;
    private int rear;
    private int capacity;
    private int count;

    /**
     * Initializes circular queue with default capacity of 100.
     */
    public CircularCallQueue() {
        this(100);
    }

    /**
     * Initializes circular queue with custom capacity.
     * @param capacity maximum number of elements
     */
    public CircularCallQueue(int capacity) {
        this.capacity = capacity;
        this.elements = new Call[capacity];
        this.front = 0;
        this.rear = -1;
        this.count = 0;
    }

    /**
     * Enqueues a call to the circular queue.
     * If full, the call is marked as MISSED and not added.
     */
    @Override
    public void enqueue(Call call) {
        if (isFull()) {
            call.setStatus(CallStatus.MISSED);
            System.out.println("  [!] Circular queue is full! Call from " 
                    + call.getCustomerName() + " was MISSED.");
            return;
        }
        rear = (rear + 1) % capacity;
        elements[rear] = call;
        count++;
    }

    /**
     * Dequeues and returns the first call (FIFO).
     */
    @Override
    public Call dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Circular queue is empty!");
        }
        Call call = elements[front];
        elements[front] = null;
        front = (front + 1) % capacity;
        count--;
        return call;
    }

    /**
     * Peeks at the first call without removing it.
     */
    @Override
    public Call peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Circular queue is empty!");
        }
        return elements[front];
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public int size() {
        return count;
    }

    /**
     * Checks if the circular queue is full.
     * @return true if full
     */
    public boolean isFull() {
        return count == capacity;
    }

    /**
     * Gets maximum capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Removes a specific call from the circular queue and shifts subsequent elements.
     * Used when a call is dequeued from the priority queue so it is no longer waiting.
     */
    public void remove(Call call) {
        if (isEmpty() || call == null) {
            return;
        }

        int index = front;
        boolean found = false;
        int distance = -1; // distance from front

        for (int i = 0; i < count; i++) {
            if (elements[index] != null && elements[index].getCustomerId().equals(call.getCustomerId())) {
                found = true;
                distance = i;
                break;
            }
            index = (index + 1) % capacity;
        }

        if (!found) {
            return;
        }

        // Shift elements to fill the gap left by the removed element.
        // We shift elements from (index + 1) back to index.
        int current = index;
        for (int i = distance; i < count - 1; i++) {
            int next = (current + 1) % capacity;
            elements[current] = elements[next];
            current = next;
        }

        // Clear the last element (which is now a duplicate)
        elements[current] = null;
        rear = (rear - 1 + capacity) % capacity;
        count--;
    }

    /**
     * Converts circular queue elements to a List (in FIFO order).
     */
    @Override
    public List<Call> toList() {
        List<Call> list = new ArrayList<>();
        if (isEmpty()) {
            return list;
        }
        int index = front;
        for (int i = 0; i < count; i++) {
            list.add(elements[index]);
            index = (index + 1) % capacity;
        }
        return list;
    }
}
