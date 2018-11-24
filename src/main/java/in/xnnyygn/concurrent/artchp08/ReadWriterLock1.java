package in.xnnyygn.concurrent.artchp08;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class ReadWriterLock1 implements ReadWriteLock {

    private final Lock lock = new ReentrantLock();
    private int readers = 0;
    private boolean write = false;
    private final ReaderLock readerLock = new ReaderLock();
    private final WriterLock writerLock = new WriterLock();
    private final Condition condition;

    public ReadWriterLock1() {
        condition = lock.newCondition();
    }

    @Override
    @Nonnull
    public Lock readLock() {
        return readerLock;
    }

    @Override
    @Nonnull
    public Lock writeLock() {
        return writerLock;
    }

    private class ReaderLock implements Lock {

        @Override
        public void lock() {
            lock.lock();
            try {
                try {
                    while (write) {
                        condition.await(); // wait for no writer
                    }
                } catch (InterruptedException ignore) {
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
            lock.lock();
            try {
                if (write) {
                    return false;
                }
                try {
                    while (write) {
                        condition.await(); // wait for no writer
                    }
                } catch (InterruptedException ignore) {
                }
                readers++;
                return true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unlock() {
            lock.lock();
            try {
                if (readers == 0) {
                    throw new IllegalStateException("readers is zero");
                }
                readers--;
                if (readers == 0) {
                    condition.signalAll(); // no reader
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

    private class WriterLock implements Lock {

        @Override
        public void lock() {
            lock.lock();
            try {
                try {
                    while (readers > 0) {
                        condition.await(); // wait for no reader
                    }
                } catch (InterruptedException ignore) {
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
                condition.signalAll(); // no writer
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
