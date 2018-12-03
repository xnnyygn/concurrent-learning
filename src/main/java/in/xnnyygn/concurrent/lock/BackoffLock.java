package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicBoolean;

public class BackoffLock {

    private final AtomicBoolean state = new AtomicBoolean(false);

    public void lock() throws InterruptedException {
        Backoff backoff = new Backoff(400, 1000);
        while (true) {
            while (state.get()) {
            }
            if (!state.getAndSet(true)) {
                return;
            }
            backoff.backoff();
        }
    }

    public void unlock() {
        state.set(false);
    }
}
