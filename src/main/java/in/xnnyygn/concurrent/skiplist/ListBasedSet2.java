package in.xnnyygn.concurrent.skiplist;

import java.util.concurrent.atomic.AtomicReference;

/**
 * HM linked list using marker node.
 *
 * @param <T> element type
 */
public class ListBasedSet2<T> {

    private final Node<T> head;

    ListBasedSet2() {
        Node<T> tail = new Node<>(NodeKind.NORMAL, Integer.MAX_VALUE, null, null);
        head = new Node<>(NodeKind.NORMAL, Integer.MIN_VALUE, null, tail);
    }

    public boolean contains(T item) {
        final int key = item.hashCode();
        Node<T> current = head.next();
        boolean[] markHolder = {false};
        while (current.key < key) {
            current = current.next(markHolder); // skip marker node
            assert current != null;
        }
        return current.key == key && !markHolder[0];
    }

    @SuppressWarnings("unchecked")
    public boolean add(T item) {
        final int key = item.hashCode();
        Node<T>[] nodes = (Node<T>[]) new Node[3];
        while (true) {
            // same key
            if (find(key, nodes)) {
                return false;
            }
            // p -> (n) -> c -> s
            Node<T> newNode = new Node<>(NodeKind.NORMAL, key, item, nodes[1]);
            if (nodes[0].next.compareAndSet(nodes[1], newNode)) {
                return true;
            }
        }
    }

    private boolean find(int key, Node<T>[] nodes) {
        Node<T> predecessor;
        Node<T> current;
        Node<T> successor;

        boolean[] markHolder = {false};
        boolean snip;

        restart:
        for (predecessor = head, current = predecessor.next(); ; ) {
            assert current != null;
            successor = current.next(markHolder);
            while (markHolder[0]) {
                assert successor != null;
                snip = predecessor.next.compareAndSet(current, successor);
                if (!snip) {
                    continue restart;
                }
                current = successor;
                successor = current.next(markHolder);
            }
            if (current.key < key) {
                predecessor = current;
                current = successor;
            } else {
                break;
            }
        }

        nodes[0] = predecessor;
        nodes[1] = current;
        nodes[2] = successor;
        return current.key == key;
    }

    @SuppressWarnings("unchecked")
    public boolean remove(T item) {
        final int key = item.hashCode();
        Node<T>[] nodes = (Node<T>[]) new Node[3];

        while (true) {
            if (!find(key, nodes)) {
                return false;
            }
            // p -> [c -> (m)] -> s
            // p -> s
            Node<T> marker = new Node<>(NodeKind.MARKER, 0, null, nodes[2]);
            if (nodes[1].next.compareAndSet(nodes[2], marker)) {
                nodes[0].next.compareAndSet(nodes[1], nodes[2]);
                return true;
            }
        }
    }

    @Override
    public String toString() {
        return "ListBasedSet2{" +
                "head=" + head +
                '}';
    }

    private enum NodeKind {
        NORMAL,
        MARKER;
    }

    private static final class Node<T> {
        final NodeKind kind;
        final int key;
        final T item;
        final AtomicReference<Node<T>> next;

        Node(NodeKind kind, int key, T item, Node<T> next) {
            this.kind = kind;
            this.key = key;
            this.item = item;
            this.next = new AtomicReference<>(next);
        }

        Node<T> next() {
            return next.get();
        }

        Node<T> next(boolean[] markHolder) {
            Node<T> successor = next.get();
            if (successor == null) { // tail
                return null;
            }
            if (successor.kind == NodeKind.MARKER) {
                markHolder[0] = true;
                return successor.next();
            }
            markHolder[0] = false;
            return successor;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Node{");
            if (kind == NodeKind.NORMAL) {
                builder.append("kind=NORMAL,");
                builder.append("key=").append(key).append(',');
                builder.append("item=").append(item).append(',');
            } else {
                builder.append("kind=MARKER,");
            }
            builder.append("next=\n").append(next.get()).append('}');
            return builder.toString();
        }
    }
}
