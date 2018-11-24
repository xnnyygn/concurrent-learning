package in.xnnyygn.concurrent.queue;

import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBoundedQueue<T> {

    private final Lock enqueueLock = new ReentrantLock();
    private final Lock dequeueLock = new ReentrantLock();
    private final Condition notFullCondition;
    private final Condition notEmptyCondition;
    private final T[] buffer;
    @GuardedBy("dequeueLock")
    private int head;
    @GuardedBy("enqueueLock")
    private int tail;

    @SuppressWarnings("unchecked")
    public ArrayBoundedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        buffer = (T[]) new Object[capacity];
        notFullCondition = enqueueLock.newCondition();
        notEmptyCondition = dequeueLock.newCondition();
        head = 0;
        tail = 0;
    }

    public void enqueue(T item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException();
        }
        boolean wakeUpDequeueThread = false;
        enqueueLock.lock();
        try {
            if (tail - head == buffer.length) { // read head
                notFullCondition.await();
            }
            if (head == tail) {
                wakeUpDequeueThread = true;
            }
            buffer[tail % buffer.length] = item;
            tail++;
        } finally {
            enqueueLock.unlock();
        }
        if (wakeUpDequeueThread) {
            dequeueLock.lock();
            try {
                notEmptyCondition.signal();
            } finally {
                dequeueLock.unlock();
            }
        }
    }

    public T dequeue() throws InterruptedException {
        boolean wakeUpEnqueueThread = false;
        T result;
        dequeueLock.lock();
        try {
            if (head == tail) {
                notEmptyCondition.await();
            }
            if (tail - head == buffer.length) {
                wakeUpEnqueueThread = true;
            }
            result = buffer[head % buffer.length];
            head++;
        } finally {
            dequeueLock.unlock();
        }
        if (wakeUpEnqueueThread) {
            enqueueLock.lock();
            try {
                notFullCondition.signal();
            } finally {
                enqueueLock.unlock();
            }
        }
        return result;
    }
}
