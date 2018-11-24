package in.xnnyygn.concurrent.artchp07;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class CompositeLock3 extends CompositeLock2 {
    private static final int FAST_PATH_MASK = 1 << 30;

    public CompositeLock3(int capacity) {
        super(capacity);
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        if (tryFastPathLock()) {
            System.out.println("fast path lock");
            return true;
        }
        if (super.tryLock(time, unit)) {
            // other threads
            // stamp with fast path mark
            waitForFastPath();
            return true;
        }
        return false;
    }

    private boolean tryFastPathLock() {
        int[] stampHolder = new int[1];
        Node node = atomicTail.get(stampHolder);
        int stamp = stampHolder[0];
        if (node != null || (stamp & FAST_PATH_MASK) != 0) {
            // fast path lock is used
            return false;
        }
        return atomicTail.compareAndSet(null, null,
                stamp, (stamp + 1) | FAST_PATH_MASK);
    }

    private void waitForFastPath() {
        System.out.println("fast path wait");
        while ((atomicTail.getStamp() & FAST_PATH_MASK) != 0) {
            // spin
        }
    }

    @Override
    public void unlock() {
        if (!fastPathUnlock()) {
            super.unlock();
        }
    }

    private boolean fastPathUnlock() {
        if ((atomicTail.getStamp() & FAST_PATH_MASK) == 0) {
            // not fast path
            return false;
        }

        System.out.println("fast path unlock");

        // fast path mark will be set while unlock
        // so it is necessary to ensure clearing the mark by loop
        Node node;
        int[] stampHolder = new int[1];
        do {
            node = atomicTail.get(stampHolder);
        } while (!atomicTail.compareAndSet(node, node, stampHolder[0], stampHolder[0] & (~FAST_PATH_MASK)));
        return true;
    }
}
