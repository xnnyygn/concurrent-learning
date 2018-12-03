package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestAndTestAndSetLock {

    private final AtomicBoolean atomicValue = new AtomicBoolean(false);

    public void lock() {
        while (true) {
            while (atomicValue.get()) {
            }
            if (!atomicValue.getAndSet(true)) {
                return;
            }
        }
    }

    public void unlock() {
        atomicValue.set(false);
    }
}
