package in.xnnyygn.concurrent.stack;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {

    private final AtomicReference<Node<T>> atomicTop = new AtomicReference<>(null);

    public void push(T value) {
        Node<T> node = new Node<>(value);
        while (!tryPush(node)) {
        }
    }

    protected boolean tryPush(Node<T> node) {
        Node<T> top = atomicTop.get();
        node.next = top;
        return atomicTop.compareAndSet(top, node);
    }

    public T pop() {
        Node<T> top;
        while ((top = tryPop()) == null) {
        }
        return top.value;
    }

    protected Node<T> tryPop() {
        Node<T> top = atomicTop.get();
        if (top == null) {
            throw new IllegalStateException("empty stack");
        }
        Node<T> next = top.next;
        return atomicTop.compareAndSet(top, next) ? top : null;
    }

    protected static class Node<T> {
        final T value;
        private Node<T> next = null;

        Node(T value) {
            this.value = value;
        }
    }
}
