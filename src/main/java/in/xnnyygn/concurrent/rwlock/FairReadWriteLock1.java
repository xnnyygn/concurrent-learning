package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;

public class FairReadWriteLock1 implements ReadWriteLock {
    // private static final int READER_MASK = 0x00FF;
    private static final int WRITER_MASK = 0xFF00;
    private static final int WRITER_UNIT = 0x0100;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Queue queue = new Queue();
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    @Override
    @Nonnull
    public Lock readLock() {
        return readLock;
    }

    @Override
    @Nonnull
    public Lock writeLock() {
        return writeLock;
    }

    private static abstract class AbstractLock implements Lock {
        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
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
    }

    @SuppressWarnings("Duplicates")
    private class ReadLock extends AbstractLock {
        private final ThreadLocal<Integer> reentrantTimes = ThreadLocal.withInitial(() -> 0);

        @Override
        public void lock() {
            int rt = reentrantTimes.get();
            if (rt > 0) {
                reentrantTimes.set(rt + 1);
                return;
            }
            Node node = new Node(Thread.currentThread(), true);
            Node predecessor = queue.enqueue(node);
            if ((count.get() & WRITER_MASK) == 0 && predecessor == queue.head.get()) {
                myTurn(node);
                return;
            }
            LockSupport.park(this);
            myTurn(node);
        }

        private void myTurn(@Nonnull Node node) {
            reentrantTimes.set(1);
            queue.head.set(node);
            count.incrementAndGet();

            // propagate to successor(reader)
            Node successor = queue.findSuccessor(node);
            if (successor != null && successor.shared) {
                LockSupport.unpark(successor.thread);
            }
        }

        @Override
        public void unlock() {
            int rt = reentrantTimes.get();
            if (rt < 1) {
                throw new IllegalStateException("not the thread holding lock");
            }
            if (rt > 1) {
                reentrantTimes.set(rt - 1);
                return;
            }
            // rt == 1
            reentrantTimes.set(0);
            int c = count.get();
            if (c < 1) {
                throw new IllegalStateException("count < 1");
            }
            if (c > 1) {
                count.set(c - 1);
                return;
            }
            // c == 1
            count.set(0);

            Node node = queue.head.get();
            Node successor = queue.findSuccessor(node);
            if (successor != null) {
                LockSupport.unpark(successor.thread);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private class WriteLock extends AbstractLock {
        private Thread writer;

        @Override
        public void lock() {
            if (writer == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return;
            }
            Node node = new Node(Thread.currentThread());
            Node predecessor = queue.enqueue(node);
            if (predecessor == queue.head.get() && count.get() == 0) {
                myTurn(node);
                return;
            }
            LockSupport.park(this);
            myTurn(node);
        }

        private void myTurn(@Nonnull Node node) {
            writer = Thread.currentThread();
            queue.head.set(node);
            count.set(WRITER_UNIT);
        }

        @Override
        public void unlock() {
            if (writer != Thread.currentThread()) {
                throw new IllegalStateException("not the thread holding write lock");
            }
            int c = count.get();
            if (c < WRITER_UNIT) {
                throw new IllegalStateException("no writer");
            }
            if (c > WRITER_UNIT) {
                count.set(c - WRITER_UNIT);
                return;
            }
            // c == WRITER_UNIT
            writer = null;
            // linearization point
            count.set(0);

            // signal successor
            Node node = queue.head.get();
            Node successor = queue.findSuccessor(node);
            if (successor != null) {
                LockSupport.unpark(successor.thread);
            }
        }
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
            Node s = node.successor.get();
            if (s != null) {
                return s;
            }

            // find from tail
            Node c = tail.get();
            while (c != node) {
                s = c;
                c = c.predecessor.get();
            }
            return s;
        }
    }

    private static class Node {
        final Thread thread;
        final boolean shared;
        final AtomicReference<Node> predecessor = new AtomicReference<>();
        // optimization
        final AtomicReference<Node> successor = new AtomicReference<>();

        Node() {
            this(null, false);
        }

        Node(@Nullable Thread thread) {
            this(thread, false);
        }

        Node(@Nullable Thread thread, boolean shared) {
            this.thread = thread;
            this.shared = shared;
        }
    }
}
