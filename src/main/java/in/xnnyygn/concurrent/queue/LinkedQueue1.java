package in.xnnyygn.concurrent.queue;

import javax.annotation.Nonnull;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class LinkedQueue1<T> extends AbstractQueue<T> {

    private final AtomicReference<Node<T>> head;
    private final AtomicReference<Node<T>> tail;

    public LinkedQueue1() {
        Node<T> sentinel = new Node<>();
        head = new AtomicReference<>(sentinel);
        tail = new AtomicReference<>(sentinel);
    }

    @Override
    @Nonnull
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean offer(T value) {
        final Node<T> n = new Node<>(value);
        Node<T> t; // tail
        Node<T> s; // successor
        while (true) {
            t = tail.get();
            s = t.next.get();
            if (s != null) {
                tail.compareAndSet(t, s); // help
                System.out.println("help");
            } else if (t.next.compareAndSet(null, n)) {
                tail.compareAndSet(t, n);
                return true;
            }
        }
    }

    @Override
    public T poll() {
        Node<T> h; // head
        Node<T> s; // successor
        while (true) {
            h = head.get();
            s = h.next.get();
            if (s == null) {
                throw new IllegalStateException("queue is empty");
            }
            if (head.compareAndSet(h, s)) {
                return s.value;
            }
        }
    }

    @Override
    public T peek() {
        final Node<T> s = head.get().next.get();
        return s != null ? s.value : null;
    }

    private static class Node<T> {
        final T value;
        final AtomicReference<Node<T>> next = new AtomicReference<>(null);

        Node() {
            this(null);
        }

        Node(T value) {
            this.value = value;
        }
    }
}
