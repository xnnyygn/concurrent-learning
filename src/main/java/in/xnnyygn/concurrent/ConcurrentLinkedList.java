package in.xnnyygn.concurrent;

import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentLinkedList<T> {

    private final AtomicReference<Node<T>> atomicHead = new AtomicReference<>();
    private final AtomicReference<Node<T>> atomicTail = new AtomicReference<>();

    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        if (atomicTail.get() == null) {
            if (atomicTail.compareAndSet(null, node)) {
                if (atomicHead.compareAndSet(null, node)) {
                    return;
                }
                // ?
            }
            Node<T> tail = atomicTail.get();
            if(atomicTail.compareAndSet(tail, node)) {

            }
            atomicHead.get().atomicNext.compareAndSet(null, node);
        }
    }

    private static class Node<T> {

        private final T value;
        private final AtomicReference<Node<T>> atomicNext = new AtomicReference<>();

        Node(T value) {
            this.value = value;
        }

    }

}
