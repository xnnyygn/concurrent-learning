package in.xnnyygn.concurrent.listset;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CoarseList<T> {

    private final Lock lock = new ReentrantLock();
    private final Node<T> head;

    public CoarseList() {
        head = new Node<>(Integer.MIN_VALUE);
        head.next = new Node<>(Integer.MAX_VALUE);
    }

    public boolean add(T x) {
        Node<T> current = head;
        Node<T> predecessor = null;
        int key = x.hashCode();
        lock.lock();
        try {
            // traverse from head
            while (current.key < key) {
                predecessor = current;
                current = current.next;
            }
            if (current.key == key) {
                return false;
            }
            assert predecessor != null;
            Node<T> node = new Node<>(key, x);
            predecessor.next = node;
            node.next = current;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(T x) {
        Node<T> current = head;
        Node<T> predecessor = null;
        int key = x.hashCode();
        lock.lock();
        try {
            // traverse from head
            while (current.key < key) {
                predecessor = current;
                current = current.next;
            }
            if (current.key != key) {
                return false;
            }
            assert predecessor != null;
            predecessor.next = current.next;
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(T x) {
        Node<T> current = head;
        int key = x.hashCode();
        lock.lock();
        try {
            // traverse from head
            while (current.key < key) {
                current = current.next;
            }
            return current.key == key;
        } finally {
            lock.unlock();
        }
    }

    private static class Node<T> {
        final int key;
        T value;
        Node<T> next;

        Node(int key) {
            this.key = key;
        }

        Node(int key, T value) {
            this.key = key;
            this.value = value;
        }
    }
}
