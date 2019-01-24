package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class SimpleReadWriteLock implements ReadWriteLock {
    private static final int WRITER_MASK = 0xFF00;
    private static final int WRITER_UNIT = 0x0100;

    // writer reentrant times, reader count
    private final AtomicInteger count = new AtomicInteger(0);
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
        public void lock() {
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
        // reentrant times of current reader
        private final ThreadLocal<Integer> reentrantTimes = ThreadLocal.withInitial(() -> 0);

        @Override
        public boolean tryLock() {
            int rt = reentrantTimes.get();
            if (rt > 0) {
                reentrantTimes.set(rt + 1);
                return true;
            }
            int c = count.get();
            if (((c & WRITER_MASK) == 0) && count.compareAndSet(c, c + 1)) {
                reentrantTimes.set(1);
                return true;
            }
            return false;
        }

        @Override
        public void unlock() {
            int rt = reentrantTimes.get();
            if (rt <= 0) {
                throw new IllegalStateException("attempt to unlock without holding lock");
            }
            if (rt > 1) {
                reentrantTimes.set(rt - 1);
                return;
            }
            reentrantTimes.set(0);
            if (count.get() < 1) {
                throw new IllegalStateException("no reentrantTimes");
            }
            count.decrementAndGet();
        }
    }

    @SuppressWarnings("Duplicates")
    private class WriteLock extends AbstractLock {
        private Thread writer;

        @Override
        public boolean tryLock() {
            if (writer == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return true;
            }
            if (count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                writer = Thread.currentThread();
                return true;
            }
            return false;
        }

        @Override
        public void unlock() {
            if (writer != Thread.currentThread()) {
                throw new IllegalStateException("attempt to unlock without holding lock");
            }
            int c = count.get();
            if (c < WRITER_UNIT) {
                throw new IllegalStateException("no writer");
            }
            if (c == WRITER_UNIT) {
                writer = null;
            }
            count.set(c - WRITER_UNIT);
        }
    }
}
