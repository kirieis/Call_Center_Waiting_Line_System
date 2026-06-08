package core;

import java.util.List;

public interface StandardQueue<T> {
    void enqueue(T item);
    T dequeue();
    T peek();
    boolean isEmpty();
    int size();
    List<T> toList();
}
