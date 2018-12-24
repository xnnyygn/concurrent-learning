package in.xnnyygn.concurrent.exchange;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Exchanger3<T> {

    private static final int STATE_EMPTY = 0;
    private static final int STATE_FIRST = 1;
    private static final int STATE_SECOND = 2;

    private static final int THREAD_STATE_NORMAL = 0;
    private static final int THREAD_STATE_PARK = 1;
    private static final int THREAD_STATE_WAKE_UP = 2;

    private final AtomicInteger state = new AtomicInteger(STATE_EMPTY);
    private volatile Payload<T> payload = null;

    public T exchange(T value, long time, TimeUnit unit) throws TimeoutException, InterruptedException {
        final long startTime = System.nanoTime();
        final long timeout = unit.toNanos(time);
        final Payload<T> payload = new Payload<>(value, Thread.currentThread());
        int s;
        Payload<T> p;
        boolean first = false;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            s = state.get();
            if (first) {
                if (s == STATE_SECOND) {
                    p = waitForPayload();
                    state.set(STATE_EMPTY);
                    return p.value;
                }
                // assert reference.getStamp() == STATE_FIRST
                if (state.compareAndSet(STATE_FIRST, STATE_EMPTY)) {
                    throw new TimeoutException();
                }
                // assert reference.getStamp() == STATE_SECOND
                p = waitForPayload();
                state.set(STATE_EMPTY);
                return p.value;
            }
            if (s == STATE_EMPTY && state.compareAndSet(STATE_EMPTY, STATE_FIRST)) {
                this.payload = payload;
                first = true;
                payload.park(timeout);
            } else if (s == STATE_FIRST && state.compareAndSet(STATE_FIRST, STATE_SECOND)) {
                p = waitForPayload();
                this.payload = payload;
                p.wakeUp();
                return p.value;
            } else if (System.nanoTime() - startTime > timeout) {
                throw new TimeoutException();
            }
        }
    }

    private Payload<T> waitForPayload() {
        Payload<T> p;
        while ((p = this.payload) == null) {
            Thread.yield();
        }
        return p;
    }

    private static class Payload<T> {
        final T value;
        final Thread thread;
        final AtomicInteger threadState = new AtomicInteger(THREAD_STATE_NORMAL);

        Payload(T value, Thread thread) {
            this.value = value;
            this.thread = thread;
        }

        void park(long nanos) {
            if (threadState.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_PARK)) {
                LockSupport.parkNanos(this, nanos);
                if (!threadState.compareAndSet(THREAD_STATE_PARK, THREAD_STATE_WAKE_UP)) {
                    Thread.interrupted();
                }
            }
        }

        void wakeUp() {
            int ts = threadState.get();
            if (ts == THREAD_STATE_NORMAL && threadState.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_WAKE_UP)) {
                return;
            }
            if (ts == THREAD_STATE_PARK && threadState.compareAndSet(THREAD_STATE_PARK, THREAD_STATE_WAKE_UP)) {
                thread.interrupt();
            }
        }
    }
}
