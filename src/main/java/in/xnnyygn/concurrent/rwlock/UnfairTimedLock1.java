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
public class UnfairTimedLock1 implements Lock {

    private final Queue queue = new Queue();
    private final AtomicInteger reentrantTimes = new AtomicInteger(0);
    private Thread owner;

    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        if (owner == Thread.currentThread()) {
            // reentrant
            reentrantTimes.incrementAndGet();
            return true;
        }
        if (reentrantTimes.get() == 0 && reentrantTimes.compareAndSet(0, 1)) {
            owner = Thread.currentThread();
            return true;
        }
        final long deadline = unit.toNanos(time) + System.nanoTime();
        Node node = new Node(Thread.currentThread());
        Node predecessor = queue.enqueue(node);
        long nanos;
        while (true) {
            if (predecessor == queue.head.get() &&
                    reentrantTimes.get() == 0 && reentrantTimes.compareAndSet(0, 1)) {
                myTurn(predecessor, node);
                return true;
            }
            nanos = deadline - System.nanoTime();
            // timeout
            if (nanos <= 0L) {
                abort(predecessor, node);
                return false;
            }
            switch (predecessor.status.get()) {
                case Node.STATUS_ABORTED:
                    predecessor = queue.skipAbortedPredecessors(predecessor, node);
                    predecessor.successor.set(node);
                    break;
                case Node.STATUS_SIGNAL_SUCCESSOR:
                    LockSupport.parkNanos(this, nanos);
                    break;
                case Node.STATUS_NORMAL:
                    /*
                     * recheck is required after CAS
                     * 1. CAS failed
                     * 2. CAS successfully, but status changed to ABORTED before parking
                     * 3. predecessor unlock between first check and CAS(no unpark)
                     */
                    predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL_SUCCESSOR);
                    break;
            }
            if (Thread.interrupted()) {
                abort(predecessor, node);
                throw new InterruptedException();
            }
        }
    }

    private void abort(@Nonnull Node predecessor, @Nonnull Node node) {
        node.clearThread();

        Node p = queue.skipAbortedPredecessors(predecessor, node);
        Node ps = p.successor.get();

        // linearization point
        node.status.set(Node.STATUS_ABORTED);

        /*
         * at end
         *
         * A   -> |<B>
         * ANY -> |ABORTED
         *
         * no lost-wakeup problem
         */
        if (queue.tail.get() == node && queue.tail.compareAndSet(node, p)) {
            /*
             * failure is ok, which means
             * new node may enqueue between removing and setting successor
             */
            p.successor.compareAndSet(ps, null);
            return;
        }

        /*
         * at beginning
         *
         * A   -> |<B>       -> C
         * ANY -> |ABORTED   -> ANY
         *
         * lost-wakeup problem may happen
         *
         * scenarios
         * 1. B didn't set flag of A
         *
         * 2. B was signaled
         * sequence
         *    a. B set flag
         *    b. A signaled B
         *    c. B aborted
         */
        if (p == queue.head.get()) {
            signalNormalSuccessor(node);
            return;
        }

        /*
         * in middle
         *
         * A   -> |B   -> <C>     -> D
         * ANY -> |ANY -> ABORTED -> ANY
         *
         * lost-wakeup problem may happen
         *
         * conditions
         * 1. no one set flag of B
         * 1.1 D set flag of C before C aborts
         * 1.2 C didn't set the flag of B
         *
         * 2. B acquired lock and finished processing after p == head check
         *
         * first, try to set flag of B, then recheck if predecessor finished(unlocked or aborted)
         */
        if (p.ensureSignalSuccessorStatus() && p.thread.get() != null) {
            Node s = node.successor.get();
            if (s != null && s.status.get() != Node.STATUS_ABORTED) {
                p.successor.compareAndSet(ps, s);
            }
        } else {
            signalNormalSuccessor(node);
        }
    }

    private void myTurn(@Nonnull Node predecessor, @Nonnull Node node) {
        owner = Thread.currentThread();
        node.clearThread();
        queue.head.set(node);
        node.predecessor.set(null);
        predecessor.successor.set(null);
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
        if (node != null &&
                node.status.get() == Node.STATUS_SIGNAL_SUCCESSOR &&
                node.status.compareAndSet(Node.STATUS_SIGNAL_SUCCESSOR, Node.STATUS_NORMAL)) {
            signalNormalSuccessor(node);
        }
    }

    @Override
    public void lock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
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

        @Nonnull
        Node skipAbortedPredecessors(@Nonnull Node predecessor, @Nonnull Node node) {
            Node h = head.get();
            Node p = predecessor;
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
