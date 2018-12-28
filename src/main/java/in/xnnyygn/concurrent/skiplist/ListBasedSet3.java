package in.xnnyygn.concurrent.skiplist;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 1. HM linked list using marker node
 * 2. no tail node
 * 3. comparator
 *
 * @param <T> element type
 */
@SuppressWarnings("Duplicates")
public class ListBasedSet3<T> {

    private final Comparator<T> comparator;
    private final Node<T> head;

    public ListBasedSet3(Comparator<T> comparator) {
        this.comparator = comparator;
        head = new Node<>(NodeKind.HEAD, null, null);
    }

    private int compareItem(Node<T> node1, T item) {
        switch (node1.kind) {
            case HEAD:
                return -1;
            case NORMAL:
                return comparator.compare(node1.item, item);
            default:
                throw new IllegalStateException("cannot compareItem");
        }
    }

    public boolean contains(T item) {
        int c;
        for (Node<T> current = head.next.get(), successor; ; ) {
            if (current == null) {
                // predecessor is last node
                return false;
            }
            // current.kind never be head
            assert current.kind != NodeKind.HEAD;
            successor = current.next.get();
            if (current.kind != NodeKind.MARKER) { // skip marker
                c = compareItem(current, item);
                if (c == 0) {
                    // current.item == item
                    // current.kind = normal
                    // item present if current is not marked
                    return successor == null || successor.kind != NodeKind.MARKER;
                }
                if (c > 0) {
                    // current.item > item
                    // not found
                    return false;
                }
            }
            // 1. current.kind == marker
            // 2. current.item < item
            // go next
            current = successor;
        }
    }

    public boolean add(T item) {
        int c;
        boolean[] markHolder = {false};

        restart:
        while (true) {
            for (Node<T> predecessor = head, current = predecessor.next.get(), successor; ; ) {
                if (current == null) {
                    // no more node
                    if (insert(predecessor, item, null)) {
                        return true;
                    }
                    continue restart;
                }
                successor = current.next(markHolder);
                // current is deleted
                while (markHolder[0]) {
                    if (!predecessor.next.compareAndSet(current, successor)) {
                        continue restart;
                    }
                    current = successor;
                    if (current == null) {
                        // no more node
                        if (insert(predecessor, item, null)) {
                            return true;
                        }
                        continue restart;
                    }
                    successor = current.next(markHolder);
                }
                c = compareItem(current, item);
                if (c == 0) {
                    // same key, item will not be replaced
                    return false;
                }
                if (c > 0) {
                    // current.item < item
                    if (insert(predecessor, item, successor)) {
                        return true;
                    }
                    continue restart;
                }
                predecessor = current;
                current = successor;
            }
        }
    }

    private boolean insert(Node<T> predecessor, T item, Node<T> successor) {
        Node<T> newNode = new Node<>(NodeKind.NORMAL, item, successor);
        return predecessor.next.compareAndSet(successor, newNode);
    }

    public boolean remove(T item) {
        int c;
        boolean[] markHolder = {false};
        Node<T> marker;

        restart:
        while (true) {
            for (Node<T> predecessor = head, current = predecessor.next.get(), successor; ; ) {
                if (current == null) {
                    // no more node
                    return false;
                }
                successor = current.next(markHolder);
                while (markHolder[0]) {
                    if (!predecessor.next.compareAndSet(current, successor)) {
                        continue restart;
                    }
                    current = successor;
                    if (current == null) {
                        // no more node
                        return false;
                    }
                    successor = current.next(markHolder);
                }
                c = compareItem(current, item);
                if (c == 0) {
                    marker = new Node<>(NodeKind.MARKER, null, successor);
                    // logical remove
                    if (current.next.compareAndSet(successor, marker)) {
                        // physical remove
                        predecessor.next.compareAndSet(current, successor);
                        return true;
                    }
                    continue restart;
                }
                if (c > 0) {
                    // not found
                    return false;
                }
                predecessor = current;
                current = successor;
            }
        }
    }

    private enum NodeKind {
        HEAD,
        NORMAL,
        MARKER
    }

    private static final class Node<T> {
        final NodeKind kind;
        final T item;
        final AtomicReference<Node<T>> next;

        Node(NodeKind kind, T item, Node<T> next) {
            this.kind = kind;
            this.item = item;
            this.next = new AtomicReference<>(next);
        }

        Node<T> next(boolean[] markHolder) {
            Node<T> successor = next.get();
            if (successor == null) {
                // last element
                markHolder[0] = false;
                return null;
            }
            if (successor.kind == NodeKind.MARKER) {
                markHolder[0] = true;
                return successor.next.get();
            }
            markHolder[0] = false;
            return successor;
        }
    }

}
