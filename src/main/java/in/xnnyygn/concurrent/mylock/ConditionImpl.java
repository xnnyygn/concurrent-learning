package in.xnnyygn.concurrent.mylock;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
class ConditionImpl implements Condition {
    private final MyLock lock;
    private final ConditionQueue conditionQueue = new ConditionQueue();

    ConditionImpl(MyLock lock) {
        this.lock = lock;
    }

    @Override
    public void await() throws InterruptedException {
        Node node = Node.createConditionForCurrent();
        conditionQueue.enqueue(node);
        lock.unlock();
        boolean interrupted = false;
        while (nodeNotLockEnqueued(node)) {
            LockSupport.park(this);
            /*
             * 1. interrupted, unknown
             * 2. predecessor is aborted, lock enqueued
             * 3. signaled by predecessor, lock enqueued
             */
            if (Thread.interrupted()) {
                interrupted = lockEnqueueByAwaitingThread(node);
                break;
            }
        }
        if (lock.acquireUninterruptibly(node) && !interrupted) {
            Thread.currentThread().interrupt();
        }
        if (interrupted) {
            conditionQueue.removeNonConditionNodes();
            throw new InterruptedException();
        }
    }

    @Override
    public void awaitUninterruptibly() {
        Node node = Node.createConditionForCurrent();
        conditionQueue.enqueue(node);
        lock.unlock();
        boolean interrupted = false;
        while (nodeNotLockEnqueued(node)) {
            LockSupport.park(this);
            if (Thread.interrupted()) {
                interrupted = true;
            }
        }
        if (lock.acquireUninterruptibly(node) || interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) {
            return 0L;
        }
        Node node = Node.createConditionForCurrent();
        conditionQueue.enqueue(node);
        lock.unlock();
        long deadline = System.nanoTime() + nanosTimeout;
        long nanos = nanosTimeout;
        boolean interrupted = false;
        while (nodeNotLockEnqueued(node)) {
            LockSupport.parkNanos(this, nanos);
            nanos = deadline - System.nanoTime();
            if (Thread.interrupted()) {
                interrupted = lockEnqueueByAwaitingThread(node);
                break;
            }
            if (nanos <= 0L) {
                lockEnqueueByAwaitingThread(node);
                break;
            }
        }
        if (lock.acquireUninterruptibly(node) && !interrupted) {
            Thread.currentThread().interrupt();
        }
        if (interrupted || nanos <= 0L) {
            conditionQueue.removeNonConditionNodes();
        }
        if (interrupted) {
            throw new InterruptedException();
        }
        return nanos;
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        return awaitNanos(unit.toNanos(time)) > 0;
    }

    @Override
    public boolean awaitUntil(@Nonnull Date deadline) throws InterruptedException {
        return awaitNanos(TimeUnit.MILLISECONDS.toNanos(deadline.getTime() - System.currentTimeMillis())) > 0;
    }

    @Override
    public void signal() {
        Node node;
        do {
            node = conditionQueue.dequeue();
            if (node == null) {
                return;
            }
        } while (!lockEnqueueBySignalingThread(node));
    }

    @Override
    public void signalAll() {
        Node node;
        while ((node = conditionQueue.dequeue()) != null) {
            lockEnqueueBySignalingThread(node);
        }
    }

    private boolean nodeNotLockEnqueued(@Nonnull Node node) {
        if (node.status.get() == Node.STATUS_CONDITION || node.predecessor.get() == null) {
            return true;
        }
        if (node.successor.get() != null) {
            return false;
        }
        // status == NORMAL
        return !lock.queue.contains(node);
    }

    private boolean lockEnqueueByAwaitingThread(@Nonnull Node node) {
        if (node.status.compareAndSet(Node.STATUS_CONDITION, Node.STATUS_NORMAL)) {
            lock.queue.enqueue(node);
            return true;
        }
        // enqueuing by signal thread
        while (!lock.queue.contains(node)) {
            Thread.yield();
        }
        return false;
    }

    private boolean lockEnqueueBySignalingThread(@Nonnull Node node) {
        if (!node.status.compareAndSet(Node.STATUS_CONDITION, Node.STATUS_NORMAL)) {
            return false;
        }
        Node predecessor = lock.queue.enqueue(node);
        if (predecessor.isAborted() || !predecessor.status.compareAndSet(Node.STATUS_NORMAL, Node.STATUS_SIGNAL)) {
            // predecessor is aborted
            LockSupport.unpark(node.thread.get());
        }
        return true;
    }
}
