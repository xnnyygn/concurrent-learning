package in.xnnyygn.concurrent.queue;

public class SRSWBoundedQueue<T> {

    private final T[] buffer;
    private volatile int cursor;
    private volatile int max;

    public SRSWBoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        buffer = (T[]) new Object[capacity];
        cursor = 0;
        max = 0;
    }

    public void enqueue(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        if (max - cursor == buffer.length) {
            throw new IllegalStateException("queue is full");
        }
        buffer[max % buffer.length] = item;
        max++;
    }

    public T dequeue() {
        if (max == cursor) {
            throw new IllegalStateException("queue is empty");
        }
        T result = buffer[cursor % buffer.length];
        cursor++;
        return result;
    }

}
