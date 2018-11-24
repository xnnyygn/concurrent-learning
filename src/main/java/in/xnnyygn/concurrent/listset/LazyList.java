package in.xnnyygn.concurrent.listset;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LazyList<T> {

    private final Node<T> head;

    public LazyList() {
        head = new Node<>(Integer.MIN_VALUE);
        head.next = new Node<>(Integer.MAX_VALUE);
    }

    public boolean add(T item) {
        int key = item.hashCode();
        Node<T> predecessor;
        Node<T> current;
        while (true) {
            predecessor = head;
            current = predecessor.next;
            while (current.key < key) {
                predecessor = current;
                current = current.next;
            }

            predecessor.lock();
            try {
                current.lock();
                try {
                    if (!validate(predecessor, current)) {
                        continue;
                    }
                    if (current.key == key) {
                        return false;
                    }
                    Node<T> node = new Node<>(key, item);
                    predecessor.next = node;
                    node.next = current;
                    return true;
                } finally {
                    current.unlock();
                }
            } finally {
                predecessor.unlock();
            }
        }
    }

    public boolean remove(T item) {
        int key = item.hashCode();
        Node<T> predecessor;
        Node<T> current;
        while (true) {
            predecessor = head;
            current = predecessor.next;
            while (current.key < key) {
                predecessor = current;
                current = current.next;
            }
            predecessor.lock();
            try {
                current.lock();
                try {
                    if (!validate(predecessor, current)) {
                        continue;
                    }
                    if (current.key != key) {
                        return false;
                    }
                    current.marked = true; // linearation point
                    predecessor.next = current.next;
                    return true;
                } finally {
                    current.unlock();
                }
            } finally {
                predecessor.unlock();
            }
        }
    }

    public boolean contains(T item) {
        int key = item.hashCode();
        Node<T> current = head.next;
        while (current.key < key) {
            current = current.next;
        }
        return current.key == key && !current.marked;
    }


    private boolean validate(Node<T> predecessor, Node<T> current) {
        return !predecessor.marked && predecessor.next == current && !current.marked;
    }

    private static class Node<T> {
        private final int key;
        private T value;
        private Node<T> next;
        private final Lock lock = new ReentrantLock();
        private volatile boolean marked = false;

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
