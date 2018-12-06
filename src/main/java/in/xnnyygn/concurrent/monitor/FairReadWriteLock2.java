package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

// state only, spin lock
public class FairReadWriteLock2 implements ReadWriteLock {

    private static final int WRITER_MASK = 0x100;
    private static final int READERS_MASK = 0xFF;

    private final Lock readerLock = new ReaderLock();
    private final Lock writerLock = new WriterLock();
    private final AtomicInteger state = new AtomicInteger(0);

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
            // if current state contains writer thread, wait
            // else increase reader count
            int s;
            while (true) {
                s = state.get();
                if ((s & WRITER_MASK) != 0) {
                    // wait for previous writer thread
                    Thread.yield();
                } else if (state.compareAndSet(s, s + 1)) {
                    break;
                }
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
            // TODO check if locked
            int s = state.decrementAndGet();
            if((s & READERS_MASK) == 0) {
                // notify successor
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
            int s;
            // wait for previous writer to unlock
            // and try to set writer flag
            while (true) {
                s = state.get();

                // wait until writer to be unset
                if ((s & WRITER_MASK) != 0) {
                    // wait for previous writer thread
                    Thread.yield();
                    continue;
                }

                // try to set writer
                if (state.compareAndSet(s, s | WRITER_MASK)) {
                    break;
                }
            }
            // wait for readers to be 0
            while ((state.get()) != WRITER_MASK) {
                // wait for last reader
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
            // state must be WRITER_MASK
            // clear writer flag
            state.set(0);
            // notify successor
        }

        @Override
        @Nonnull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
