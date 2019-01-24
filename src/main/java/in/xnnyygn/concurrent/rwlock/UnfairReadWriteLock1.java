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

public class UnfairReadWriteLock1 implements ReadWriteLock {
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
            int c = count.get();
            if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                reentrantTimes.set(1);
                return;
            }
            Node node = new Node(Thread.currentThread(), true);
            Node predecessor = queue.enqueue(node);
            while (true) {
                if (predecessor == queue.head.get()) {
                    c = count.get();
                    if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                        myTurn(node);
                        return;
                    }
                }
                LockSupport.park(this);
            }
        }

        private void myTurn(@Nonnull Node node) {
            reentrantTimes.set(1);
            node.clearThread();
            queue.head.set(node);

            // propagate to successor(reader)
            Node successor = queue.findSuccessor(node);
            if (successor != null && successor.shared) {
                LockSupport.unpark(successor.thread.get());
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
            if (count.get() < 1) {
                throw new IllegalStateException("count < 1");
            }
            if (count.decrementAndGet() > 0) {
                return;
            }
            // last reader
            Node node = queue.head.get();
            if (node != null) {
                Node successor = queue.findSuccessor(node);
                if (successor != null) {
                    LockSupport.unpark(successor.thread.get());
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private class WriteLock extends AbstractLock {
        private Thread owner;

        @Override
        public void lock() {
            if (owner == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return;
            }
            if (count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                owner = Thread.currentThread();
                return;
            }
            Node node = new Node(Thread.currentThread());
            Node predecessor = queue.enqueue(node);
            while (true) {
                if (predecessor == queue.head.get() &&
                        count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                    myTurn(node);
                    return;
                }
                LockSupport.park(this);
            }
        }

        private void myTurn(@Nonnull Node node) {
            owner = Thread.currentThread();
            queue.head.set(node);
            node.clearThread();
        }

        @Override
        public void unlock() {
            if (owner != Thread.currentThread()) {
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
            owner = null;
            // linearization point
            count.set(0);

            // signal successor
            Node node = queue.head.get();
            if (node != null) {
                Node successor = queue.findSuccessor(node);
                if (successor != null) {
                    LockSupport.unpark(successor.thread.get());
                }
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private static class Queue {
        final AtomicReference<Node> head = new AtomicReference<>();
        final AtomicReference<Node> tail = new AtomicReference<>();

        @Nonnull
        Node enqueue(@Nonnull Node node) {
            Node t;
            while (true) {
                t = tail.get();
                if (t == null) {
                    Node sentinel = new Node();
                    if (head.compareAndSet(null, sentinel)) {
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
            Node s = node.successor.get();
            if (s != null) {
                return s;
            }

            // find from tail
            Node c = tail.get();
            while (c != null && c != node) {
                s = c;
                c = c.predecessor.get();
            }
            return s;
        }
    }

    private static class Node {
        final AtomicReference<Thread> thread;
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
            this.thread = new AtomicReference<>(thread);
            this.shared = shared;
        }

        void clearThread() {
            thread.set(null);
        }
    }
}
