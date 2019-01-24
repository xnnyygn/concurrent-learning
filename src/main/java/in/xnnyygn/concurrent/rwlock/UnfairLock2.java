package in.xnnyygn.concurrent.rwlock;

import in.xnnyygn.concurrent.monitor.ThreadScheduler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class UnfairLock2 implements Lock {

    private final Queue queue = new Queue();
    private final AtomicInteger reentrantTimes = new AtomicInteger(0);
    private Thread owner;

    public void lock() {
        if (owner == Thread.currentThread()) {
            // reentrant
            reentrantTimes.incrementAndGet();
            return;
        }
        if (reentrantTimes.get() == 0 && reentrantTimes.compareAndSet(0, 1)) {
            owner = Thread.currentThread();
            /*
             * owner is visible to current thread
             * in theory, it's ok for other thread not to see the latest value
             */
            return;
        }
        Node node = new Node(Thread.currentThread());
        Node predecessor = queue.enqueue(node);
        while (true) {
            if (predecessor == queue.head.get() &&
                    reentrantTimes.get() == 0 && reentrantTimes.compareAndSet(0, 1)) {
                myTurn(node);
                return;
            }
            node.park(this);
        }
    }

    private void myTurn(@Nonnull Node node) {
        owner = Thread.currentThread();
        queue.head.set(node);
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

        /*
         * signal successor
         *
         * node maybe current node
         * head will not be signaled, so current node or sentinel is ok
         *
         * 1. first
         * (sentinel, current, successor) -> (current, successor)
         * 2. second
         * (predecessor, current, successor) -> (current, successor)
         * 3. sentinel only during lazy initialization
         * no successor, ok
         * 4. one thread -> two threads -> one thread
         * (no node)
         * -> (sentinel, thread 1, thread 2)
         * -> (thread 1, thread 2)
         * -> (thread 2)
         * thread 2's node will be the sentinel
         * no successor, ok
         */
        Node node = queue.head.get();
        if (node != null) {
            Node successor = queue.findSuccessor(node);
            if (successor != null) {
                successor.wakeUp();
            }
        }
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
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
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
        Node findSuccessor(@Nonnull Node node) {
            Node n = node.successor.get();
            if (n != null) {
                return n;
            }

            // find node from tail
            Node c = tail.get();
            // tail maybe null during lazy initialization
            while (c != null && c != node) {
                n = c;
                c = c.predecessor.get();
            }
            return n;
        }
    }

    private static class Node {
        final Thread thread;
        final ThreadScheduler threadScheduler = new ThreadScheduler();
        final AtomicReference<Node> predecessor = new AtomicReference<>();
        // optimization
        final AtomicReference<Node> successor = new AtomicReference<>();

        Node() {
            this(null);
        }

        Node(@Nullable Thread thread) {
            this.thread = thread;
        }

        void park(Object blocker) {
            threadScheduler.park(blocker);
        }

        void wakeUp() {
            threadScheduler.wakeUp(thread);
        }
    }
}
