package in.xnnyygn.concurrent.listset;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class FreeLockList<T> {

    private final Node<T> head;

    public FreeLockList() {
        head = new Node<>(Integer.MIN_VALUE, null, new Node<>(Integer.MAX_VALUE));
    }

    public boolean add(T item) {
        int key = item.hashCode();
        Window<T> window;
        while (true) {
            window = find(head, key);
            if (window.currentKey() == key) {
                return false;
            }
            Node<T> node = new Node<>(key, item, window.current);
            if (window.casPredecessor(node)) {
                return true;
            }
        }
    }

    public boolean remove(T item) {
        int key = item.hashCode();
        Window<T> window;
        while (true) {
            window = find(head, key);
            if (window.currentKey() != key) {
                return false;
            }
            if (!window.attemptMarkCurrent()) {
                continue; // retry
            }
            window.casPredecessor(window.successor());
            return true;
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
        Node<T> predecessor;
        Node<T> current;
        Node<T> successor;
        boolean[] markedHolder = {false};
        boolean snip;

        retry:
        while (true) {
            predecessor = head;
            current = predecessor.next();

            while (true) {
                successor = current.nextAndMarked.get(markedHolder);

                // current node is deleted
                while (markedHolder[0]) {
                    snip = predecessor.nextAndMarked.compareAndSet(current, successor, false, false);
                    if (!snip) {
                        continue retry;
                    }

                    current = successor;
                    successor = current.nextAndMarked.get(markedHolder);
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

        int currentKey() {
            return current.key;
        }

        boolean casPredecessor(Node<T> node) {
            return predecessor.nextAndMarked.compareAndSet(current, node, false, false);
        }

        Node<T> successor() {
            return current.next();
        }

        boolean attemptMarkCurrent() {
            Node<T> successor = current.next();
            return current.nextAndMarked.attemptMark(successor, true);
        }
    }

    private static class Node<T> {
        private final int key;
        private T value;
        private final AtomicMarkableReference<Node<T>> nextAndMarked;

        Node(int key) {
            this.key = key;
            nextAndMarked = new AtomicMarkableReference<>(null, false);
        }

        Node(int key, T value, Node<T> next) {
            this.key = key;
            this.value = value;
            nextAndMarked = new AtomicMarkableReference<>(next, false);
        }

        boolean isMarked() {
            return nextAndMarked.isMarked();
        }

        Node<T> next() {
            return nextAndMarked.getReference();
        }
    }
}
