package in.xnnyygn.concurrent.monitor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class CountDownLatch2 {

    private static final int STATE_START = 0;
    private static final int START_PARK = 1;
    private static final int START_WAKEUP = 2;

    private final AtomicInteger count;
    private final AtomicReference<Node> head = new AtomicReference<>(null);
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    public CountDownLatch2(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }
        this.count = new AtomicInteger(count);
    }

    // blocking
    public void await() {
        if (count.get() == 0) {
            return;
        }
        // append self to waiters
        Node n = new Node(Thread.currentThread());
        Node p = tail.getAndSet(n);
        if (p == null) { // first waiter
            head.set(n);
        } else {
            p.next = n;
        }
        // re-check
        if (count.get() == 0) {
            return;
        }
        if (n.state.compareAndSet(STATE_START, START_PARK)) {
            LockSupport.park(this);
        }
    }

    // count down
    // if value becomes zero, notify waiters
    public void countDown() {
        int c;
        do {
            c = count.get();
        } while (c > 0 && !count.compareAndSet(c, c - 1));
        // not the last
        if (c > 1) {
            return;
        }
        // no waiter
        if (tail.get() == null) {
            return;
        }
        // wait for first waiter to be set
        while (head.get() == null) {
            Thread.yield();
        }
        Node n = head.get();
        while (true) {
            wakeUp(n);
            // last waiter
            if (n == tail.get()) {
                break;
            }
            // wait for next to be set
            while (n.next == null) {
                Thread.yield();
            }
            n = n.next;
        }
    }

    private void wakeUp(Node n) {
        if (n.state.get() == STATE_START && n.state.compareAndSet(STATE_START, START_WAKEUP)) {
            return;
        }
        n.state.set(START_WAKEUP);
        LockSupport.unpark(n.thread);
    }

    private static class Node {
        final Thread thread;
        final AtomicInteger state = new AtomicInteger(STATE_START);
        volatile Node next = null;

        Node(Thread thread) {
            this.thread = thread;
        }
    }
}
