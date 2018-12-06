package in.xnnyygn.concurrent.artchp08;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("Duplicates")
public class SimpleReentrantLock implements Lock {

    private final Lock lock = new ReentrantLock();
    private final Condition condition;
    private Thread holder;
    private int nesting = 0;

    public SimpleReentrantLock() {
        condition = lock.newCondition();
    }

    @Override
    public void lock() {
        lock.lock();
        try {
            if (holder == Thread.currentThread()) {
                nesting++;
                return;
            }
            while (nesting != 0) {
                try {
                    condition.await();
                } catch (InterruptedException ignore) {
                }
            }
            holder = Thread.currentThread();
            nesting++;
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
            nesting--;
            if (nesting == 0) {
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
