package core;

import model.Call;
import java.util.List;
import java.util.ArrayList;

public class PriorityCallQueue implements StandardQueue<Call> {
    private List<Call> heap;

    public PriorityCallQueue() {
        this.heap = new ArrayList<>();
    }

    @Override
    public void enqueue(Call call) {
        // Enqueue skeleton
    }

    @Override
    public Call dequeue() {
        return null;
    }

    @Override
    public Call peek() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public List<Call> toList() {
        return null;
    }

    private void siftUp(int index) {
        // siftUp skeleton
    }

    private void siftDown(int index) {
        // siftDown skeleton
    }

    private void swap(int i, int j) {
        // swap skeleton
    }
}
