package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;

// node, park/unpark, reentrant
@SuppressWarnings("Duplicates")
public class FairReadWriteLock5 implements ReadWriteLock {

    private static final int WRITER_MASK = 0x100;
    private static final int READERS_MASK = 0xFF;

    private static final int STATE_START = 0;
    private static final int STATE_PARK = 1;
    private static final int STATE_WAKE_UP = 2;
    private static final int STATE_LOCK = 3;
    private static final int STATE_UNLOCKED = 10;

    private final Lock readerLock = new ReaderLock();
    private final Lock writerLock = new WriterLock();
    private final AtomicInteger state = new AtomicInteger(0);
    private final AtomicReference<Node> tail = new AtomicReference<>(null);
    private final ThreadLocal<Node> node = new ThreadLocal<>();

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

    private static class Node {

        final boolean writer;
        final Thread thread;
        volatile Node successor = null;
        final AtomicInteger state = new AtomicInteger(STATE_START);

        Node(boolean writer, Thread thread) {
            this.writer = writer;
            this.thread = thread;
        }

        void wakeUpSuccessor() {
            Node s = successor;
            if (s == null) {
                return;
            }
            int ss;
            while (true) {
                ss = s.state.get();
                if (ss == STATE_START && s.state.compareAndSet(STATE_START, STATE_WAKE_UP)) {
                    break;
                }
                if (ss == STATE_PARK) {
                    LockSupport.unpark(s.thread);
                    s.state.set(STATE_WAKE_UP);
                    break;
                }
            }
            if (!writer && !s.writer) {
                s.wakeUpSuccessor();
            }
        }
    }

    private Node enqueue(Node n) {
        Node p = tail.getAndSet(n);
        p.successor = n;
        node.set(n);
        return p;
    }

    private void tryPark(Node p, Node n) {
        if (p == null || p.state.get() == STATE_UNLOCKED) {
            return;
        }
        int s = n.state.get();
        if (s == STATE_START && n.state.compareAndSet(STATE_START, STATE_PARK)) {
            LockSupport.park(this);
        }
    }

    private void wakeUpSuccessor(Node n) {
        Node s = n.successor;
        if (s == null) {
            if (tail.compareAndSet(n, null)) {
                return;
            }
            while (n.successor == null) {
                Thread.yield();
            }
        }
        n.wakeUpSuccessor();
        n.successor = null; // help GC
    }

    private class ReaderLock implements Lock {

        @Override
        public void lock() {
            Node n = node.get();
            if (n != null && n.state.get() == STATE_LOCK) {
                return;
            }
            n = new Node(false, Thread.currentThread());
            Node p = enqueue(n);

            // if current state contains writer thread, wait
            // else increase reader count
            int s;
            while (true) {
                s = state.get();
                if ((s & WRITER_MASK) != 0) {
                    tryPark(p, n);
                } else if (state.compareAndSet(s, s + 1)) {
                    break;
                }
            }
            n.state.set(STATE_LOCK);
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
            if ((s & READERS_MASK) != 0) {
                return;
            }

            Node n = node.get();
            n.state.set(STATE_UNLOCKED);
            wakeUpSuccessor(n);
            node.set(null);
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
            Node n = node.get();
            if (n != null && n.state.get() == STATE_LOCK) {
                return;
            }
            n = new Node(true, Thread.currentThread());
            Node p = enqueue(n);

            int s;
            // wait for previous writer to unlock
            // and try to set writer flag
            while (true) {
                s = state.get();

                // wait until writer to be unset
                if ((s & WRITER_MASK) != 0) {
                    // wait for previous writer thread
                    tryPark(p, n);
                    continue;
                }

                // try to set writer
                if (state.compareAndSet(s, s | WRITER_MASK)) {
                    break;
                }
            }
            // wait for readers to be 0
            while (state.get() != WRITER_MASK) {
                // wait for last reader
                tryPark(p, n);
            }
            n.state.set(STATE_LOCK);
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
            Node n = node.get();
            n.state.set(STATE_UNLOCKED);
            wakeUpSuccessor(n);
            node.set(null);
        }

        @Override
        @Nonnull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
