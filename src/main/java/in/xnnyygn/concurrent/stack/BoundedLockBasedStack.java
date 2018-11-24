package in.xnnyygn.concurrent.stack;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedLockBasedStack<T> {

    private final Lock lock = new ReentrantLock();
    private final T[] buffer;
    @GuardedBy("lock")
    private int cursor = 0;

    public BoundedLockBasedStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        buffer = (T[]) new Object[capacity];
    }

    public void push(T value) {
        lock.lock();
        try {
            if (cursor == buffer.length) {
                throw new IllegalStateException("stack full");
            }
            buffer[cursor] = value;
            cursor++;
        } finally {
            lock.unlock();
        }
    }

    public T pop() {
        lock.lock();
        try {
            if (cursor == 0) {
                throw new IllegalStateException("empty stack");
            }
            T result = buffer[cursor];
            cursor--;
            return result;
        } finally {
            lock.unlock();
        }
    }
}
