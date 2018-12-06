package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("Duplicates")
public class UnfairReadWriteLock implements ReadWriteLock {

    private final Lock lock = new ReentrantLock();
    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();
    private final Condition condition;
    private boolean write = false;
    private int readers = 0;

    public UnfairReadWriteLock() {
        condition = lock.newCondition();
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
            lock.lock();
            try {
                while (write) {
                    try {
                        condition.await();
                    } catch (InterruptedException ignore) {
                    }
                }
                readers++;
            } finally {
                lock.unlock();
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
            lock.lock();
            try {
                readers--;
                if (readers == 0) {
                    condition.signalAll();
                }
            } finally {
                lock.unlock();
            }
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
            lock.lock();
            try {
                while (write || readers > 0) {
                    try {
                        condition.await();
                    } catch (InterruptedException ignore) {
                    }
                }
                write = true;
            } finally {
                lock.unlock();
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
            lock.lock();
            try {
                write = false;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        @Nonnull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
