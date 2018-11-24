package in.xnnyygn.concurrent.queue;

public class BoundedQueue2<T> {

    private final T[] buffer;
    private int cursor;
    private int max;

    @SuppressWarnings("unchecked")
    public BoundedQueue2(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        buffer = (T[]) new Object[capacity];
        cursor = 0;
        max = 0;
    }

    public void enqueue(T x) throws InterruptedException {
        if (x == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            while ((max - cursor) == buffer.length) {
                wait();
            }
            buffer[max - cursor] = x;
            max++;
            notifyAll();
        }
    }

    public T dequeue() throws InterruptedException {
        T result;
        synchronized (this) {
            while (cursor == max) {
                wait();
            }
            result = buffer[cursor++];
            notifyAll();
        }
        return result;
    }

}
