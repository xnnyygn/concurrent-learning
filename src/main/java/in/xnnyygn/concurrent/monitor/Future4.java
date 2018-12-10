package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Future4<T> {

    private static final int STATE_NEW = 0;
    private static final int STATE_COMPLETED = 1;

    private final AtomicInteger state = new AtomicInteger(STATE_NEW);
    private volatile T value;
    private final AtomicReference<Node> top = new AtomicReference<>(null);


    public T get() throws InterruptedException {
        Node n = null;
        boolean queued = false;
        Node t;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (state.get() == STATE_COMPLETED) {
                return value;
            }
            if (n == null) {
                n = new Node(Thread.currentThread());
            } else if (!queued) {
                t = top.get();
                n.next = t;
                queued = top.compareAndSet(t, n);
            } else {
                LockSupport.park(this);
            }
        }
    }


    public T get(long time, @Nonnull TimeUnit unit) throws InterruptedException, TimeoutException {
        final long deadline = System.nanoTime() + unit.toNanos(time);
        Node n = null;
        boolean queued = false;
        Node t;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (state.get() == STATE_COMPLETED) {
                return value;
            }
            if (n == null) {
                n = new Node(Thread.currentThread());
            } else if (!queued) {
                t = top.get();
                n.next = t;
                queued = top.compareAndSet(t, n);
            } else if (System.nanoTime() > deadline) {
                throw new TimeoutException();
            } else {
                LockSupport.parkUntil(this, deadline);
            }
        }
    }

    public void set(T value) {
        if (state.compareAndSet(STATE_NEW, STATE_COMPLETED)) {
            throw new IllegalStateException("expected state new, but was " + state.get());
        }
        this.value = value;

        Node n = top.get();
        Node s;
        while (n != null) {
            LockSupport.unpark(n.thread);

            // clear next
            s = n.next;
            n.next = null;
            n = s;
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
