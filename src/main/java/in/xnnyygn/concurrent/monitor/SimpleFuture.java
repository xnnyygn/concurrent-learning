package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleFuture<T> {

    private volatile boolean completed = false;
    private volatile T value;
    private volatile Thread thread = null;
    private final ThreadScheduler scheduler = new ThreadScheduler();

    public T get() {
        if (completed) {
            return value;
        }
        thread = Thread.currentThread();
        if (completed) {
            return value;
        }
        scheduler.park(this);
        return value;
    }

    public T get(long time, @Nonnull TimeUnit unit) throws TimeoutException {
        if (completed) {
            return value;
        }
        final long deadline = System.nanoTime() + unit.toNanos(time);
        thread = Thread.currentThread();
        if (completed) {
            return value;
        }
        if (scheduler.parkUntil(this, deadline)) {
            // 1. completed = true, interrupt/unpark
            // 2. completed = false, now > deadline(timeout)
            if (completed) {
                return value;
            }
            throw new TimeoutException();
        } else {
            return value;
        }
    }

    public void set(T value) {
        this.value = value;
        completed = true;
        Thread t = thread;
        if (t != null) {
            scheduler.wakeUp(t);
        }
    }

}
