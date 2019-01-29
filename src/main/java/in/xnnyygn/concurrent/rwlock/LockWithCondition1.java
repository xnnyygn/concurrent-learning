package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

public class LockWithCondition1 implements Lock {

    @Override
    public void lock() {

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    @Nonnull
    public Condition newCondition() {
        return new ConditionImpl();
    }

    @SuppressWarnings("Duplicates")
    private class ConditionImpl implements Condition {
        final LinkedList<WaitingThread> waitingThreads = new LinkedList<>();

        @Override
        public void await() throws InterruptedException {
            waitingThreads.addLast(new WaitingThread(Thread.currentThread()));
            unlock();
            LockSupport.park(this);
            lockInterruptibly();
        }

        @Override
        public void awaitUninterruptibly() {
            waitingThreads.addLast(new WaitingThread(Thread.currentThread()));
            unlock();
            LockSupport.park(this);
            lock();
        }

        @Override
        public long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (nanosTimeout <= 0L) {
                return 0L;
            }
            long startAt = System.nanoTime();
            waitingThreads.addLast(new WaitingThread(Thread.currentThread()));
            unlock();
            LockSupport.parkNanos(this, nanosTimeout);
            lock();
            return System.nanoTime() - startAt;
        }

        @Override
        public boolean await(long time, @Nonnull TimeUnit unit) throws InterruptedException {
            WaitingThread t = new WaitingThread(Thread.currentThread());
            waitingThreads.addLast(t);
            unlock();
            LockSupport.parkNanos(this, unit.toNanos(time));
            boolean signaled = (t.status.get() == WaitingThread.STATUS_SIGNALED || !t.abort());
            lock();
            return signaled;
        }

        @Override
        public boolean awaitUntil(@Nonnull Date deadline) throws InterruptedException {
            return await(deadline.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void signal() {
            WaitingThread t = waitingThreads.removeFirst();
            if (t != null) {
                t.wakeUp();
            }
        }

        @Override
        public void signalAll() {
            for (WaitingThread t : waitingThreads) {
                t.wakeUp();
            }
            waitingThreads.clear();
        }
    }

    private static class WaitingThread {
        static final int STATUS_NORMAL = 0;
        static final int STATUS_SIGNALED = 1;
        static final int STATUS_ABORTED = -1;

        final AtomicReference<Thread> thread;
        final AtomicInteger status = new AtomicInteger(STATUS_NORMAL);

        WaitingThread(@Nonnull Thread thread) {
            this.thread = new AtomicReference<>(thread);
        }

        void wakeUp() {
            if (status.compareAndSet(STATUS_NORMAL, STATUS_SIGNALED)) {
                LockSupport.unpark(thread.get());
            }
        }

        boolean abort() {
            thread.set(null);
            return status.compareAndSet(STATUS_NORMAL, STATUS_ABORTED);
        }
    }
}
