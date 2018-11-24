package in.xnnyygn.concurrent;

import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentStack<T> {

    private final AtomicReference<Node<T>> top = new AtomicReference<>();

    public void push(T value) {
        Node<T> previous;
        Node<T> next;
        do {
            previous = top.get();
            next = new Node<>(value, previous);
        } while (!top.compareAndSet(previous, next));
    }

    public T pop() {
        Node<T> previous;
        Node<T> next;
        do {
            previous = top.get();
            if (previous == null) {
                return null;
            }
            next = previous.next;
        } while (!top.compareAndSet(previous, next));
        previous.next = null; // to help GC
        return previous.value;
    }

    private static class Node<T> {

        private final T value;
        private Node<T> next;

        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }

    }

}
