package in.xnnyygn.concurrent.queue;

import java.util.concurrent.atomic.AtomicInteger;

public class LockFreeArrayBoundedQueue<T> {

    private final T[] buffer;
    private final AtomicInteger atomicHead = new AtomicInteger(0);
    private final AtomicInteger atomicTail = new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    public LockFreeArrayBoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        buffer = (T[]) new Object[capacity];
    }

    public void enqueue(T item) {
        int tail = atomicTail.get();
        // cannot wait
        if (tail - atomicHead.get() == buffer.length) {
            throw new IllegalStateException("queue is full");
        }
        // tail maybe change between setting and increment
        buffer[tail % buffer.length] = item;
        atomicTail.getAndIncrement();
    }
}
