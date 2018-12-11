package in.xnnyygn.concurrent.monitor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class ThreadScheduler {

    private static final int THREAD_STATE_NORMAL = 0;
    private static final int THREAD_STATE_PARK = 1;
    private static final int THREAD_STATE_PARK_TIMED = 2;
    private static final int THREAD_STATE_WAKE_UP = 3;

    private final AtomicInteger state = new AtomicInteger(THREAD_STATE_NORMAL);

    public boolean park(Object blocker) {
        if (state.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_PARK)) {
            LockSupport.park(blocker);
            return true;
        }
        return false;
    }

    public boolean parkUntil(Object blocker, long deadline) {
        if (!state.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_PARK_TIMED)) {
            return false;
        }
        LockSupport.parkUntil(blocker, deadline);
        if (!state.compareAndSet(THREAD_STATE_PARK_TIMED, THREAD_STATE_WAKE_UP)) {
            Thread.interrupted();
        }
        return true;
    }

    public void wakeUp(Thread thread) {
        int s = state.get();
        if (s == THREAD_STATE_NORMAL && state.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_WAKE_UP)) {
            return;
        }
        if (s == THREAD_STATE_PARK) {
            state.set(THREAD_STATE_WAKE_UP);
            LockSupport.unpark(thread);
        }
        if (s == THREAD_STATE_PARK_TIMED && state.compareAndSet(THREAD_STATE_PARK_TIMED, THREAD_STATE_WAKE_UP)) {
            thread.interrupt();
        }
    }

    @Override
    public String toString() {
        return "ThreadScheduler{" +
                "state=" + state.get() +
                '}';
    }
}
