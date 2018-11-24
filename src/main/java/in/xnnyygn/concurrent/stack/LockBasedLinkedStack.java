package in.xnnyygn.concurrent.stack;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedLinkedStack<T> {

    private final Lock lock = new ReentrantLock();
    private Node<T> head = null;

    public void push(T value) {
        lock.lock();
        try {
            head = new Node<>(value, head);
        } finally {
            lock.unlock();
        }
    }

    public T pop() {
        lock.lock();
        try {
            if (head == null) {
                throw new IllegalStateException("empty stack");
            }
            Node<T> oldHead = head;
            head = head.next;
            return oldHead.value;
        } finally {
            lock.unlock();
        }
    }

    private static class Node<T> {
        private final T value;
        private final Node<T> next;

        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }
}
