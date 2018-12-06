package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FairReadWriteLock {
    private final Lock lock = new ReentrantLock();
    private final ReaderLock readerLock = new ReaderLock();
    private final WriterLock writerLock = new WriterLock();
    private final Condition condition;
    private boolean write = false;
    private int readers = 0;

    public FairReadWriteLock() {
        condition = lock.newCondition();
    }

    public Lock readLock() {
        return readerLock;
    }

    public Lock writerLock() {
        return writerLock;
    }

    private class ReaderLock implements Lock {

        // write: true, wait until write to be false
        // write: false, increase readers
        @Override
        public void lock() {
            lock.lock();
            try {
                try {
                    while (write) {
                        condition.await();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        // decrease readers
        // if readers == 0, notify other threads
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

    private class WriterLock implements Lock {

        // write: false, means current thread is the first writer thread, set write to true
        // readers > 0, reader thread(s) present, wait until readers to be 0

        // write: true, current thread is not the first writer thread, wait for write to be false and readers to be 0
        @Override
        public void lock() {
            lock.lock();
            try {
                while (write) {
                    condition.await();
                }
                write = true;
                while (readers > 0) {
                    condition.await();
                }
//                if (!write) {
//                    write = true;
//                    while (readers > 0) {
//                        condition.await();
//                    }
//                } else {
//                    while (write || readers > 0) {
//                        condition.await();
//                    }
//                    write = true;
//                }
            } catch (InterruptedException ignore) {
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
