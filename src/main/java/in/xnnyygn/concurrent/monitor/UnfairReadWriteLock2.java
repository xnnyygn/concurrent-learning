package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class UnfairReadWriteLock2 implements ReadWriteLock {
    private static final int WRITE_MASK = 0x100;
    private static final int READER_MASK = 0xFF;

    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();
    private final AtomicInteger state = new AtomicInteger(0);

    public UnfairReadWriteLock2() {
    }

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
