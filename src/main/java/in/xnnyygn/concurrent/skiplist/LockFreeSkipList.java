package in.xnnyygn.concurrent.skiplist;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicMarkableReference;

@SuppressWarnings("Duplicates")
public class LockFreeSkipList<T> {

    private static final int MAX_LEVEL = 5;
    private static final int NOT_FOUND = -1;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Node<T> head;

    public LockFreeSkipList() {
        head = new Node<>(Integer.MIN_VALUE);
        Node<T> tail = new Node<>(Integer.MAX_VALUE);
        for (int level = 0; level <= MAX_LEVEL; level++) {
            head.next[level] = new AtomicMarkableReference<>(tail, false);
        }
    }

    public boolean contains(T x) {
        int key = x.hashCode();
        Node<T> node0 = head;
        Node<T> node1 = null;
        Node<T> node2;
        boolean[] markHolder = new boolean[1];
        for (int level = MAX_LEVEL; level >= 0; level--) {
            node1 = node0.next(level);
            while (true) {
                node2 = node1.nextAndMark(level, markHolder);
                while (markHolder[0]) {
                    // node1 = node1.next(level); // read again?
                    node1 = node2;
                    node2 = node1.nextAndMark(level, markHolder);
                }
                if (node1.key < key) {
                    node0 = node1;
                    node1 = node2;
                } else {
                    break;
                }
            }
        }
        return node1.key == key;
    }

    private boolean find(T x, Node<T>[] predecessors, Node<T>[] successors) {
        int key = x.hashCode();
        Node<T> predecessor = head;
        Node<T> current = null;
        Node<T> successor;
        boolean[] markHolder = new boolean[1];
        boolean snip;

        retry:
        for (int level = MAX_LEVEL; level >= 0; level--) {
            current = predecessor.next(level);
            while (true) {
                successor = current.nextAndMark(level, markHolder);
                while (markHolder[0]) {
                    snip = predecessor.next[level].compareAndSet(current, successor, false, false);
                    if (!snip) {
                        continue retry;
                    }
                    current = predecessor.next(level);
                    successor = current.nextAndMark(level, markHolder);
                }

                if (current.key < key) {
                    predecessor = current;
                    current = successor;
                } else {
                    break;
                }
            }
            predecessors[level] = predecessor;
            successors[level] = current;
        }
        return current.key == key;
    }

    private boolean find2(T x, Node<T>[] predecessors, Node<T>[] successors) {
        int key = x.hashCode();
        Node<T> node0 = head;
        Node<T> node1 = null;
        Node<T> node2;
        Node<T> node3;
        boolean[] markHolder = new boolean[1];

        retry:
        for (int level = MAX_LEVEL; level >= 0; level--) {
            while (true) {
                node1 = node0.next(level);
                node2 = node1;
                node3 = node2.next[level].get(markHolder);
                while (markHolder[0]) {
                    node2 = node3;
                    node3 = node2.next[level].get(markHolder);
                }
                node1 = node0.next(level);
                if (node2 != node1) {
                    if (!node0.next[level].compareAndSet(node1, node2, false, false)) {
                        continue retry;
                    }
                    node1 = node2;
                }
                if (node1.key < key) {
                    node0 = node1;
                } else {
                    break;
                }
            }
            predecessors[level] = node0;
            successors[level] = node1;
        }
        return node1.key == key;
    }

    @SuppressWarnings("unchecked")
    public boolean add(T x) {
        int topLevel = random.nextInt(MAX_LEVEL) + 1;
        int key = x.hashCode();
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> node = new Node<>(key, x, topLevel);

        while (true) {
            if (find(x, predecessors, successors)) {
                return false;
            }
            for (int level = 0; level <= topLevel; level++) {
                node.next[level] = new AtomicMarkableReference<>(successors[level], false);
            }
            if (!predecessors[0].next[0].compareAndSet(successors[0], node, false, false)) {
                continue;
            }
            for (int level = 1; level <= topLevel; level++) {
                while (!predecessors[level].next[level].compareAndSet(successors[level], node, false, false)) {
                    find(x, predecessors, successors);
                }
            }
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean remove(T x) {
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL + 1];
        Node<T> next;
        boolean[] markHolder = new boolean[1];
        boolean iMarkedIt;

        if (!find(x, predecessors, successors)) {
            return false;
        }
        Node<T> node = successors[0];
        int topLevel = node.next.length;
        for (int level = topLevel - 1; level >= 1; level--) {
            next = node.next[level].get(markHolder);
            while (!markHolder[0]) {
                node.next[level].attemptMark(next, true);
                next = node.next[level].get(markHolder);
            }
        }
        next = node.next[0].get(markHolder);
        while (true) {
            iMarkedIt = node.next[0].compareAndSet(next, next, false, true);
            if (iMarkedIt) {
                find(x, predecessors, successors);
                return true;
            }
            next = node.next[0].get(markHolder);
            if (markHolder[0]) {
                return false;
            }
        }
    }

    private static class Node<T> {
        private final int key;
        private final T value;
        private final AtomicMarkableReference<Node<T>>[] next;

        @SuppressWarnings("unchecked")
        Node(int key) {
            this.key = key;
            value = null;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
        }

        @SuppressWarnings("unchecked")
        Node(int key, T value, int topLevel) {
            this.key = key;
            this.value = value;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[topLevel + 1];
        }

        Node<T> next(int level) {
            return next[level].getReference();
        }

        Node<T> nextUnmarked(int level, boolean[] snipHolder) {
            boolean[] markHolder = new boolean[1];
            Node<T> successor = next[level].getReference();
            Node<T> node1 = successor;
            Node<T> node2 = node1.next[level].get(markHolder);
            while (markHolder[0]) {
                node1 = node2;
                node2 = node2.next[level].get(markHolder);
            }
            if (node1 != successor) {
                snipHolder[0] = next[level].compareAndSet(successor, node1, false, false);
            } else {
                snipHolder[0] = false;
            }
            return node1;
        }

        Node<T> nextAndMark(int level, boolean[] markHolder) {
            return next[level].get(markHolder);
        }
    }
}
