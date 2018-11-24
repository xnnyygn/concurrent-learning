package in.xnnyygn.concurrent.listset;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OptimisticList<T> {

    private final Node<T> head;

    public OptimisticList() {
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
                predecessor.unlock();
                current.unlock();
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
            current.lock();
            try {
                if (!validate(predecessor, current)) {
                    continue;
                }
                if (current.key != key) {
                    return false;
                }
                predecessor.next = current.next;
                return true;
            } finally {
                predecessor.unlock();
                current.unlock();
            }
        }
    }

    public boolean contains(T item) {
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
            current.lock();
            try {
                if (validate(predecessor, current)) {
                    return current.key == key;
                }
            } finally {
                predecessor.unlock();
                current.unlock();
            }
        }
    }

    private boolean validate(Node<T> predecessor, Node<T> current) {
        Node<T> node = head;
        while (node.key <= predecessor.key) {
            if (node.key == predecessor.key) {
                return node.next == current;
            }
            node = node.next;
        }
        return false;
    }

    private static class Node<T> {
        private final int key;
        private T value;
        private Node<T> next;
        private final Lock lock = new ReentrantLock();

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
