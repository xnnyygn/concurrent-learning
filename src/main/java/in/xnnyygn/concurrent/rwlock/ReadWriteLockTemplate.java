package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class ReadWriteLockTemplate implements ReadWriteLock {

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

    private class ReadLock extends AbstractLock {
        @Override
        public void lock() {
        }

        @Override
        public void unlock() {
        }
    }

    private class WriteLock extends AbstractLock {
        @Override
        public void lock() {
        }

        @Override
        public void unlock() {
        }
    }
}
