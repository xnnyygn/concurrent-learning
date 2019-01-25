package in.xnnyygn.concurrent.rwlock;

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
public class UnfairLock1 implements Lock {

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
        // PROBLEM: thread may be signaled here
        while (true) {
            if (predecessor == queue.head.get() &&
                    reentrantTimes.get() == 0 && reentrantTimes.compareAndSet(0, 1)) {
                myTurn(predecessor, node);
                return;
            }
            if (predecessor.signalSuccessor.get()) {
                LockSupport.park(this);
            } else {
                predecessor.signalSuccessor.set(true);
            }
        }
    }

    private void myTurn(@Nonnull Node predecessor, @Nonnull Node node) {
        owner = Thread.currentThread();
        node.clearThread();
        queue.head.set(node);
        node.predecessor.set(null);
        predecessor.successor.set(null);
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
        reentrantTimes.set(0);

        Node node = queue.head.get();
        if (node != null && node.signalSuccessor.get() &&
                node.signalSuccessor.compareAndSet(true, false)) {
            Node successor = queue.findSuccessor(node);
            if (successor != null) {
                LockSupport.unpark(successor.thread.get());
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
        final AtomicReference<Thread> thread;
        final AtomicBoolean signalSuccessor = new AtomicBoolean(false);
        final AtomicReference<Node> predecessor = new AtomicReference<>();
        // optimization
        final AtomicReference<Node> successor = new AtomicReference<>();

        Node() {
            this(null);
        }

        Node(@Nullable Thread thread) {
            this.thread = new AtomicReference<>(thread);
        }

        void clearThread() {
            thread.set(null);
        }
    }
}
