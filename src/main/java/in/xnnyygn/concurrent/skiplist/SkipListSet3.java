package in.xnnyygn.concurrent.skiplist;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("Duplicates")
public class SkipListSet3<T> {

    private final Comparator<T> comparator;
    private final AtomicReference<HeadIndex<T>> headIndex;

    public SkipListSet3(Comparator<T> comparator) {
        Preconditions.checkNotNull(comparator);
        this.comparator = comparator;

        Node<T> node = Node.ofNormal(null, null);
        headIndex = new AtomicReference<>(new HeadIndex<>(node, null, null, 1));
    }

    public boolean contains(T item) {
        Preconditions.checkNotNull(item);

        for (Node<T> node = findNode(item, false); node != null; node = node.nextNotMarker()) {
            T value = node.item.get();
            if (value == null) {
                // node is removed
                // no helping
                // go next
                continue;
            }
            int c = comparator.compare(value, item);
            if (c == 0) {
                // found
                return true;
            }
            if (c > 0) {
                // node.item > item
                // not found
                return false;
            }
        }
        // no more node
        return false;
    }

    public boolean contains2(T item) {
        Preconditions.checkNotNull(item);

        restart:
        while (true) {
            for (Node<T> predecessor = findNode(item, true),
                 current = predecessor.next.get(); current != null; ) {

                if (current.marker) {
                    // predecessor is deleted
                    continue restart;
                }
                T value = current.item.get();
                if (value == null) {
                    // current is deleted
                    helpDelete(predecessor, current);
                    continue restart;
                }
                int c = comparator.compare(value, item);
                if (c == 0) {
                    // found
                    return true;
                }
                if (c > 0) {
                    // current.item > item
                    // not found
                    return false;
                }
                // current.item < item
                predecessor = current;
                current = current.next.get();
            }
            return false;
        }
    }

    /**
     * Help delete current.
     *
     * @param predecessor predecessor
     * @param current     current
     */
    private void helpDelete(Node<T> predecessor, Node<T> current) {
        Node<T> successor = current.next.get();
        if (successor == null || !successor.marker) {
            // step 1 -> 2, insert marker, logical remove
            current.casNext(successor, Node.ofMarker(successor));
        } else {
            // step 2 -> 3, physical remove
            predecessor.casNext(current, successor.next.get());
        }
    }

    @Nonnull
    private Node<T> findNode(T item, boolean onlyPredecessor) {
        while (true) {
            for (Index<T> predecessor = headIndex.get(), current = predecessor.right.get(), successor, down; ; ) {
                if (current != null) {
                    T value = current.node.item.get();
                    if (value == null) {
                        // current is deleted
                        successor = current.right.get();
                        if (!predecessor.casRight(current, successor)) {
                            break; // restart
                        }
                        current = successor;
                        continue;
                    }
                    int c = comparator.compare(value, item);
                    if (c == 0) {
                        // found
                        return onlyPredecessor ? predecessor.node : current.node;
                    }
                    if (c < 0) {
                        // current.node.item < item
                        // go right
                        predecessor = current;
                        current = current.right.get();
                        continue;
                    }
                }
                // 1. current == null, no more index
                // 2. current.item > item
                // go down
                down = predecessor.down;
                if (down == null) {
                    // the last index level
                    return predecessor.node;
                }
                predecessor = down;
                current = predecessor.right.get();
            }
        }
    }

    public T add(T item) {
        Preconditions.checkNotNull(item);

        while (true) {
            for (Node<T> predecessor = findNode(item, true),
                 current = predecessor.nextNotMarker(), successor; ; ) {

                if (current != null) {
                    T value = current.item.get();
                    if (value == null) {
                        // current is deleted
                        successor = current.nextNotMarker();
                        if (!predecessor.casNext(current, successor)) {
                            break; // restart
                        }
                        current = successor;
                        continue;
                    }
                    int c = comparator.compare(value, item);
                    if (c == 0) {
                        if (value.equals(item) || current.casItem(value, item)) {
                            return value;
                        }
                        break; // restart
                    }
                    if (c < 0) {
                        // current.item < item
                        predecessor = current;
                        // current may be deleted at this point
                        current = current.nextNotMarker();
                        continue;
                    }
                }
                // 1. no more node
                // 2. current.item > item
                // insert node
                Node<T> newNode = Node.ofNormal(item, current);
                if (predecessor.casNext(current, newNode)) {
                    // ok
                    buildIndices(randomLevel(), newNode);
                    return null;
                }
                break; // restart
            }
        }
    }

