package in.xnnyygn.concurrent.listset;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * HM linked list based on {@code AtomicMarkableReference}.
 *
 * @param <T>
 */
public class LockFreeList<T> {

    private final Node<T> head;

    public LockFreeList() {
        head = new Node<>(Integer.MIN_VALUE, null, new Node<>(Integer.MAX_VALUE));
    }

    public boolean add(T item) {
        final int key = item.hashCode();
        Window<T> window;
        Node<T> newNode;
        while (true) {
            window = find(head, key);
            // same key
            if (window.current.key == key) {
                return false;
            }
            newNode = new Node<>(key, item, window.current);
            if (window.predecessor.nextAndMark.compareAndSet(
                    window.current, newNode, false, false)) {
                return true;
            }
            // retry
        }
    }

    public boolean remove(T item) {
        final int key = item.hashCode();
        Window<T> window;
        Node<T> successor;
        while (true) {
            window = find(head, key);
            // not found
            if (window.current.key != key) {
                return false;
            }
            successor = window.current.nextAndMark.getReference();
            // logical delete
            if (window.current.nextAndMark.attemptMark(successor, true)) {
                // physical delete
                window.predecessor.nextAndMark.compareAndSet(
                        window.current, successor, false, false);
                // fail is ok
                return true;
            }
            // retry
        }
    }

    public boolean contains(T item) {
        int key = item.hashCode();
        Node<T> current = head.next();
        while (current.key < key) {
            current = current.next();
        }
        return current.key == key && !current.isMarked();
    }

    private Window<T> find(Node<T> head, int key) {
        boolean[] markedHolder = {false};
        boolean snip;

        retry:
        for (Node<T> predecessor = head, current = predecessor.next(), successor; ; ) {
            while (true) {
                successor = current.nextAndMark.get(markedHolder);

                // current node is deleted
                while (markedHolder[0]) {
                    snip = predecessor.nextAndMark.compareAndSet(
                            current, successor, false, false);
                    if (!snip) {
                        continue retry;
                    }

                    current = successor;
                    successor = current.nextAndMark.get(markedHolder);
                }

                if (current.key >= key) {
                    return new Window<>(predecessor, current);
                }

                predecessor = current;
                current = successor;
            }
        }
    }


    private static class Window<T> {
        private final Node<T> predecessor;
        private final Node<T> current;

        Window(Node<T> predecessor, Node<T> current) {
            this.predecessor = predecessor;
            this.current = current;
        }
    }

    private static class Node<T> {
        private final int key;
        private final T item;
        private final AtomicMarkableReference<Node<T>> nextAndMark;

        Node(int key) {
            this.key = key;
            this.item = null;
            nextAndMark = new AtomicMarkableReference<>(null, false);
        }

        Node(int key, T item, Node<T> next) {
            this.key = key;
            this.item = item;
            nextAndMark = new AtomicMarkableReference<>(next, false);
        }

        boolean isMarked() {
            return nextAndMark.isMarked();
        }

        Node<T> next() {
            return nextAndMark.getReference();
        }
    }
}
