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
public class FairLock1 implements Lock {

    private final Queue queue = new Queue();
    private final AtomicInteger reentrantTimes = new AtomicInteger(0);
    private Thread owner;

    public void lock() {
        if (owner == Thread.currentThread()) {
            // reentrant
            reentrantTimes.incrementAndGet();
            return;
        }
        // OPTIMIZATION POINT: if there's only one thread, node could be eliminated
        Node node = new Node(Thread.currentThread());
        Node predecessor = queue.enqueue(node);
        // PROBLEM: thread may be signaled here
        if (predecessor == queue.head.get() && reentrantTimes.get() == 0) {
            myTurn(node);
            return;
        }
        // signal only once
        LockSupport.park(this);
        // predecessor must be head and reentrant times is 0
        myTurn(node);
    }

    private void myTurn(@Nonnull Node node) {
        owner = Thread.currentThread();
        queue.head.set(node);
        reentrantTimes.set(1);
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

        // signal successor
        Node node = queue.head.get();
        Node successor = queue.findSuccessor(node);
        if (successor != null) {
            LockSupport.unpark(successor.thread);
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
        final AtomicReference<Node> head;
        final AtomicReference<Node> tail;

        Queue() {
            Node sentinel = new Node();
            head = new AtomicReference<>(sentinel);
            tail = new AtomicReference<>(sentinel);
        }

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
                node.predecessor.lazySet(t);
                if (tail.compareAndSet(t, node)) {
                    t.successor.set(node);
                    return t;
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
            while (c != node) {
                n = c;
                c = c.predecessor.get();
            }
            return n;
        }
    }

    private static class Node {
        final Thread thread;
        final AtomicReference<Node> predecessor = new AtomicReference<>();
        // optimization
        final AtomicReference<Node> successor = new AtomicReference<>();

        Node() {
            this(null);
        }

        Node(@Nullable Thread thread) {
            this.thread = thread;
        }
    }
}
