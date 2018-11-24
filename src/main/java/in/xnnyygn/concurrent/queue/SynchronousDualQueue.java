package in.xnnyygn.concurrent.queue;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;

public class SynchronousDualQueue<T> {

    private final AtomicReference<Node<T>> atomicHead;
    private final AtomicReference<Node<T>> atomicTail;

    public SynchronousDualQueue() {
        Node<T> sentinel = new Node<>(null);
        atomicHead = new AtomicReference<>(sentinel);
        atomicTail = new AtomicReference<>(sentinel);
    }

//    public void enqueue(T item) {
//        if (item == null) {
//            throw new IllegalArgumentException("item required");
//        }
//        Node<T> head = atomicHead.get();
//        Node<T> current = head;
//        Node<T> itemNode = new Node<>(item);
//        while (true) {
//            // sentinel: type == item && item == null && next == null
//            if (current.type == NodeType.ITEM) {
//                if (current.item() == null) {
//                    if (current.next() == null) {
//                        // sentinel, append node
//                        if (current.casNext(itemNode)) {
//                            itemNode.waitUntilItemBecomeNull();
//                            atomicHead.compareAndSet(head, itemNode);
//                            return;
//                        }
//                        // if failed, next has been set, move forward
//                    } else {
//                        // next != null
//                        // not sentinel, but used, set head
//                        if (atomicHead.compareAndSet(head, current)) {
//                            head = current;
//                        }
//                    }
//                } else {
//                    // item != null
//                    // another enqueue thread
//                    // try to append node
//                    if (current.next() == null && current.casNext(itemNode)) {
//                        itemNode.waitUntilItemBecomeNull();
//                        atomicHead.compareAndSet(head, itemNode);
//                        return;
//                    }
//                    // current.next != null or failed to cas next, move forward
//                }
//            } else {
//                // reservation
//                if (current.item() == null) {
//                    if (current.casItem(null, item)) {
//                        current.waitUntilItemBecomeNull();
//                        return;
//                    }
//                    // failed to CAS item, set by another enqueue thread
//                    // try to append node
//                    if (current.next() == null && current.casNext(itemNode)) {
//                        itemNode.waitUntilItemBecomeNull();
//                        atomicHead.compareAndSet(head, itemNode);
//                        return;
//                    }
//                }
//            }
//            current = current.next();
//        }
//    }

    public void enqueue(T item) {
        // if head == tail, then current node is sentinel
        Node<T> head;
        Node<T> tail;
        Node<T> next;
        Node<T> node = new Node<>(item);
        while (true) {
            head = atomicHead.get();
            tail = atomicTail.get();
            if (head == tail || tail.type == NodeType.ITEM) {
                // sentinel, no other enqueue thread, or enqueue threads exist
                next = tail.next();
                if (next != null) {
                    atomicTail.compareAndSet(tail, next);
                } else {
                    if (tail.casNext(node)) {
                        atomicTail.compareAndSet(tail, node);
                        node.waitUntilItemBecomeNull();

                        // move forward
                        head = atomicHead.get();
                        if (head.next() == node) {
                            atomicHead.compareAndSet(head, node);
                        }
                        return;
                    }
                }
            } else {
                next = head.next();
                // head.next == null => sentinel
                // wait for dequeue thread
                if (next == null || head != atomicHead.get() || tail != atomicTail.get()) {
                    continue;
                }
                // head != tail, tail.type != ITEM, head.next != null
                // set item
                // 1. dequeue thread, item = null => success
                // 2. dequeue threads, fulfilled by other enqueue thread, false
                boolean success = next.casItem(null, item);
                // move forward
                atomicHead.compareAndSet(head, next);
                if (success) {
                    return;
                }
            }
        }
    }

    public T dequeue() {
        Node<T> head;
        Node<T> tail;
        Node<T> next;
        Node<T> node = new Node<>();
        T result;
        while (true) {
            head = atomicHead.get();
            tail = atomicTail.get();
            if (head == tail || tail.type == NodeType.RESERVATION) {
                // no dequeue thread or dequeue threads exist
                next = tail.next();
                if (next != null) {
                    atomicTail.compareAndSet(tail, next);
                } else {
                    if (tail.casNext(node)) {
                        atomicTail.compareAndSet(tail, node);
                        result = node.waitUntilItemBecomeNotNull();
                        head = atomicHead.get();
                        if (head.next() == node) {
                            atomicHead.compareAndSet(head, node);
                        }
                        return result;
                    }
                }
            } else {
                next = head.next();
                if (next == null || head != atomicHead.get() || tail != atomicTail.get()) {
                    continue;
                }
                result = next.item();
                boolean success = next.casItem(result, null);
                atomicHead.compareAndSet(head, next);
                if (success) {
                    return result;
                }
            }
        }
    }

//    public T dequeue() {
//        Node<T> head = atomicHead.get();
//        Node<T> current = head;
//        Node<T> node = new Node<>();
//        T result;
//        while (true) {
//            if (current.type == NodeType.RESERVATION) { // other dequeue thread exists
//                // append node
//                if (current.next() == null && current.casNext(node)) {
//                    return waitForItem(node, head);
//                }
//            } else {
//                // type == item
//                if ((result = current.item()) != null) { // not sentinel
//                    if (current.casItem(result, null)) {
//                        return result;
//                    }
//                }
//                if (current.next() == null && current.casNext(node)) {
//                    return waitForItem(node, head);
//                }
//            }
//            current = current.next();
//        }
//    }

//    private T waitForItem(Node<T> node, Node<T> head) {
//        T result = node.waitUntilItemBecomeNotNull();
//        atomicHead.compareAndSet(head, node);
//        return result;
//    }

    private enum NodeType {
        ITEM, RESERVATION;
    }

    private static class Node<T> {
        private final NodeType type;
        private final AtomicReference<T> atomicItem;
        private final AtomicReference<Node<T>> atomicNext = new AtomicReference<>(null);

        Node() {
            type = NodeType.RESERVATION;
            atomicItem = new AtomicReference<>(null);
        }

        Node(T item) {
            type = NodeType.ITEM;
            atomicItem = new AtomicReference<>(item);
        }

        boolean casItem(T expected, T updated) {
            return atomicItem.compareAndSet(expected, updated);
        }

        boolean casNext(Node<T> node) {
            return atomicNext.compareAndSet(null, node);
        }

        Node<T> next() {
            return atomicNext.get();
        }

        T item() {
            return atomicItem.get();
        }

        void waitUntilItemBecomeNull() {
            while (atomicItem.get() != null) {
            }
        }

        T waitUntilItemBecomeNotNull() {
            T item;
            while ((item = atomicItem.get()) == null) {
            }
            return item;
        }
    }
}
