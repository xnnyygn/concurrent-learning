package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@SuppressWarnings("Duplicates")
public class SimpleLock implements Lock {
    // reentrant count
    private final AtomicInteger count = new AtomicInteger(0);
    private Thread owner;

    public boolean tryLock() {
        if (owner == Thread.currentThread()) {
            count.incrementAndGet();
            return true;
        }
        if (count.get() == 0 && count.compareAndSet(0, 1)) {
            owner = Thread.currentThread();
            return true;
        }
        return false;
    }

    public void unlock() {
        if (owner != Thread.currentThread()) {
            throw new IllegalMonitorStateException("attempt to unlock without holding lock");
        }
        int c = count.get();
        if (c < 1) {
            throw new IllegalStateException("count < 1 when unlock");
        }
        if (c == 1) {
            owner = null;
        }
        count.set(c - 1);
    }

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
