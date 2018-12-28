package in.xnnyygn.concurrent.skiplist;

import com.google.common.base.Preconditions;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicMarkableReference;

@SuppressWarnings("Duplicates")
public class SkipListSet1<T> {

    private static final int MAX_LEVEL = 5;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Node<T> head;

    public SkipListSet1() {
        head = new Node<>(null, Integer.MIN_VALUE, MAX_LEVEL);
        final Node<T> tail = new Node<>(null, Integer.MAX_VALUE, MAX_LEVEL);
        for (int i = 0; i < MAX_LEVEL; i++) {
            head.next[i] = new AtomicMarkableReference<>(tail, false);
        }
    }

    /**
     * Test if element in set.
     *
     * @param x element
     * @return true if present, otherwise false
     */
    public boolean contains(T x) {
        Preconditions.checkNotNull(x);
        final int key = x.hashCode();
        Node<T> predecessor = head;
        Node<T> current = null;
        Node<T> successor;
        boolean[] markHolder = new boolean[1];
        for (int i = MAX_LEVEL - 1; i >= 0; i--) {
            current = predecessor.next(i);
            while (true) {
                successor = current.next[i].get(markHolder);
                while (markHolder[0]) {
                    current = successor;
                    successor = current.next[i].get(markHolder);
                }
                if (current.key < key) {
                    predecessor = current;
                    current = current.next(i);
                } else {
                    break;
                }
            }
        }
        return current.key == key;
    }

    /**
     * Add element.
     *
     * @param x element
     * @return true if added, false if present
     */
    @SuppressWarnings("unchecked")
    public boolean add(T x) {
        Preconditions.checkNotNull(x);
        final int key = x.hashCode();
        final int level = random.nextInt(MAX_LEVEL) + 1;
        final Node<T> node = new Node<>(x, key, level);
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL];
        boolean success;
        while (true) {
            if (find(key, predecessors, successors)) {
                return false;
            }
            node.next[0] = new AtomicMarkableReference<>(successors[0], false);
            success = predecessors[0].next[0].compareAndSet(successors[0], node, false, false);
            if (!success) {
                continue; // retry
            }
            // node added to skip list if success

            for (int i = 1; i < level; i++) {
                while (true) {
                    node.next[i] = new AtomicMarkableReference<>(successors[i], false);
                    success = predecessors[i].next[i].compareAndSet(successors[i], node, false, false);
                    if (success) {
                        break;
                    } else {
                        find(key, predecessors, successors);
                    }
                }
            }
            return true;
        }
    }

    /**
     * Find element in skip list and return predecessors and successors.
     *
     * @param key key
     * @return true if found, otherwise false
     */
    private boolean find(int key, Node<T>[] predecessors, Node<T>[] successors) {
        Node<T> predecessor = head;
        Node<T> current = null;
        Node<T> successor;

        boolean[] markHolder = new boolean[1];
        boolean snip;

        restart:
        for (int i = MAX_LEVEL - 1; i >= 0; i--) {
            current = predecessor.next(i);
            // find first node
            // 1. not marked
            // 2. node's key is not greater than specified key
            while (true) {
                successor = current.next[i].get(markHolder);
                while (markHolder[0]) {
                    snip = predecessor.next[i].compareAndSet(current, successor, false, false);
                    if (!snip) {
                        continue restart;
                    }
                    current = successor;
                    successor = current.next[i].get(markHolder);
                }
                // current is not marked
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

    /**
     * Remove element.
     *
     * @param x element
     * @return true if success, false if not found or removed
     */
    @SuppressWarnings("unchecked")
    public boolean remove(T x) {
        final int key = x.hashCode();
        Node<T>[] predecessors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T>[] successors = (Node<T>[]) new Node[MAX_LEVEL];
        Node<T> node;
        Node<T> next;
        boolean[] markHolder = new boolean[1];
        boolean success;

        if (!find(key, predecessors, successors)) {
            return false; // not found
        }
        node = successors[0];
        for (int i = node.level() - 1; i >= 1; i--) {
            next = node.next[i].get(markHolder);
            while (!markHolder[0]) {
                success = node.next[i].attemptMark(next, true);
                if (success) {
                    break;
                }
                next = node.next[i].get(markHolder);
            }
        }
        while (true) {
            next = node.next[0].get(markHolder);
            // logically removed
            if (markHolder[0]) {
                return false;
            }
            success = node.next[0].attemptMark(next, true);
            if (success) {
                find(key, predecessors, successors);
                return true;
            }
        }
    }

    private static final class Node<T> {
        final T value;
        final int key;
        final AtomicMarkableReference<Node<T>>[] next;

        @SuppressWarnings("unchecked")
        Node(T value, int key, int level) {
            this.value = value;
            this.key = key;
            next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[level];
        }

        Node<T> next(int level) {
            return next[level].getReference();
        }

        int level() {
            return next.length;
        }
    }
}
