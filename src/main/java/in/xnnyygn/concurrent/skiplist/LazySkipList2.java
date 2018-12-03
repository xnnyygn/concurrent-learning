package in.xnnyygn.concurrent.skiplist;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("Duplicates")
public class LazySkipList2<T> {

    private static final int MAX_LEVEL = 5;
    private int highestLevel = MAX_LEVEL;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Node<T> head;
    private final Node<T> tail;

    public LazySkipList2() {
        head = new Node<>(Integer.MIN_VALUE);
        tail = new Node<>(Integer.MAX_VALUE);
        for (int level = 0; level <= MAX_LEVEL; level++) {
            head.next[level] = tail;
        }
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

    private void updateHighestLevel() {
        head.lock();
        try {
            for (int level = MAX_LEVEL; level >= 0; level--) {
                if (head.next[level] != tail) {
                    highestLevel = level;
                    break;
                }
            }
        } finally {
            head.unlock();
        }
    }

    private int find(T x, Node<T>[] predecessors, Node<T>[] successors) {
        int key = x.hashCode();
        int levelFound = -1;
        Node<T> predecessor = head;
        Node<T> current;
        for (int level = highestLevel; level >= 0; level--) {
            current = predecessor.next[level];
            while (key > current.key) {
                predecessor = current;
                current = current.next[level];
            }
            if (levelFound == -1 && current.key == key && current.values.contains(x)) {
                levelFound = level;
            }
            predecessors[level] = predecessor;
            successors[level] = current;
        }
        return levelFound;
    }

    @SuppressWarnings("unchecked")
    public boolean add(T x) {
        int topLevel = random.nextInt(MAX_LEVEL) + 1;
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
                    nodeFound.lock();
                    try {
                        if (nodeFound.values.contains(x)) {
                            return false;
                        }
                        nodeFound.values.add(x);
                        return true;
                    }finally {
                        nodeFound.unlock();
                    }
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
                updateHighestLevel();
            }
        }
    }

    @SuppressWarnings({"unchecked"})
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
                if (victim.marked || !victim.values.contains(x)) {
                    victim.unlock();
                    return false;
                }

                victim.values.remove(x);
                if(!victim.values.isEmpty()) {
                    victim.unlock();
                    return true;
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
                updateHighestLevel();
            }
        }
    }

    private static class Node<T> {
        private final Lock lock = new ReentrantLock();
        private final int key;
        private final List<T> values;
        private final Node<T>[] next;
        private volatile boolean marked = false;
        private volatile boolean fullyLinked = false;
        private final int topLevel;

        @SuppressWarnings("unchecked")
        Node(int key) {
            this.key = key;
            values = Collections.emptyList();
            next = (Node<T>[]) new Node[MAX_LEVEL + 1];
            topLevel = MAX_LEVEL;
        }

        @SuppressWarnings("unchecked")
        Node(T x, int height) {
            key = x.hashCode();
            values = new LinkedList<>();
            values.add(x);
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
