package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("Duplicates")
public class SimpleFuture2<T> {

    private volatile boolean completed = false;
    private volatile T value;
    private volatile Thread thread = null;
    final ThreadScheduler scheduler = new ThreadScheduler();

    public T get() {
        while (true) {
            if (completed) {
                return value;
            }
            if (thread == null) {
                thread = Thread.currentThread();
            } else {
                scheduler.park(this);
            }
        }
    }

    public T get(long time, @Nonnull TimeUnit unit) throws TimeoutException {
        final long deadline = System.nanoTime() + unit.toNanos(time);
        while (true) {
            if (completed) {
                return value;
            }
            if (thread == null) {
                thread = Thread.currentThread();
            } else if (System.nanoTime() > deadline) {
                throw new TimeoutException();
            } else {
                scheduler.parkUntil(this, deadline);
            }
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
