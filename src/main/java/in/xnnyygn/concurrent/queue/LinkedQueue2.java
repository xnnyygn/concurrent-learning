package in.xnnyygn.concurrent.queue;

import javax.annotation.Nonnull;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("Duplicates")
public class LinkedQueue2<T> extends AbstractQueue<T> {

    private final AtomicReference<Node<T>> head;
    private final AtomicReference<Node<T>> tail;

    public LinkedQueue2() {
        final Node<T> node = new Node<>(null);
        head = new AtomicReference<>(node);
        tail = new AtomicReference<>(node);
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
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        final Node<T> node = new Node<>(value);

        Node<T> t = tail.get();
        Node<T> t2;
        Node<T> n = t; // current node
        Node<T> s; // successor
        while (true) {
            s = n.next.get();
            if (s == null) { // last node
                if (n.next.compareAndSet(null, node)) {
                    if (n != t) {
                        tail.compareAndSet(t, n);
                    }
                    return true;
                }
                // re-read next and retry
                continue;
            }

            if (s == n) { // linked to self
                t2 = tail.get();
                if (t2 != t) { // tail changed
                    t = t2;
                    n = t2;
                } else {
                    n = head.get();
                }
            } else {
                if (n != t) {
                    t2 = tail.get();
                    if (t2 != t) { // tail changed
                        t = t2;
                        n = t2;
                        continue;
                    }
                }
                n = s;
            }
        }
    }

    @Override
    public T poll() {
        T value;

        restart:
        while (true) {
            for (Node<T> h = head.get(), n = h, s; ; ) {
                value = n.value.get();

                if (value != null && n.value.compareAndSet(value, null)) {
                    if (n != h) { // move every two nodes
                        s = n.next.get();
                        if (s == null) {
                            updateHead(h, n);
                        } else {
                            updateHead(h, s);
                        }
                    }
                    return value;
                }

                s = n.next.get();
                if (s == null) { // last node
                    updateHead(h, n);
                    return null;
                }
                if (s == n) { // linked to self
                    continue restart;
                }

                n = s;
            }
        }
    }

    @Override
    public T peek() {
        T value;

        restart:
        while (true) {
            for (Node<T> h = head.get(), n = h, s; ; ) {
                value = n.value.get();

                if (value != null) {
                    updateHead(h, n);
                    return value;
                }

                s = n.next.get();
                if (s == null) { // last node
                    updateHead(h, n);
                    return null;
                }

                if (s == n) { // linked to self
                    continue restart;
                }

                n = s;
            }
        }
    }

    private void updateHead(Node<T> head, Node<T> node) {
        if (head != node && this.head.compareAndSet(head, node)) {
            head.next.set(head);
        }
    }

    private static class Node<T> {
        final AtomicReference<T> value;
        final AtomicReference<Node<T>> next = new AtomicReference<>(null);

        Node(T v) {
            value = new AtomicReference<>(v);
        }
    }
}
