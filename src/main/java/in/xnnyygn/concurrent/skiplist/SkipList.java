package in.xnnyygn.concurrent.skiplist;

import java.util.Arrays;
import java.util.Random;

public class SkipList<T> {

    private static final int MAX_LEVEL = 5;
    private static final int NOT_FOUND = -1;

    private final Random random = new Random();
    private final Node<T> head;
    private final Node<T> tail;

    public SkipList() {
        head = new Node<>(Integer.MIN_VALUE);
        tail = new Node<>(Integer.MAX_VALUE);
        for (int level = 0; level < MAX_LEVEL; level++) {
            head.next[level] = tail;
        }
    }

    public boolean contains(T x) {
        int key = x.hashCode();
        Node<T> predecessor = head;
        Node<T> current;
        for (int level = MAX_LEVEL; level >= 0; level--) {
            current = predecessor.next[level];
            while (current.key < key) {
                predecessor = current;
                current = current.next[level];
            }
            if (current.key == key) {
                return true;
            }
        }
        return false;
    }

    private int find(T x, Node<T>[] predecessors, Node<T>[] successors) {
        int key = x.hashCode();
        int foundAtLevel = NOT_FOUND;
        Node<T> predecessor = head;
        Node<T> current;
        for (int level = MAX_LEVEL - 1; level >= 0; level--) {
            current = predecessor.next[level];
            while (current.key < key) {
                predecessor = current;
                current = current.next[level];
            }
            predecessors[level] = predecessor;
            successors[level] = current;
            if (foundAtLevel == NOT_FOUND && current.key == key) {
                foundAtLevel = level;
            }
        }
        return foundAtLevel;
    }

    @SuppressWarnings("unchecked")
    public boolean add(T x) {
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL];
        int foundAtLevel = find(x, predecessors, successors);
        if (foundAtLevel != NOT_FOUND) {
            return false;
        }
        int topLevel = random.nextInt(MAX_LEVEL);
        Node<T> node = new Node<>(x.hashCode(), x, topLevel);
        for (int level = 0; level < topLevel; level++) {
            node.next[level] = successors[level];
            predecessors[level].next[level] = node;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean remove(T x) {
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL];
        int foundAtLevel = find(x, predecessors, successors);
        if (foundAtLevel == NOT_FOUND) {
            return false;
        }
        Node<T> node = successors[foundAtLevel];
        for (int level = 0; level <= foundAtLevel; level++) {
            predecessors[level].next[level] = node.next[level];
        }
        return true;
    }

    private static class Node<T> {
        private final int key;
        private final T value;
        private final Node<T>[] next;

        @SuppressWarnings("unchecked")
        Node(int key) {
            this.key = key;
            value = null;
            next = (Node<T>[]) new Node[MAX_LEVEL];
        }

        // topLevel >= 1 && topLevel <= MAX_LEVEL
        @SuppressWarnings("unchecked")
        Node(int key, T value, int topLevel) {
            this.key = key;
            this.value = value;
            next = (Node<T>[]) new Node[topLevel];
        }

        int topLevel() {
            return next.length;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }
}
