package in.xnnyygn.concurrent.skiplist;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class SkipListSet2<T> {

    private static final int MAX_LEVEL = 5; // 0 ~ 4

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Node<T> head;

    public SkipListSet2() {
        Node<T> tail = new Node<>(NodeKind.NORMAL, Integer.MAX_VALUE, null, MAX_LEVEL);
        head = new Node<>(NodeKind.NORMAL, Integer.MIN_VALUE, null, MAX_LEVEL);
        for (int i = 0; i < MAX_LEVEL; i++) {
            head.next[i] = new AtomicReference<>(tail);
        }
    }

    public boolean contains(T x) {
        final int key = x.hashCode();
        Node<T> predecessor = head;
        Node<T> current = null;
        Node<T> successor;
        boolean[] markHolder = {false};
        for (int i = MAX_LEVEL - 1; i >= 0; i--) {
            current = predecessor.next(i, markHolder); // predecessor not check
            while (true) {
                assert current != null;
                successor = current.next(i, markHolder);
                while (markHolder[0]) {
                    assert successor != null;
                    current = successor;
                    successor = current.next(i, markHolder);
                }
                if (current.key < key) {
                    predecessor = current;
                    current = successor;
                } else {
                    break;
                }
            }
        }
        return current.key == key;
    }

    @SuppressWarnings("unchecked")
    public boolean add(T x) {
        final int key = x.hashCode();
        final int level = random.nextInt(MAX_LEVEL) + 1;
        final Node<T> newNode = new Node<>(NodeKind.NORMAL, key, x, level);
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL];
        boolean success;

        while (true) {
            if (find(key, predecessors, successors)) {
                return false;
            }
            newNode.next[0] = new AtomicReference<>(successors[0]);
            success = predecessors[0].next[0].compareAndSet(successors[0], newNode);
            if (!success) {
                continue;
            }

            for (int i = 1; i < MAX_LEVEL; i++) {
                while (true) {
                    newNode.next[i] = new AtomicReference<>(successors[i]);
                    success = predecessors[i].next[i].compareAndSet(successors[i], newNode);
                    if (success) {
                        break;
                    }
                    find(key, predecessors, successors);
                }
            }
            return true;
        }
    }

    private boolean find(int key, Node<T>[] predecessors, Node<T>[] successors) {
        Node<T> predecessor = head;
        Node<T> current = null;
        Node<T> successor;
        boolean[] markHolder = {false};
        boolean snip;

        restart:
        for (int i = MAX_LEVEL - 1; i >= 0; i--) {
            current = predecessor.next(i, markHolder);
            while (true) {
                assert current != null;
                successor = current.next(i, markHolder);
                while (markHolder[0]) {
                    assert successor != null;
                    snip = predecessor.next[i].compareAndSet(current, successor);
                    if (!snip) {
                        continue restart;
                    }
                    current = successor;
                    successor = current.next(i, markHolder);
                }
                if (current.key < key) {
                    predecessor = current;
                    current = successor;
                } else {
                    break;
                }
            }
            predecessors[i] = predecessor;
            successors[i] = current;
        }
        return current.key == key;
    }

    @SuppressWarnings("unchecked")
    public boolean remove(T x) {
        final int key = x.hashCode();
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T> node;
        Node<T> successor;
        int level;
        boolean[] markHolder = {false};

        if (!find(key, predecessors, successors)) {
            return false;
        }
        node = successors[0];
        level = node.level();
        Node<T> marker = new Node<>(NodeKind.MARKER, 0, null, level);
        for (int i = level; i >= 1; i--) {
            do {
                successor = node.next(i, markHolder);
                if (markHolder[0]) {
                    break;
                }
                marker.next[i] = new AtomicReference<>(successor);
            } while (!node.next[i].compareAndSet(successor, marker));
        }
        while (true) {
            successor = node.next(0, markHolder);
            if (markHolder[0]) {
                return false;
            }
            marker.next[0] = new AtomicReference<>(successor);
            if (node.next[0].compareAndSet(successor, marker)) {
                find(key, predecessors, successors);
                return true;
            }
        }
    }

    private enum NodeKind {
        NORMAL,
        MARKER
    }

    private final static class Node<T> {
        final NodeKind kind;
        final int key;
        final T value;
        final AtomicReference<Node<T>>[] next;

        @SuppressWarnings("unchecked")
        Node(NodeKind kind, int key, T value, int level) {
            this.kind = kind;
            this.key = key;
            this.value = value;
            next = (AtomicReference<Node<T>>[]) new AtomicReference[level];
        }

        Node<T> next(int level, boolean[] markHolder) {
            Node<T> successor = next[level].get();
            if (successor == null) { // tail
                return null;
            }
            if (successor.kind == NodeKind.MARKER) {
                markHolder[0] = true;
                return successor.next[level].get();
            }
            markHolder[0] = false;
            return successor;
        }

        int level() {
            return next.length;
        }
    }

}
