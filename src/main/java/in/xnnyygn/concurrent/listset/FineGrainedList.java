package in.xnnyygn.concurrent.listset;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FineGrainedList<T> {

    private final Node<T> head;

    public FineGrainedList() {
        head = new Node<>(Integer.MIN_VALUE);
        head.next = new Node<>(Integer.MAX_VALUE);
    }

    public boolean add(T item) {
        int key = item.hashCode();
        Node<T> predecessor = head;
        Node<T> current;
        predecessor.lock();
        try {
            current = predecessor.next;
            current.lock();
            try {
                while (current.key < key) {
                    predecessor.unlock();
                    predecessor = current;
                    current = current.next;
                    current.lock();
                }
                if (current.key == key) {
                    return false;
                }
                Node<T> node = new Node<>(key, item);
                node.next = current;
                predecessor.next = node;
                return true;
            } finally {
                current.unlock();
            }
        } finally {
            predecessor.unlock();
        }
    }

    public boolean remove(T item) {
        int key = item.hashCode();
        Node<T> predecessor = head;
        Node<T> current;
        predecessor.lock();
        try {
            current = predecessor.next;
            current.lock();
            try {
                while (current.key < key) {
                    predecessor.unlock();
                    predecessor = current;
                    current = current.next;
                    current.lock();
                }
                if (current.key != key) {
                    return false;
                }
                predecessor.next = current.next;
                return true;
            } finally {
                current.unlock();
            }
        } finally {
            predecessor.unlock();
        }
    }

    public boolean contains(T item) {
        int key = item.hashCode();
        Node<T> predecessor = head;
        Node<T> current;
        predecessor.lock();
        try {
            current = predecessor.next;
            current.lock();
            try {
                while (current.key < key) {
                    predecessor.unlock();
                    predecessor = current;
                    current = current.next;
                    current.lock();
                }
                return current.key == key;
            } finally {
                current.unlock();
            }
        } finally {
            predecessor.unlock();
        }
    }

    static class Node<T> {
        final Lock lock = new ReentrantLock();
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

        void lock() {
            lock.lock();
        }

        void unlock() {
            lock.unlock();
        }
    }
}
