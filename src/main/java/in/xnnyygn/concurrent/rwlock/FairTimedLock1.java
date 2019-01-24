package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class FairTimedLock1 implements Lock {

    private final Queue queue = new Queue();
    private final AtomicInteger reentrantTimes = new AtomicInteger(0);
    private Thread owner;

    public void lock() {
        if (tryAcquire()) {
            return;
        }
        Node node = new Node(Thread.currentThread());
        queue.enqueue(node);
        while (true) {
            if (queue.isNextCandidate(node) && reentrantTimes.get() == 0) {
                myTurn(node);
                return;
            }
            if (isReadyToPark(node)) {
                LockSupport.park(this);
            }
        }
    }

    private boolean tryAcquire() {
        if (owner == Thread.currentThread()) {
            // reentrant
            reentrantTimes.incrementAndGet();
            return true;
        }
        return false;
    }

    public void lockInterruptibly() throws InterruptedException {
        if (tryAcquire()) {
            return;
        }
        Node node = new Node(Thread.currentThread());
        queue.enqueue(node);
        while (true) {
            if (queue.isNextCandidate(node) && reentrantTimes.get() == 0) {
                myTurn(node);
                return;
            }
            if (isReadyToPark(node)) {
                LockSupport.park(this);
            }
            if (Thread.interrupted()) {
                abort(node);
                throw new InterruptedException();
            }
        }
    }

    public boolean tryLock() {
        return tryAcquire();
    }

    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        if (tryAcquire()) {
            return true;
        }
        final long deadline = unit.toNanos(time) + System.nanoTime();
        Node node = new Node(Thread.currentThread());
        queue.enqueue(node);
        long nanos;
        while (true) {
            if (queue.isNextCandidate(node) && reentrantTimes.get() == 0) {
                myTurn(node);
                return true;
            }
            nanos = deadline - System.nanoTime();
            // timeout
            if (nanos <= 0L) {
                abort(node);
                return false;
            }
            if (isReadyToPark(node)) {
                LockSupport.parkNanos(this, nanos);
            }
            if (Thread.interrupted()) {
                abort(node);
                throw new InterruptedException();
            }
        }
    }

    private boolean isReadyToPark(@Nonnull Node node) {
        Node p = node.predecessor.get();
        int s = p.status.get();
        if (s == Node.STATUS_SIGNAL_SUCCESSOR) {
            return true;
        }
        if (s == Node.STATUS_ABORTED) {
            p = queue.skipAbortedPredecessors(node);
            p.successor.set(node);
        } else if (s == Node.STATUS_NORMAL) {
            p.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL_SUCCESSOR);
        }
        return false;
    }

    private void abort(@Nonnull Node node) {
        node.clearThread();

        Node p = queue.skipAbortedPredecessors(node);
        Node ps = p.successor.get();

        // linearization point
        node.status.set(Node.STATUS_ABORTED);

        if (queue.tail.get() == node && queue.tail.compareAndSet(node, p)) {
            p.successor.compareAndSet(ps, null);
            return;
        }

        if (p != queue.head.get() && p.ensureSignalSuccessorStatus() && p.thread.get() != null) {
            Node s = node.successor.get();
            if (s != null && s.status.get() != Node.STATUS_ABORTED) {
                p.successor.compareAndSet(ps, s);
            }
        } else {
            signalNormalSuccessor(node);
        }
    }

    private void myTurn(@Nonnull Node node) {
        owner = Thread.currentThread();
        queue.head.set(node);
        node.clearThread();
        node.predecessor.set(null);
        reentrantTimes.set(1);
    }

    private void signalNormalSuccessor(@Nonnull Node node) {
        Node successor = queue.findNormalSuccessor(node);
        if (successor != null) {
            LockSupport.unpark(successor.thread.get());
        }
    }

    public void unlock() {
        if (owner != Thread.currentThread()) {
            throw new IllegalStateException("not the thread holding lock");
        }
        int rt = reentrantTimes.get();
        if (rt < 1) {
            throw new IllegalStateException("reentrant times < 1 when try to unlock");
        }
        if (rt > 1) {
            reentrantTimes.set(rt - 1);
            return;
        }
        // rt == 1
        owner = null;
        // linearization point
        reentrantTimes.set(0);

        Node node = queue.head.get();
        if (node != null && node.status.get() == Node.STATUS_SIGNAL_SUCCESSOR) {
            signalNormalSuccessor(node);
        }
    }

    @Override
    @Nonnull
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("Duplicates")
    private static class Queue {
        final AtomicReference<Node> head = new AtomicReference<>();
        final AtomicReference<Node> tail = new AtomicReference<>();

        /**
         * Enqueue node.
         *
         * @param node new node
         * @return predecessor
         */
        @Nonnull
        Node enqueue(@Nonnull Node node) {
            Node t;
            while (true) {
                t = tail.get();
                if (t == null) {
                    // lazy initialization
                    Node sentinel = new Node();
                    if (head.get() == null && head.compareAndSet(null, sentinel)) {
                        tail.set(sentinel);
                    }
                } else {
                    node.predecessor.lazySet(t);
                    // linearization point
                    if (tail.compareAndSet(t, node)) {
                        t.successor.set(node);
                        return t;
                    }
                }
            }
        }

        @Nullable
        Node findNormalSuccessor(@Nonnull Node node) {
            Node n = node.successor.get();
            if (n != null && n.status.get() != Node.STATUS_ABORTED) {
                return n;
            }

            // find normal node from tail
            Node c = tail.get();
            n = null;
            // tail maybe null during lazy initialization
            while (c != null && c != node) {
                if (c.status.get() != Node.STATUS_ABORTED) {
                    n = c;
                }
                c = c.predecessor.get();
            }
            return n;
        }

        boolean isNextCandidate(@Nonnull Node node) {
            return node.predecessor.get() == head.get();
        }

        @Nonnull
        Node skipAbortedPredecessors(@Nonnull Node node) {
            Node h = head.get();
            Node p = node.predecessor.get();
            while (p != h && p.status.get() != Node.STATUS_ABORTED) {
                p = p.predecessor.get();
                /*
                 * set predecessor every time to help successors of
                 * current node to find the normal predecessor more quickly
                 */
                node.predecessor.set(p);
            }
            return p;
        }
    }

    /**
     * Node.
     * <p>
     * Status change:
     * NORMAL -> ABORTED
     */
    private static class Node {
        static final int STATUS_NORMAL = 0;
        static final int STATUS_ABORTED = -1;
        static final int STATUS_SIGNAL_SUCCESSOR = 1;

        /**
         * thread will be null if
         * 1. abort
         * 2. enter mutual exclusion area
         */
        final AtomicReference<Thread> thread;
        final AtomicInteger status = new AtomicInteger(STATUS_NORMAL);
        final AtomicReference<Node> predecessor = new AtomicReference<>();
        // optimization
        final AtomicReference<Node> successor = new AtomicReference<>();

        Node() {
            this(null);
        }

        Node(@Nullable Thread thread) {
            this.thread = new AtomicReference<>(thread);
        }

        boolean ensureSignalSuccessorStatus() {
            int s = this.status.get();
            return s == STATUS_SIGNAL_SUCCESSOR ||
                    (s == STATUS_NORMAL && this.status.compareAndSet(s, STATUS_SIGNAL_SUCCESSOR));
        }

        void clearThread() {
            thread.set(null);
        }
    }
}
