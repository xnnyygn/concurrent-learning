package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Future6<T> {

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
                removeNode(n);
                throw new InterruptedException();
            }
            if (state.get() == STATE_COMPLETED) {
                return value;
            }
            if (n == null) {
                n = new Node();
            } else if (!queued) {
                t = top.get();
                n.next = t;
                queued = top.compareAndSet(t, n);
            } else {
                n.scheduler.park(this);
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
                removeNode(n);
                throw new InterruptedException();
            }
            if (state.get() == STATE_COMPLETED) {
                return value;
            }
            if (n == null) {
                n = new Node();
            } else if (!queued) {
                t = top.get();
                n.next = t;
                queued = top.compareAndSet(t, n);
            } else if (System.nanoTime() > deadline) {
                removeNode(n);
                throw new TimeoutException();
            } else {
                n.scheduler.parkUntil(this, deadline);
            }
        }
    }

    private void removeNode(Node n) {
        if (n == null) {
            return;
        }
        n.thread = null;
        unlinkRemovedNodes();
    }

    private void unlinkRemovedNodes() {
        Node p; // predecessor
        Node m; // current node
        Node s; // successor

        restart:
        while (true) {
            for (p = null, m = top.get(); m != null; m = s) {
                s = m.next;
                if (m.thread != null) {
                    p = m;
                } else if (p != null) {
                    // m.thread == null, m was removed
                    if (p.next != s) {
                        p.next = s; // skip 1 node every time
                    }
                    if (p.thread == null) { // predecessor was removed
                        continue restart;
                    }
                } else if (!top.compareAndSet(m, s)) {
                    // m.thread == null && p == null, m is first node and removed
                    // if failed, top node was changed during traversal
                    continue restart;
                }
            }
            break;
        }
    }

    public void set(T value) {
        if (state.compareAndSet(STATE_NEW, STATE_COMPLETED)) {
            throw new IllegalStateException("expected state new, but was " + state.get());
        }
        this.value = value;

        Node n = top.getAndSet(null);
        Node s;
        Thread t;
        while (n != null) {
            t = n.thread;
            if (t != null) {
                n.scheduler.wakeUp(t);
                n.thread = null;
            }
            s = n.next;
            n.next = null;
            n = s;
        }
    }

    private static class Node {
        volatile Thread thread;
        volatile Node next = null;
        final ThreadScheduler scheduler = new ThreadScheduler();

        Node() {
            thread = Thread.currentThread();
        }
    }
}
