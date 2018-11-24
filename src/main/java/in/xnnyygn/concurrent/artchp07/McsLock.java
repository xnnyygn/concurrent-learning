package in.xnnyygn.concurrent.artchp07;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class McsLock implements Lock {

    private final ThreadLocal<Node> localNode = ThreadLocal.withInitial(Node::new);
    private final AtomicReference<Node> atomicTail = new AtomicReference<>();

    @Override
    public void lock() {
        Node node = localNode.get();
        Node predecessor = atomicTail.getAndSet(node);
        if (predecessor == null) {
            return;
        }
        node.waiting = true;
        predecessor.next = node;
        while (node.waiting) {
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
        Node node = localNode.get();
        if (node.next == null) {
            if (atomicTail.compareAndSet(node, null)) {
                return;
            }
            // waiting for setting in lock
            // predecessor.next = node;
            while (node.next == null) {
            }
        }
        node.next.waiting = false;
        node.next = null;
    }

    @Override
    @Nonnull
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private static class Node {
        volatile boolean waiting = false;
        volatile Node next = null;
    }
}
