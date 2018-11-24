package in.xnnyygn.concurrent.artchp07;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ClhLock implements Lock {

    private final AtomicReference<Node> atomicTail = new AtomicReference<>(new Node());
    private final ThreadLocal<Node> localCurrent = ThreadLocal.withInitial(Node::new);
    private final ThreadLocal<Node> localPredecessor = new ThreadLocal<>();

    @Override
    public void lock() {
        Node current = localCurrent.get();
        current.locked = true;
        Node predecessor = atomicTail.getAndSet(current);
        localPredecessor.set(predecessor);
        while (predecessor.locked) {
            // spin
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
        Node current = localCurrent.get();
        current.locked = false;
        localCurrent.set(localPredecessor.get());
    }

    @Override
    @Nonnull
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private static class Node {
        volatile boolean locked = false;
    }
}
