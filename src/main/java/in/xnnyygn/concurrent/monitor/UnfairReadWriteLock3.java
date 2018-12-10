package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

@SuppressWarnings("ALL")
public class UnfairReadWriteLock3 implements ReadWriteLock {
    private static final int WRITE_MASK = 0x100;
    private static final int READER_MASK = 0xFF;

    private static final int STATE_START = 0;
    private static final int STATE_PARK = 1;
    private static final int STATE_WAKE_UP = 2;
    private static final int STATE_UNLOCK = 10;

    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();
    private final AtomicInteger state = new AtomicInteger(STATE_START);
    private final AtomicReference<Node> head = new AtomicReference<>(null);

    // node x, notify successors
    // start -> park <-> wake up -> lock -> unlock

    // when enqueue
    // append to the head

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

    private static class Node {
        final boolean writer;
        final Thread thread;
        final AtomicInteger state = new AtomicInteger(STATE_START);

        volatile Node successor;

        Node(boolean writer, Thread thread) {
            this.writer = writer;
            this.thread = thread;
        }
    }

    private class ReadLock implements Lock {

        @Override
        public void lock() {
            int s;
            while (true) {
                s = state.get();
                if ((s & WRITE_MASK) == 0 && state.compareAndSet(s, s + 1)) {
                    break;
                }
                Thread.yield();
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
        public void unlock() {
            int s;
            do {
                s = state.get();
            } while (!state.compareAndSet(s, s - 1));
        }

        @Override
        @Nonnull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    private class WriteLock implements Lock {

        @Override
        public void lock() {
            int s;
            while (true) {
                s = state.get();
                if (s == 0 && state.compareAndSet(0, WRITE_MASK)) {
                    break;
                }
                Thread.yield();
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
        public void unlock() {
            // s = WRITE_MASK
            state.set(0);
        }

        @Override
        @Nonnull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
