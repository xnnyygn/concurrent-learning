package in.xnnyygn.concurrent.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Future2<T> {

    private static final int STATE_START = 0;
    private static final int STATE_COMPLETED = 10;

    private static final int NODE_STATE_START = 0;
    private static final int NODE_STATE_PARK = 1;
    private static final int NODE_STATE_WAKEUP = 2;
    private static final int NODE_STATE_ABORT = 3;

    private final AtomicInteger state = new AtomicInteger(STATE_START);
    private volatile T value = null;
    private final AtomicReference<Node> head = new AtomicReference<>(null);
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    public T get() {
        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        Node n = new Node(Thread.currentThread());
        Node p = tail.getAndSet(n);
        if (p == null) {
            head.set(n);
        } else {
            p.next = n;
        }
        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        if (n.state.compareAndSet(NODE_STATE_START, NODE_STATE_PARK)) {
            LockSupport.park(this);
        }
        return value;
    }

    public T get(long time, TimeUnit unit) throws TimeoutException {
        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        Node n = new Node(Thread.currentThread());
        Node p = tail.getAndSet(n);
        if (p == null) {
            head.set(n);
        } else {
            p.next = n;
        }
        if (state.get() == STATE_COMPLETED) {
            return value;
        }
        long deadline = System.nanoTime() + unit.toNanos(time);
        if (n.state.compareAndSet(NODE_STATE_START, NODE_STATE_PARK)) {
            LockSupport.parkUntil(this, deadline);
            if (state.get() != STATE_COMPLETED && n.state.compareAndSet(NODE_STATE_PARK, NODE_STATE_ABORT)) {
                throw new TimeoutException();
            }
        }
        return value;
    }

    public void set(T value) {
        this.value = value;
        state.set(STATE_COMPLETED);
        if (tail.get() == null) {
            return;
        }
        while (head.get() == null) {
            Thread.yield();
        }
        Node n = head.get();
        while (true) {
            wakeUp(n);
            if (n == tail.get()) {
                break;
            }
            while (n.next == null) {
                Thread.yield();
            }
            n = n.next;
        }
    }

    private void wakeUp(Node n) {
        int s = n.state.get();
        if (s == NODE_STATE_ABORT) {
            return;
        }
        if (s == NODE_STATE_START && n.state.compareAndSet(NODE_STATE_START, NODE_STATE_WAKEUP)) {
            return;
        }
        if (n.state.compareAndSet(NODE_STATE_PARK, NODE_STATE_WAKEUP)) {
            LockSupport.unpark(n.thread);
        }
    }

    private static class Node {
        final Thread thread;
        final AtomicInteger state = new AtomicInteger(NODE_STATE_START);
        volatile Node next;

        Node(Thread thread) {
            this.thread = thread;
        }
    }
}
