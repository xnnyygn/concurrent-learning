package in.xnnyygn.concurrent.lock;

import java.util.concurrent.TimeUnit;

public class CompositeLock2 extends CompositeLock {

    private static final int FAST_PATH = 0x080000;

    public CompositeLock2(int capacity) {
        super(capacity);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (fastPathLock()) {
            return true;
        }
        if (super.tryLock(time, unit)) {
            while ((tail.getStamp() & FAST_PATH) != 0) {
            }
            return true;
        }
        return false;
    }

    private boolean fastPathLock() {
        int[] stamp = {0};
        Node n = tail.get(stamp);
        if (n != null) {
            return false;
        }
        if ((stamp[0] & FAST_PATH) != 0) {
            return false;
        }
        return tail.compareAndSet(null, null, stamp[0], (stamp[0] + 1) & FAST_PATH);
    }

    @Override
    public void unlock() {
        if (!fastPathUnlock()) {
            super.unlock();
        }
    }

    private boolean fastPathUnlock() {
        int[] stamp = {0};
        Node n = tail.get(stamp);
        if ((stamp[0] & FAST_PATH) == 0) {
            return false;
        }
        while (!tail.compareAndSet(n, n, stamp[0], (stamp[0] & (~FAST_PATH)))) {
            n = tail.get(stamp);
        }
        return true;
    }

}
