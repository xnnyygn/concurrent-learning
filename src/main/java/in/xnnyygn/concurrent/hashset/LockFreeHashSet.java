package in.xnnyygn.concurrent.hashset;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeHashSet<T> {

    private static final int MASK = 0x00FFFFFF;
    private static final double LOAD_FACTOR_THRESHOLD = 2.0;
    private final AtomicInteger atomicCapacity;
    private final AtomicInteger atomicSize = new AtomicInteger(0);
    //    private volatile BucketList<T>[] bucketLists;
    private final DRoot<T> root = new DRoot<>();

//    @SuppressWarnings("unchecked")
//    public LockFreeHashSet(int capacity) {
//        if (capacity <= 0) {
//            throw new IllegalArgumentException("capacity <= 0");
//        }
//        bucketLists = (BucketList<T>[]) new BucketList[capacity];
//        bucketLists[0] = new BucketList<>();
//    }

    public LockFreeHashSet(int capacity) {
        atomicCapacity = new AtomicInteger(capacity);
    }

    public boolean add(T x) {
        int index = hashCode(x) % atomicCapacity.get();
        if (!root.locate(index).add(x)) {
            return false;
        }
        double size = atomicSize.incrementAndGet();
        int capacity = atomicCapacity.get();
        if (size / capacity > LOAD_FACTOR_THRESHOLD) {
            atomicCapacity.compareAndSet(capacity, capacity * 2);
        }
        return true;
    }

    public boolean remove(T x) {
        int index = hashCode(x) % atomicCapacity.get();
        if (root.locate(index).remove(x)) {
            atomicSize.decrementAndGet();
            return true;
        }
        return false;
    }

    public boolean contains(T x) {
        int index = hashCode(x) % atomicCapacity.get();
        return root.locate(index).contains(x);
    }

    public int size() {
        return atomicSize.get();
    }

    @SuppressWarnings("unchecked")
//    private void resize() {
//        int oldCapacity = capacity;
//        if (atomicResizing.compareAndSet(false, true)) {
//            return;
//        }
//        if (capacity != oldCapacity) {
//            return;
//        }
//        int newCapacity = oldCapacity * 2;
//        System.out.println("resize from " + oldCapacity + " to " + newCapacity);
//        capacity = newCapacity;
//        atomicResizing.set(false);
//    }

//    private BucketList<T> bucketList(int index) {
//        BucketList<T> bucketList = bucketLists[index];
//        if (bucketList == null) {
//            BucketList<T> parentBucketList = bucketList(parent(index));
//            bucketLists[index] = parentBucketList.subList(index);
//        }
//        return bucketLists[index];
//    }

//    private int parent(int index) {
//        return index - Integer.highestOneBit(index);
//    }

//    @Override
//    public String toString() {
//        return "LockFreeHashSet{" +
//                "size=" + atomicSize.get() +
//                ", bucketLists=" + Arrays.toString(bucketLists) +
//                '}';
//    }

    private static class DRoot<T> {
        private final DNode<T> left; // 0
        private DNode<T> right; // 1

        DRoot() {
            left = new DNode<>(0, new BucketList<>());
        }

        BucketList<T> locate(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index < 0");
            }
            DNode<T> node = (index & 1) == 0 ? left : right();
            return node.locate(index, 2);
        }

        private DNode<T> right() {
            if (right == null) {
                right = new DNode<>(1, left.list.subList(1));
            }
            return right;
        }
    }

    private static class DNode<T> {
        private final int index;
        private final BucketList<T> list;
        private DNode<T> left;
        private DNode<T> right;

        DNode(int index, BucketList<T> list) {
            this.index = index;
            this.list = list;
        }

        BucketList<T> locate(int index, int mask) {
            if (index == this.index) {
                return list;
            }
            if ((index & mask) == 0) {
                return left().locate(index, mask << 1);
            }
            return right(mask).locate(index, mask << 1);
        }

        private DNode<T> left() {
            if (left == null) {
                left = new DNode<>(index, list);
            }
            return left;
        }

        private DNode<T> right(int mask) {
            if (right == null) {
                int i = this.index + mask;
                right = new DNode<>(i, list.subList(i));
            }
            return right;
        }
    }

    private static class BucketList<T> {
        private final Node<T> head;

        BucketList() {
            head = new Node<>(0);
            head.next = new AtomicMarkableReference<>(new Node<>(Integer.MAX_VALUE), false);
        }

        BucketList(Node<T> head) {
            this.head = head;
        }

        boolean add(T x) {
            int key = ordinaryKey(x.hashCode());
            Window<T> window;
            Node<T> node;
            while (true) {
                window = find(head, key);
                if (window.current.key == key) {
                    return false;
                }
                node = new Node<>(key, x);
                node.next = new AtomicMarkableReference<>(window.current, false);
                if (window.predecessor.casNext(window.current, node, false, false)) {
                    return true;
                }
            }
        }

        boolean remove(T x) {
            int key = ordinaryKey(x.hashCode());
            Window<T> window;
            while (true) {
                window = find(head, key);
                if (window.current.key != key) {
                    return false;
                }
                if (window.predecessor.attemptMarkNext(window.current, true)) {
                    return true;
                }
            }
        }

        boolean contains(T x) {
            int key = ordinaryKey(x.hashCode());
            Window window = find(head, key);
            return window.current.key == key;
        }

        BucketList<T> subList(int index) {
            int key = sentinelKey(index);
            Window<T> window;
            Node<T> sentinel;
            while (true) {
                window = find(head, key);
                if (window.current.key == key) {
                    return new BucketList<>(window.current);
                }
                System.out.println("make sub list " + index);
                sentinel = new Node<>(key);
                sentinel.next = new AtomicMarkableReference<>(window.current, false);
                // (current, false) -> (sentinel, false)
                if (window.predecessor.casNext(window.current, sentinel, false, false)) {
                    return new BucketList<>(sentinel);
                }
            }
        }

        Window<T> find(Node<T> start, int key) {
            Node<T> predecessor = start;
            Node<T> current = predecessor.nextUnmarked();
            while (current.key < key) {
                predecessor = current;
                current = current.nextUnmarked();
            }
            return new Window<>(predecessor, current);
        }

        @Override
        public String toString() {
            return "BucketList{" +
                    "head=" + head +
                    '}';
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
        private T value;
        // next node reference and mark
        private AtomicMarkableReference<Node<T>> next;

        Node(int key) {
            this.key = key;
        }

        Node(int key, T value) {
            this.key = key;
            this.value = value;
        }

        boolean attemptMarkNext(Node<T> expectedNext, boolean expectedMark) {
            return next.attemptMark(expectedNext, expectedMark);
        }

        boolean casNext(Node<T> expectedNext, Node<T> updatedNext, boolean expectedMarked, boolean updatedMarked) {
            return next.compareAndSet(expectedNext, updatedNext, expectedMarked, updatedMarked);
        }

        Node<T> nextUnmarked() {
            // node 0 -> node 1 -> node 2
            final Node<T> node0 = this;
            boolean[] node1MarkedHolder = new boolean[1];
            boolean[] node2MarkedHolder = new boolean[1];
            Node<T> node1 = next.get(node1MarkedHolder);
            Node<T> node2;
            while (node1MarkedHolder[0]) {
                node2 = node1.next.get(node2MarkedHolder);
                // (node1, true) -> (node2, node2 mark)
                node0.next.compareAndSet(node1, node2, true, node2MarkedHolder[0]);
                // re-read node 1
                node1 = node0.next.get(node1MarkedHolder);
            }
            return node1;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Node{key=");
            builder.append(key).append('(').append(Integer.toBinaryString(key)).append(')');
            builder.append(", type=");
            if ((key & ORDINARY_KEY_MASK) != 0) {
                builder.append("ORDINARY");
                builder.append(", value=").append(value);
            } else {
                builder.append("SENTINEL");
                builder.append(", index=").append(reverseBits(key));
            }
            if (next != null) {
                boolean[] markHolder = new boolean[1];
                Node<T> successor = next.get(markHolder);
                builder.append(", next={marked=").append(markHolder[0]);
                builder.append(", \n").append(successor).append("}");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    private static final int ORDINARY_KEY_MASK = 1;

    static int ordinaryKey(int hashCode) {
        return reverseBits(hashCode) | ORDINARY_KEY_MASK;
    }

    static int sentinelKey(int bucket) {
        return reverseBits(bucket);
    }

    static <T> int hashCode(T x) {
        return x.hashCode() & MASK;
    }

    private static int reverseBits(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }
        int low = 0x00000001;
        int high = 0x00800000;
        int r = 0;
        for (int i = 0; i < 24; i++) {
            if ((n & low) != 0) {
                r |= high;
            }
            low <<= 1;
            high >>>= 1;
        }
        return r;
    }
}
