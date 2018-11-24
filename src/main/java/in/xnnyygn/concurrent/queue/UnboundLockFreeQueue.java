package in.xnnyygn.concurrent.queue;

import java.util.concurrent.atomic.AtomicReference;

public class UnboundLockFreeQueue<T> {

    private final AtomicReference<Node<T>> atomicHead;
    private final AtomicReference<Node<T>> atomicTail;

    public UnboundLockFreeQueue() {
        Node<T> node = new Node<>();
        atomicHead = new AtomicReference<>(node);
        atomicTail = new AtomicReference<>(node);
    }

    public void enqueue(T item) {
        Node<T> node = new Node<>(item);
        Node<T> tail;
        Node<T> next;
        while (true) {
            tail = atomicTail.get();
            next = tail.next();
            if (tail != atomicTail.get()) {
                continue;
            }
            if (next == null && tail.casNext(node)) {
                atomicTail.compareAndSet(tail, node);
                return;
            }
            // helping
            atomicTail.compareAndSet(tail, tail.next());
        }
    }

    public T dequeue() {
        Node<T> head;
        Node<T> next;
        Node<T> tail;
        while (true) {
            head = atomicHead.get();
            next = head.next();
            tail = atomicTail.get();
            if (head != atomicHead.get()) {
                continue;
            }
            if (head == tail) {
                if (next == null) {
                    throw new IllegalStateException("queue is empty");
                }
                // helping
                atomicTail.compareAndSet(tail, next);
            } else {
                if (atomicHead.compareAndSet(head, next)) {
                    return next.value;
                }
            }
        }
    }

    private static class Node<T> {
        private T value;
        private final AtomicReference<Node<T>> atomicNext = new AtomicReference<>(null);

        Node() {
            this.value = null;
        }

        Node(T value) {
            this.value = value;
        }

        boolean casNext(Node<T> node) {
            return atomicNext.compareAndSet(null, node);
        }

        Node<T> next() {
            return atomicNext.get();
        }
    }
}