    public T add2(T item) {
        Preconditions.checkNotNull(item);

        while (true) {
            for (Node<T> predecessor = findNode(item, true), current = predecessor.next.get(); ; ) {
                if (current != null) {
                    if (current.marker) {
                        // predecessor is deleted
                        break; // restart
                    }

                    T value = current.item.get();
                    if (value == null) {
                        // current is deleted
                        helpDelete(predecessor, current);
                        break;
                    }
                    int c = comparator.compare(value, item);
                    if (c == 0) {
                        if (value.equals(item) || current.casItem(value, item)) {
                            return value;
                        }
                        break; // restart
                    }
                    if (c < 0) {
                        // current.item < item
                        predecessor = current;
                        current = current.next.get();
                        continue;
                    }
                }
                // 1. no more node
                // 2. current.item > item
                // insert node
                Node<T> newNode = Node.ofNormal(item, current);
                if (predecessor.casNext(current, newNode)) {
                    // ok
                    buildIndices(randomLevel(), newNode);
                    return null;
                }
                break; // restart
            }
        }
    }

    private void buildIndices(int level, Node<T> node) {
        if (level < 1) {
            // no index
            return;
        }

        HeadIndex<T> head = headIndex.get();
        int newLevel;
        Index<T>[] indices;
        if (level > head.level) {
            newLevel = level;
            indices = makeIndices(newLevel, node);
        } else {
            newLevel = head.level + 1;
            indices = makeIndices(newLevel, node);
            head = increaseLevel(newLevel, head, node, indices);
        }
        insertIndices(head, newLevel, indices);
    }

    private void insertIndices(HeadIndex<T> head, int level, Index<T>[] indices) {
        final T item = indices[0].node.item.get();

        Index<T> predecessor;
        Index<T> current;
        Index<T> successor;

        restart:
        while (true) {
            predecessor = head;
            current = predecessor.right.get();

            int currentLevel = head.level;
            while (currentLevel > 0) {
                if (current != null) {
                    T value = current.node.item.get();
                    if (value == null) {
                        // current is deleted
                        successor = current.right.get();
                        if (!predecessor.casRight(current, successor)) {
                            continue restart;
                        }
                        current = successor;
                        continue;
                    }
                    int c = comparator.compare(value, item);
                    if (c == 0) { // impossible
                        throw new IllegalStateException("encounter index with same item when insert index");
                    }
                    if (c < 0) {
                        // go right
                        predecessor = current;
                        current = current.right.get();
                        continue;
                    }
                }
                // 1. current == null
                // 2. current.item > item
                // insert index
                if (currentLevel <= level) {
                    // insert index
                    indices[currentLevel - 1].lazySetRight(current);
                    if (!predecessor.casRight(current, indices[currentLevel - 1])) {
                        continue restart;
                    }
                    // node maybe deleted at this point
                }
                // 1. index inserted
                // 2. current level > level
                // go down
                predecessor = predecessor.down;
                current = predecessor.right.get();
                currentLevel--;
            }

            // indices inserted
            return;
        }
    }

    private HeadIndex<T> increaseLevel(int expectLevel, HeadIndex<T> head, Node<T> node, Index<T>[] indices) {
        HeadIndex<T> oldHead = head;
        HeadIndex<T> newHead;

        while (oldHead.level < expectLevel) {
            // build head indices at once
            newHead = oldHead;
            for (int level = oldHead.level + 1; level <= expectLevel; level++) {
                newHead = new HeadIndex<>(node, indices[level - 1], newHead, level);
            }
            // newHead = new HeadIndex<>(node, indices[oldHead.level], oldHead, oldHead.level + 1);
            if (headIndex.compareAndSet(oldHead, newHead)) {
                return newHead;
            }
            oldHead = headIndex.get();
        }
        return oldHead;
    }

    @SuppressWarnings("unchecked")
    private Index<T>[] makeIndices(int level, Node<T> node) {
        assert level > 0;
        Index<T>[] indices = (Index<T>[]) new Index[level];
        Index<T> lastIndex = null;
        for (int i = 0; i < level; i++) {
            indices[i] = new Index<>(node, null, lastIndex);
            lastIndex = indices[i];
        }
        return indices;
    }

    private int randomLevel() {
        int r = (int) System.nanoTime();
        // xor shift
        r ^= r << 13;
        r ^= r >>> 17;
        r ^= r << 5;
        if ((r & 0x80000001) != 0) {
            return 0;
        }
        int level = 1;
        while (((r >>>= 1) & 1) != 0) {
            level++;
        }
        return level;
    }

