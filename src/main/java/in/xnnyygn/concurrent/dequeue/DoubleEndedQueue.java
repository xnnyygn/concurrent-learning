package in.xnnyygn.concurrent.dequeue;

public interface DoubleEndedQueue<T> {
    void pushBottom(T x);

    T popTop();

    T popBottom();

    boolean isEmpty();
}
