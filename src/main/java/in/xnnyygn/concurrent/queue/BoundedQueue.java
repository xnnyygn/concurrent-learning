package in.xnnyygn.concurrent.queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedQueue<T> {

    private final ReentrantLock enqueueLock = new ReentrantLock();
    private final ReentrantLock dequeueLock = new ReentrantLock();
    private final Condition notEmptyCondition;
    private final Condition notFullCondition;
    private final AtomicInteger atomicSize; // remaining size
    private final int capacity;
    private Node<T> head;
    private Node<T> tail;

    public BoundedQueue(int capacity) {
        this.capacity = capacity;
        atomicSize = new AtomicInteger(0);
        head = new Node<>();
        tail = head;
        notEmptyCondition = dequeueLock.newCondition();
        notFullCondition = enqueueLock.newCondition();
    }

    public void enqueue(T x) throws InterruptedException {
        if (x == null) {
            throw new NullPointerException();
        }
        boolean wakeDequeueThread = false;
        enqueueLock.lock();
        try {
            // release lock and wait for condition not-full
            while (atomicSize.get() == capacity) {
                notFullCondition.await();
            }
            Node<T> node = new Node<>(x);
            tail.next = node;
            tail = node;
            // enqueue item into an empty queue
            if (atomicSize.getAndIncrement() == 0) {
                wakeDequeueThread = true;
            }
        } finally {
            enqueueLock.lock();
        }
        // signal dequeue thread when enqueue item into an empty queue
        if (wakeDequeueThread) {
            dequeueLock.lock();
            try {
                notEmptyCondition.signalAll();
            } finally {
                dequeueLock.unlock();
            }
        }
    }

    public T dequeue() throws InterruptedException {
        T result;
        boolean wakeEnqueueThread = false;
        dequeueLock.lock();
        try {
            // release lock and wait for condition not empty
            while (atomicSize.get() == 0) {
                notEmptyCondition.await();
            }
            // head.value is not used
            result = head.next.value;
            head = head.next;
            // dequeue item from a full queue
            if (atomicSize.getAndDecrement() == capacity) {
                wakeEnqueueThread = true;
            }
        } finally {
            dequeueLock.unlock();
        }
        // signal enqueue thread when dequeue item from a full queue
        if (wakeEnqueueThread) {
            enqueueLock.lock();
            try {
                notFullCondition.signalAll();
            } finally {
                enqueueLock.unlock();
            }
        }
        return result;
    }

    private static class Node<T> {
        private final T value;
        private Node<T> next;

        Node() {
            this.value = null;
        }

        Node(T value) {
            this.value = value;
        }
    }
}
