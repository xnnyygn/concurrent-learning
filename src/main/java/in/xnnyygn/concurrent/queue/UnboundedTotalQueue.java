package in.xnnyygn.concurrent.queue;

import java.util.concurrent.locks.ReentrantLock;

public class UnboundedTotalQueue<T> {

    private final ReentrantLock enqueueLock = new ReentrantLock();
    private final ReentrantLock dequeueLock = new ReentrantLock();
    private Node<T> head;
    private Node<T> tail;

    public UnboundedTotalQueue() {
        head = new Node<>();
        tail = head;
    }

    public void enqueue(T item) {
        enqueueLock.lock();
        try {
            Node<T> node = new Node<>(item);
            tail.next = node;
            tail = node;
        } finally {
            enqueueLock.unlock();
        }
    }

    public T dequeue() {
        T result;
        dequeueLock.lock();
        try {
            if (head.next == null) {
                throw new IllegalStateException("queue is empty");
            }
            result = head.next.value;
            head = head.next;
        } finally {
            dequeueLock.unlock();
        }
        return result;
    }

    private static class Node<T> {
        private T value;
        private Node<T> next;

        Node() {
            this.value = null;
        }

        Node(T value) {
            this.value = value;
        }
    }
}