    public T remove(T item) {
        Preconditions.checkNotNull(item);

        while (true) {
            for (Node<T> predecessor = findNode(item, true),
                 current = predecessor.nextNotMarker(), successor; ; ) {
                if (current == null) {
                    // not found
                    return null;
                }
                T value = current.item.get();
                if (value == null) {
                    // current is deleted
                    successor = current.nextNotMarker();
                    if (!predecessor.casNext(current, successor)) {
                        break; // restart
                    }
                    current = successor;
                    continue;
                }
                int c = comparator.compare(value, item);
                if (c > 0) {
                    // not found
                    return null;
                }
                if (c < 0) {
                    // current.item < item
                    // go next
                    predecessor = current;
                    current = current.nextNotMarker();
                    continue;
                }
                // c == 0
                // found
                if (!current.casItem(value, null)) {
                    break; // restart
                }
                successor = current.next.get();
                if (successor == null || !successor.marker) {
                    Node<T> marker = Node.ofMarker(successor);
                    current.casNext(successor, marker);
                    // if failed, a marker node is inserted
                    predecessor.casNext(current, successor);
                } else {
                    predecessor.casNext(current, successor.next.get());
                }
                // unlink indices
                findNode(item, false);
                tryDecreaseLevel();
                return value;
            }
        }
    }

    public T remove2(T item) {
        Preconditions.checkNotNull(item);

        restart:
        while (true) {
            for (Node<T> predecessor = findNode(item, true),
                 current = predecessor.next.get(), successor; current != null; ) {
                if (current.marker) {
                    // predecessor is deleted
                    continue restart;
                }
                T value = current.item.get();
                if (value == null) {
                    // current is deleted
                    helpDelete(predecessor, current);
                    continue restart;
                }
                int c = comparator.compare(value, item);
                if (c > 0) {
                    // current.item > item
                    // not found
                    return null;
                }
                if (c < 0) {
                    // current.item < item
                    // go next
                    predecessor = current;
                    current = current.next.get();
                    continue;
                }
                // c == 0
                // found
                if (!current.casItem(value, null)) {
                    continue restart;
                }
                successor = current.next.get();
                if (successor == null || !successor.marker) {
                    Node<T> marker = Node.ofMarker(successor);
                    current.casNext(successor, marker);
                    // if failed, a marker node is inserted
                    predecessor.casNext(current, successor);
                } else {
                    predecessor.casNext(current, successor.next.get());
                }
                // unlink indices
                findNode(item, false);
                tryDecreaseLevel();
                return value;
            }
            // not found
            return null;
        }
    }

    private void tryDecreaseLevel() {
        HeadIndex<T> t1 = headIndex.get();
        if (t1.level <= 3) {
            return;
        }
        HeadIndex<T> t2 = (HeadIndex<T>) t1.down;
        HeadIndex<T> t3 = (HeadIndex<T>) t2.down;
        if (t3.right.get() != null || t2.right.get() != null || t1.right.get() != null) {
            return;
        }
        if (headIndex.compareAndSet(t1, t2)) {
            // rollback if right of t1 appeared
            if (t1.right.get() != null) {
                headIndex.compareAndSet(t2, t1);
            }
        }
    }

    private static class Index<T> {
        final Node<T> node;
        final AtomicReference<Index<T>> right;
        final Index<T> down;

        Index(Node<T> node, Index<T> right, Index<T> down) {
            this.node = node;
            this.right = new AtomicReference<>(right);
            this.down = down;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean casRight(Index<T> expect, Index<T> update) {
            return node.item.get() != null && right.compareAndSet(expect, update);
        }

        void lazySetRight(Index<T> right) {
            this.right.lazySet(right);
        }
    }

    private static final class HeadIndex<T> extends Index<T> {
        final int level;

        HeadIndex(Node<T> node, Index<T> right, Index<T> down, int level) {
            super(node, right, down);
            this.level = level;
        }
    }

    private static final class Node<T> {
        final boolean marker;
        final AtomicReference<T> item;
        final AtomicReference<Node<T>> next;

        Node(boolean marker, T item, Node<T> next) {
            this.marker = marker;
            this.item = new AtomicReference<>(item);
            this.next = new AtomicReference<>(next);
        }

        static <T> Node<T> ofMarker(Node<T> next) {
            return new Node<>(true, null, next);
        }

        static <T> Node<T> ofNormal(T item, Node<T> next) {
            return new Node<>(false, item, next);
        }

        Node<T> nextNotMarker() {
            Node<T> successor = next.get();
            if (successor == null) {
                return null;
            }
            if (successor.marker) {
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
