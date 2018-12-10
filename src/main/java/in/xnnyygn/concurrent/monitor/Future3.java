package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Future3<T> {

    private static final int STATE_NEW = 0;
    private static final int STATE_COMPLETED = 1;

    private final AtomicInteger state = new AtomicInteger(STATE_NEW);
    private volatile T value;
    private final AtomicReference<Node> top = new AtomicReference<>(null);


    public T get() {
        if (state.get() == STATE_COMPLETED) {
            return value;
        }

        Node n = new Node(Thread.currentThread());
        Node t;
        do {
            t = top.get();
            n.next = t;
        } while (!top.compareAndSet(t, n));

        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        LockSupport.park(this);
        return value;
    }

    public T get(long time, @Nonnull TimeUnit unit) throws TimeoutException {
        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        long deadline = System.nanoTime() + unit.toNanos(time);
        Node n = new Node(Thread.currentThread());
        Node t;
        do {
            t = top.get();
            n.next = t;
            if (System.nanoTime() > deadline) {
                throw new TimeoutException();
            }
        } while (!top.compareAndSet(t, n));
        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        LockSupport.parkUntil(this, deadline);
        if (System.nanoTime() > deadline) {
            throw new TimeoutException();
        }
        return value;
    }

    public void set(T value) {
        if (state.compareAndSet(STATE_NEW, STATE_COMPLETED)) {
            throw new IllegalStateException("expected state new, but was " + state.get());
        }
        this.value = value;
        Node n = top.get();
        while (n != null) {
            LockSupport.unpark(n.thread);
            n = n.next;
        }
    }

    private static class Node {
        final Thread thread;
        Node next = null;

        Node(Thread thread) {
            this.thread = thread;
        }
    }
}
