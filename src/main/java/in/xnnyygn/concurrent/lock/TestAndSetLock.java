package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestAndSetLock {

    private final AtomicBoolean atomicValue = new AtomicBoolean(false);

    public void lock() {
        while (atomicValue.getAndSet(true)) {
        }
    }

    public void unlock() {
        atomicValue.set(false);
    }
}
