package in.xnnyygn.concurrent.mylock;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class MyLock implements Lock {
    final LockQueue queue = new LockQueue();
    private final AtomicInteger reentrantTimes = new AtomicInteger(0);
    private Thread owner;

    @Override
    public void lock() {
        if (tryLock()) {
            return;
        }
        Node node = Node.createNormalForCurrent();
        queue.enqueue(node);
        while (true) {
            if (queue.isFirstCandidate(node) &&
                    reentrantTimes.get() == 0 &&
                    reentrantTimes.compareAndSet(0, 1)) {
                myTurn(node);
                return;
            }
            if (isReadyToPark(node)) {
                LockSupport.park(this);
            }
        }
    }

    private void myTurn(@Nonnull Node node) {
        owner = Thread.currentThread();
        node.thread.set(null);
        queue.head.set(node);

        Node predecessor = node.predecessor.get();
        node.predecessor.set(null);
        predecessor.successor.set(null);
    }

    private boolean isReadyToPark(@Nonnull Node node) {
        Node predecessor = node.predecessor.get();
        int s = predecessor.status.get();
        if (s == Node.STATUS_SIGNAL) {
            return true;
        }
        if (s == Node.STATUS_ABORTED) {
            Node p = queue.skipAbortedPredecessor(node);
            p.successor.set(node);
        } else {
            // must be NORMAL
            node.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL);
        }
        return false;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (tryLock()) {
            return;
        }
        Node node = Node.createNormalForCurrent();
        queue.enqueue(node);
        while (true) {
            if (queue.isFirstCandidate(node) &&
                    reentrantTimes.get() == 0 &&
                    reentrantTimes.compareAndSet(0, 1)) {
                myTurn(node);
                return;
            }
            if (isReadyToPark(node)) {
                LockSupport.park(this);
            }
            if (Thread.interrupted()) {
                abort(node);
                throw new InterruptedException();
            }
        }
    }

    boolean acquireUninterruptibly(@Nonnull Node node) {
        boolean interrupted = false;
        while (true) {
            if (queue.isFirstCandidate(node) &&
                    reentrantTimes.get() == 0 &&
                    reentrantTimes.compareAndSet(0, 1)) {
                myTurn(node);
                return interrupted;
            }
            if (isReadyToPark(node)) {
                LockSupport.park(this);
            }
            if (Thread.interrupted()) {
                interrupted = true;
            }
        }
    }

    @Override
    public boolean tryLock() {
        if (owner == Thread.currentThread()) {
            reentrantTimes.incrementAndGet();
            return true;
        }
        if (reentrantTimes.get() == 0 && reentrantTimes.compareAndSet(0, 1)) {
            owner = Thread.currentThread();
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        if (tryLock()) {
            return true;
        }
        long deadline = System.nanoTime() + unit.toNanos(time);
        long nanos;
        Node node = Node.createNormalForCurrent();
        queue.enqueue(node);
        while (true) {
            if (queue.isFirstCandidate(node) &&
                    reentrantTimes.get() == 0 &&
                    reentrantTimes.compareAndSet(0, 1)) {
                myTurn(node);
                return true;
            }
            nanos = deadline - System.nanoTime();
            if (nanos <= 0L) {
                abort(node);
                return false;
            }
            if (isReadyToPark(node)) {
                LockSupport.parkNanos(this, nanos);
            }
            if (Thread.interrupted()) {
                abort(node);
                throw new InterruptedException();
            }
        }
    }

    private void abort(@Nonnull Node node) {
        node.thread.set(null);

        Node pred = queue.skipAbortedPredecessor(node);
        Node predSucc = pred.successor.get();

        node.status.set(Node.STATUS_ABORTED);

        Node t = queue.tail.get();
        if (t == node && queue.tail.compareAndSet(node, pred)) {
            pred.successor.compareAndSet(predSucc, null);
            return;
        }

        if (pred == queue.head.get() && pred.ensureSignalStatus() && pred.thread.get() != null) {
            Node s = node.successor.get();
            if (s != null && !s.isAborted()) {
                pred.successor.compareAndSet(predSucc, s);
            }
        } else {
            node.resetSignalStatus();
            signalSuccessor(node);
        }
    }

    private void signalSuccessor(@Nonnull Node node) {
        Node s = queue.findNormalSuccessor(node);
        if (s != null) {
            LockSupport.unpark(s.thread.get());
        }
    }

    @Override
    public void unlock() {
        if (owner != Thread.currentThread()) {
            throw new IllegalStateException("attempt to unlock without holding lock");
        }
        int rt = reentrantTimes.get();
        if (rt < 1) {
            throw new IllegalStateException("reentrant times < 1");
        }
        if (rt > 1) {
            reentrantTimes.set(rt - 1);
            return;
        }
        // rt == 1
        owner = null;
        reentrantTimes.set(0);

        Node node = queue.head.get();
        if (node != null && node.resetSignalStatus()) {
            signalSuccessor(node);
        }
    }

    @Override
    @Nonnull
    public Condition newCondition() {
        return new ConditionImpl(this);
    }
}
