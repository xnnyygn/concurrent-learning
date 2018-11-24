package in.xnnyygn.concurrent.queue;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SynchronousQueue<T> {

    private final Lock lock = new ReentrantLock();
    private final Condition enqueuedCondition;
    private final Condition enqueuingCondition;
    private final Condition dequeuedCondition;
    private boolean enqueueing = false;
    private T item = null;

    public SynchronousQueue() {
        enqueuedCondition = lock.newCondition();
        enqueuingCondition = lock.newCondition();
        dequeuedCondition = lock.newCondition();
    }

    public void enqueue(T item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException();
        }
        lock.lock();
        try {
            // wait for previous enqueue thread
            // condition 3
            if (enqueueing) {
                enqueuingCondition.await();
            }
            enqueueing = true;

            // condition 1
            this.item = item;
            enqueuedCondition.signal();

            // wait for dequeue thread
            // condition 2
            dequeuedCondition.await();

            // condition 3
            enqueueing = false;
            enqueuingCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() throws InterruptedException {
        T result;
        lock.lock();
        try {
            // wait for enqueue thread
            // condition 1
            if (item == null) {
                enqueuedCondition.await();
            }
            result = item;

            // condition 2
            item = null;
            dequeuedCondition.signal();
            return result;
        } finally {
            lock.unlock();
        }
    }
}
