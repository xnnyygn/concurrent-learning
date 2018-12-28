package in.xnnyygn.concurrent.skiplist;

import com.google.common.base.Preconditions;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 1. HM linked list using marker node
 * 2. no tail node
 * 3. comparator
 * 4. replaceable
 *
 * @param <T> element type
 */
@SuppressWarnings("Duplicates")
public class ListBasedSet4<T> {

    private final Comparator<T> comparator;
    private final Node<T> head;

    public ListBasedSet4(Comparator<T> comparator) {
        this.comparator = comparator;
        this.head = new Node<>(NodeKind.NORMAL, null, null);
    }

    public boolean contains(T item) {
        Preconditions.checkNotNull(item);

        int c;
        T value;

        for (Node<T> current = head.next.get(), successor; ; ) {
            if (current == null) {
                return false;
            }
            value = current.item.get();
            if (value == null) {
                // current is deleted
                successor = current.next.get();
                // skip marker if present
                if (successor != null && successor.kind == NodeKind.MARKER) {
                    successor = successor.next.get();
                }
                current = successor;
                continue;
            }
            c = comparator.compare(value, item);
            if (c == 0) {
                return true;
            }
            if (c > 0) {
                // current.item > item
                return false;
            }
            current = current.next.get();
        }
    }

    public boolean add(T item) {
        Preconditions.checkNotNull(item);

        int c;
        T value;
        Node<T> newNode;

        while (true) {
            for (Node<T> predecessor = head, current = predecessor.next.get(), successor; ; ) {
                if (current == null) {
                    // no more node
                    newNode = new Node<>(NodeKind.NORMAL, item, null);
                    if (predecessor.casNext(null, newNode)) {
                        return true;
                    }
                    break; // restart
                }
                value = current.item.get();
                if (value == null) {
                    // current is deleted
                    successor = current.nextNotMarker();
                    if (predecessor.casNext(current, successor)) {
                        current = successor;
                        continue;
                    } else {
                        break;
                    }
                }
                c = comparator.compare(value, item);
                if (c == 0) {
                    // same key, replace
                    if (current.casItem(value, item)) {
                        return true;
                    }
                    break;
                }
                successor = current.next.get();
                if (c > 0) {
                    // current.item > item
                    newNode = new Node<>(NodeKind.NORMAL, item, successor);
                    if (predecessor.casNext(current, newNode)) {
                        return true;
                    }
                    break;
                }
                predecessor = current;
                current = successor;
            }
        }
    }

    public boolean remove(T item) {
        Preconditions.checkNotNull(item);

        int c;
        T value;
        Node<T> marker;

        while (true) {
            for (Node<T> predecessor = head, current = predecessor.next.get(), successor; ; ) {
                if (current == null) {
                    // no more node
                    return false;
                }
                value = current.item.get();
                if (value == null) {
                    // current is deleted
                    successor = current.nextNotMarker();
                    if (predecessor.casNext(current, successor)) {
                        current = successor;
                        continue;
                    } else {
                        break; // restart
                    }
                }
                c = comparator.compare(value, item);
                if (c == 0) {
                    // the node to remove
                    if (!current.casItem(value, null)) {
                        break;
                    }
                    // successor never be a marker node
                    successor = current.next.get();
                    marker = new Node<>(NodeKind.MARKER, null, successor);
                    if (current.casNext(successor, marker)) {
                        predecessor.casNext(current, successor);
                        return true;
                    }
                    // successor may be deleted at the same time
                    break;
                }
                if (c > 0) {
                    // not found
                    return false;
                }
                predecessor = current;
                // current may be deleted at this point
                current = current.nextNotMarker();
            }
        }
    }

    private enum NodeKind {
        NORMAL,
        MARKER
    }

    @SuppressWarnings("Duplicates")
    private static final class Node<T> {
        final NodeKind kind;
        final AtomicReference<T> item;
        final AtomicReference<Node<T>> next;

        Node(NodeKind kind, T item, Node<T> next) {
            this.kind = kind;
            this.item = new AtomicReference<>(item);
            this.next = new AtomicReference<>(next);
        }

        Node<T> nextNotMarker() {
            Node<T> successor = next.get();
            if (successor == null) {
                return null;
            }
            if (successor.kind == NodeKind.MARKER) {
                return successor.next.get();
            }
            return successor;
        }

        boolean casItem(T expect, T update) {
            return item.compareAndSet(expect, update);
        }

        boolean casNext(Node<T> expect, Node<T> update) {
            return next.compareAndSet(expect, update);
        }
    }
}
