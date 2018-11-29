package in.xnnyygn.concurrent.skiplist;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LazySkipList<T> {

    private static final int MAX_LEVEL = 5;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Node<T> head;
    private final Node<T> tail;

    public LazySkipList() {
        head = new Node<>(Integer.MIN_VALUE);
        tail = new Node<>(Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    public boolean contains(T x) {
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        int levelFound = find(x, predecessors, successors);
        if (levelFound != -1) {
            Node<T> node = successors[levelFound];
            return !node.marked && node.fullyLinked;
        }
        return false;
    }

    private int find(T x, Node<T>[] predecessors, Node<T>[] successors) {
        int key = x.hashCode();
        int levelFound = -1;
        Node<T> predecessor = head;
        Node<T> current;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            current = predecessor.next[level];
            while (key > current.key) {
                predecessor = current;
                current = current.next[level];
            }
            if (levelFound == -1 && current.key == key) {
                levelFound = level;
            }
            predecessors[level] = predecessor;
            successors[level] = current;
        }
        return levelFound;
    }

    @SuppressWarnings("unchecked")
    public boolean add(T x) {
        int topLevel = random.nextInt(MAX_LEVEL);
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        int levelFound;
        Node<T> nodeFound;
        int highestLocked;
        Node<T> predecessor;
        Node<T> successor;
        boolean valid;
        Node<T> node;
        int level;

        while (true) {
            levelFound = find(x, predecessors, successors);
            if (levelFound != -1) { // node found
                nodeFound = successors[levelFound];
                // check if node is marked(removed)
                if (!nodeFound.marked) {
                    // wait for another thread to complete linking
                    while (!nodeFound.fullyLinked) {
                    }
                    return false;
                }
                continue;
            }

            highestLocked = -1;
            valid = true;
            try {
                for (level = 0; valid && (level <= topLevel); level++) {
                    predecessor = predecessors[level];
                    successor = successors[level];
                    predecessor.lock();
                    highestLocked = level;
                    valid = !predecessor.marked && !successor.marked && predecessor.next[level] == successor;
                }
                if (!valid) {
                    continue;
                }
                node = new Node(x, topLevel);
                for (level = 0; level <= topLevel; level++) {
                    node.next[level] = successors[level];
                }
                for (level = 0; level <= topLevel; level++) {
                    predecessors[level].next[level] = node;
                }
                node.fullyLinked = true;
                return true;
            } finally {
                for (level = 0; level <= highestLocked; level++) {
                    predecessors[level].unlock();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean remove(T x) {
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        int levelFound;
        boolean marked = false;
        int topLevel;
        Node<T> victim = null;
        Node<T> predecessor;
        boolean valid;
        int highestLevel;
        int level;
        while (true) {
            levelFound = find(x, predecessors, successors);
            if (levelFound == -1) {
                return false;
            }
            victim = successors[levelFound];
            // not marked by current thread
            if (!marked && (!victim.fullyLinked || victim.topLevel != levelFound || victim.marked)) {
                return false;
            }
            if (!marked) {
                // victim's lock is required because lock coupling
                victim.lock();
                // re-check, victim must be removed by another thread
                if (victim.marked) {
                    victim.unlock();
                    return false;
                }
            }
            // mark and process
            victim.marked = true;
            marked = true;

            topLevel = victim.topLevel;
            valid = true;
            highestLevel = -1;
            try {
                for (level = 0; (valid && level <= topLevel); level++) {
                    predecessor = predecessors[level];
                    predecessor.lock();
                    highestLevel = level;
                    valid = !predecessor.marked && predecessor.next[level] == victim;
                }
                if (!valid) {
                    continue;
                }
                for (level = topLevel; level >= 0; level--) {
                    predecessors[level].next[level] = victim.next[level];
                }
                victim.unlock();
                return true;
            } finally {
                for (level = 0; level <= highestLevel; level++) {
                    predecessors[level].unlock();
                }
            }
        }
    }

    private static class Node<T> {
        private final Lock lock = new ReentrantLock();
        private final int key;
        private final T value;
        private final Node<T>[] next;
        private volatile boolean marked = false;
        private volatile boolean fullyLinked = false;
        private final int topLevel;

        @SuppressWarnings("unchecked")
        Node(int key) {
            this.key = key;
            value = null;
            next = (Node<T>[]) new Node[MAX_LEVEL + 1];
            topLevel = MAX_LEVEL;
        }

        @SuppressWarnings("unchecked")
        Node(T x, int height) {
            key = x.hashCode();
            value = x;
            next = (Node<T>[]) new Node[height + 1];
            topLevel = height;
        }

        void lock() {
            lock.lock();
        }

        void unlock() {
            lock.unlock();
        }
    }
}
