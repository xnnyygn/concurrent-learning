package in.xnnyygn.concurrent.rwlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;

@SuppressWarnings("Duplicates")
public class UnfairReadWriteTimedLock2 implements ReadWriteLock {
    // private static final int READER_MASK = 0x00FF;
    private static final int WRITER_MASK = 0xFF00;
    private static final int WRITER_UNIT = 0x0100;

    private final AtomicInteger count = new AtomicInteger(0);
    private final Queue queue = new Queue();
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
        @Nonnull
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("Duplicates")
    private class ReadLock extends AbstractLock {
        private final ThreadLocal<Integer> reentrantTimes = ThreadLocal.withInitial(() -> 0);

        @Override
        public void lock() {
            int rt = reentrantTimes.get();
            if (rt > 0) {
                reentrantTimes.set(rt + 1);
                return;
            }
            int c = count.get();
            if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                log("acquired read lock in unfair mode");
                reentrantTimes.set(1);
                return;
            }
            final Node node = new Node(Thread.currentThread(), true);
            Node predecessor = queue.enqueue(node);
            while (true) {
                if (predecessor == queue.head.get()) {
                    c = count.get();
                    if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                        myTurn(node);
                        return;
                    }
                }
                switch (predecessor.status.get()) {
                    case Node.STATUS_ABORTED:
                        predecessor = queue.skipAbortedPredecessor(node);
                        predecessor.successor.set(node);
                        break;
                    case Node.STATUS_SIGNAL:
                        LockSupport.park(this);
                        break;
                    case Node.STATUS_NORMAL:
                        predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
                        break;
                }
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            int rt = reentrantTimes.get();
            if (rt > 0) {
                reentrantTimes.set(rt + 1);
                return;
            }
            int c = count.get();
            if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                reentrantTimes.set(1);
                return;
            }
            final Node node = new Node(Thread.currentThread(), true);
            Node predecessor = queue.enqueue(node);
            while (true) {
                if (predecessor == queue.head.get()) {
                    c = count.get();
                    if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                        myTurn(node);
                        return;
                    }
                }
                switch (predecessor.status.get()) {
                    case Node.STATUS_ABORTED:
                        predecessor = queue.skipAbortedPredecessor(node);
                        predecessor.successor.set(node);
                        break;
                    case Node.STATUS_SIGNAL:
                        LockSupport.park(this);
                        break;
                    case Node.STATUS_NORMAL:
                        predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
                        break;
                }
                if (Thread.interrupted()) {
                    abort(node);
                    throw new InterruptedException();
                }
            }
        }

        @Override
        public boolean tryLock() {
            int rt = reentrantTimes.get();
            if (rt > 0) {
                reentrantTimes.set(rt + 1);
                return true;
            }
            int c = count.get();
            if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                reentrantTimes.set(1);
                return true;
            }
            return false;
        }

        @Override
        public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
            int rt = reentrantTimes.get();
            if (rt > 0) {
                reentrantTimes.set(rt + 1);
                return true;
            }
            int c = count.get();
            if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                reentrantTimes.set(1);
                return true;
            }
            final long deadline = System.nanoTime() + unit.toNanos(time);
            final Node node = new Node(Thread.currentThread(), true);
            Node predecessor = queue.enqueue(node);
            long nanos;
            while (true) {
                if (predecessor == queue.head.get()) {
                    c = count.get();
                    if ((c & WRITER_MASK) == 0 && count.compareAndSet(c, c + 1)) {
                        myTurn(node);
                        return true;
                    }
                }
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    abort(node);
                    return false;
                }
                switch (predecessor.status.get()) {
                    case Node.STATUS_ABORTED:
                        predecessor = queue.skipAbortedPredecessor(node);
                        predecessor.successor.set(node);
                        break;
                    case Node.STATUS_SIGNAL:
                        LockSupport.parkNanos(this, nanos);
                        break;
                    case Node.STATUS_NORMAL:
                        predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
                        break;
                }
                if (Thread.interrupted()) {
                    abort(node);
                    throw new InterruptedException();
                }
            }
        }

        private void myTurn(@Nonnull Node node) {
            log("acquired read lock");
            reentrantTimes.set(1);
            node.clearThread();
            queue.head.set(node);

            /*
             * propagate if successor is reader
             *
             * In ReadWriteLock, there's no need to check if propagate, it always propagates.
             */
            if (node.resetSignalStatus()) {
                Node successor = queue.findNormalSuccessor(node);
                if (successor != null && successor.shared) {
                    Thread t = successor.thread.get();
                    log("unpark " + t.getName());
                    LockSupport.unpark(t);
                }
            }
        }

        @Override
        public void unlock() {
            int rt = reentrantTimes.get();
            if (rt < 1) {
                throw new IllegalStateException("not the thread holding lock");
            }
            if (rt > 1) {
                reentrantTimes.set(rt - 1);
                return;
            }
            // rt == 1
            reentrantTimes.set(0);
            if (count.get() < 1) {
                throw new IllegalStateException("count < 1");
            }
            log("release read lock");
            if (count.decrementAndGet() > 0) {
                return;
            }
            Node h = queue.head.get();
            if (h != null && h.resetSignalStatus()) {
                unparkNormalSuccessor(h);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private class WriteLock extends AbstractLock {
        private Thread owner;

        @Override
        public void lock() {
            if (owner == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return;
            }
            if (count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                log("acquired write lock in unfair mode");
                owner = Thread.currentThread();
                return;
            }
            Node node = new Node(Thread.currentThread());
            Node predecessor = queue.enqueue(node);
            while (true) {
                if (predecessor == queue.head.get() &&
                        count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                    myTurn(node);
                    return;
                }
                switch (predecessor.status.get()) {
                    case Node.STATUS_ABORTED:
                        predecessor = queue.skipAbortedPredecessor(node);
                        predecessor.successor.set(node);
                        break;
                    case Node.STATUS_SIGNAL:
                        LockSupport.park(this);
                        break;
                    case Node.STATUS_NORMAL:
                        predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
                        break;
                }
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            if (owner == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return;
            }
            if (count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                owner = Thread.currentThread();
                return;
            }
            Node node = new Node(Thread.currentThread());
            Node predecessor = queue.enqueue(node);
            while (true) {
                if (predecessor == queue.head.get() &&
                        count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                    myTurn(node);
                    return;
                }
                switch (predecessor.status.get()) {
                    case Node.STATUS_ABORTED:
                        predecessor = queue.skipAbortedPredecessor(node);
                        predecessor.successor.set(node);
                        break;
                    case Node.STATUS_SIGNAL:
                        LockSupport.park(this);
                        break;
                    case Node.STATUS_NORMAL:
                        predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
                        break;
                }
                if (Thread.interrupted()) {
                    abort(node);
                    throw new InterruptedException();
                }
            }
        }

        @Override
        public boolean tryLock() {
            if (owner == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return true;
            }
            if (count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                owner = Thread.currentThread();
                return true;
            }
            return false;
        }

        @Override
        public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
            if (owner == Thread.currentThread()) {
                count.getAndAdd(WRITER_UNIT);
                return true;
            }
            if (count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                owner = Thread.currentThread();
                return true;
            }
            long deadline = System.nanoTime() + unit.toNanos(time);
            Node node = new Node(Thread.currentThread());
            Node predecessor = queue.enqueue(node);
            long nanos;
            while (true) {
                if (predecessor == queue.head.get() &&
                        count.get() == 0 && count.compareAndSet(0, WRITER_UNIT)) {
                    myTurn(node);
                    return true;
                }
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    abort(node);
                    return false;
                }
                switch (predecessor.status.get()) {
                    case Node.STATUS_ABORTED:
                        predecessor = queue.skipAbortedPredecessor(node);
                        predecessor.successor.set(node);
                        break;
                    case Node.STATUS_SIGNAL:
                        LockSupport.parkNanos(this, nanos);
                        break;
                    case Node.STATUS_NORMAL:
                        predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
                        break;
                }
                if (Thread.interrupted()) {
                    abort(node);
                    throw new InterruptedException();
                }
            }
        }

        private void myTurn(@Nonnull Node node) {
            log("acquired write lock");
            owner = Thread.currentThread();
            queue.head.set(node);
            node.clearThread();
        }

        @Override
        public void unlock() {
            if (owner != Thread.currentThread()) {
                throw new IllegalStateException("not the thread holding write lock");
            }
            int c = count.get();
            if (c < WRITER_UNIT) {
                throw new IllegalStateException("no writer");
            }
            if (c > WRITER_UNIT) {
                count.set(c - WRITER_UNIT);
                return;
            }
            // c == WRITER_UNIT
            owner = null;
            // linearization point
            log("release write lock");
            count.set(0);

            // signal successor
            Node node = queue.head.get();
            if (node != null && node.status.get() == Node.STATUS_SIGNAL) {
                node.resetSignalStatus();
                unparkNormalSuccessor(node);
            }
        }
    }

    private void abort(@Nonnull Node node) {
        node.clearThread();

        Node p = queue.skipAbortedPredecessor(node);
        Node ps = p.successor.get();

        node.status.set(Node.STATUS_ABORTED);

        Node t = queue.tail.get();
        if (t == node && queue.tail.compareAndSet(t, p)) {
            p.successor.compareAndSet(ps, null);
            return;
        }

        if (p != queue.head.get() && p.ensureSignalStatus() && p.thread.get() != null) {
            Node s = node.successor.get();
            if (s != null && s.status.get() != Node.STATUS_ABORTED) {
                p.successor.compareAndSet(ps, s);
            }
        } else {
            node.resetSignalStatus();
            unparkNormalSuccessor(node);
        }
    }

    private void unparkNormalSuccessor(@Nonnull Node node) {
        Node successor = queue.findNormalSuccessor(node);
        if (successor != null) {
            Thread t = successor.thread.get();
            log("unpark " + t.getName());
            LockSupport.unpark(t);
        }
    }

    private void log(String msg) {
        System.out.println(Thread.currentThread().getName() + " " + System.currentTimeMillis() + ":" + msg);
    }

    @SuppressWarnings("Duplicates")
    private static class Queue {
        final AtomicReference<Node> head = new AtomicReference<>();
        final AtomicReference<Node> tail = new AtomicReference<>();

        @Nonnull
        Node enqueue(@Nonnull Node node) {
            Node t;
            while (true) {
                t = tail.get();
                if (t == null) {
                    Node sentinel = new Node();
                    if (head.compareAndSet(null, sentinel)) {
                        tail.set(sentinel);
                    }
                } else {
                    node.predecessor.lazySet(t);
                    if (tail.compareAndSet(t, node)) {
                        t.successor.set(node);
                        return t;
                    }
                }
            }
        }

        @Nullable
        Node findNormalSuccessor(@Nonnull Node node) {
            Node s = node.successor.get();
            if (s != null && s.status.get() != Node.STATUS_ABORTED) {
                return s;
            }

            // find from tail
            s = null;
            Node c = tail.get();
            while (c != null && c != node) {
                if (c.status.get() != Node.STATUS_ABORTED) {
                    s = c;
                }
                c = c.predecessor.get();
            }
            return s;
        }

        @Nonnull
        Node skipAbortedPredecessor(@Nonnull Node node) {
            Node h = head.get();
            Node p = node.predecessor.get();
            while (p != h && p.status.get() == Node.STATUS_ABORTED) {
                p = p.predecessor.get();
                node.predecessor.set(p);
            }
            return p;
        }
    }

    private static class Node {
        static final int STATUS_NORMAL = 0;
        static final int STATUS_SIGNAL = 1;
        static final int STATUS_ABORTED = -1;

        final AtomicReference<Thread> thread;
        final boolean shared;
        final AtomicInteger status = new AtomicInteger(STATUS_NORMAL);
        final AtomicReference<Node> predecessor = new AtomicReference<>();
        // optimization
        final AtomicReference<Node> successor = new AtomicReference<>();

        Node() {
            this(null, false);
        }

        Node(@Nullable Thread thread) {
            this(thread, false);
        }

        Node(@Nullable Thread thread, boolean shared) {
            this.thread = new AtomicReference<>(thread);
            this.shared = shared;
        }

        void clearThread() {
            thread.set(null);
        }

        /**
         * Ensure signal status.
         * If current status is signal, just return.
         * If current status is normal, then try to CAS status from normal to signal.
         *
         * @return true if changed to signal, otherwise false
         */
        boolean ensureSignalStatus() {
            int s = status.get();
            return s == STATUS_SIGNAL || (s == STATUS_NORMAL && status.compareAndSet(STATUS_NORMAL, STATUS_SIGNAL));
        }

        /**
         * Reset signal status.
         * SIGNAL -> NORMAL
         *
         * @return true if successful, otherwise false
         */
        boolean resetSignalStatus() {
            return status.get() == STATUS_SIGNAL && status.compareAndSet(STATUS_SIGNAL, STATUS_NORMAL);
        }
    }
}
