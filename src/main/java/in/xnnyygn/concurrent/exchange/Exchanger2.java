package in.xnnyygn.concurrent.exchange;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Exchanger2<T> {

    private static final int STATE_EMPTY = 0;
    private static final int STATE_WAITING = 1;
    private static final int STATE_EXCHANGED = 2;

    private static final int THREAD_STATE_NORMAL = 0;
    private static final int THREAD_STATE_PARK = 1;
    private static final int THREAD_STATE_WAKE_UP = 2;

    private final AtomicStampedReference<Payload<T>> reference = new AtomicStampedReference<>(null, STATE_EMPTY);

    public T exchange(T value, long time, TimeUnit unit) throws TimeoutException, InterruptedException {
        final long startTime = System.nanoTime();
        final long timeout = unit.toNanos(time);
        final Payload<T> payload = new Payload<>(value, Thread.currentThread());
        int[] stateHolder = new int[1];
        Payload<T> p;
        boolean first = false;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            p = reference.get(stateHolder);
            if (first) {
                if (stateHolder[0] == STATE_EXCHANGED) {
                    reference.set(null, STATE_EMPTY);
                    return p.value;
                }
                // assert reference.getStamp() == STATE_WAITING
                if (reference.compareAndSet(payload, null, STATE_WAITING, STATE_EMPTY)) {
                    throw new TimeoutException();
                }
                // assert reference.getStamp() == STATE_EXCHANGED
                p = reference.getReference();
                reference.set(null, STATE_EMPTY);
                return p.value;
            }
            if (stateHolder[0] == STATE_EMPTY && reference.compareAndSet(null, payload, STATE_EMPTY, STATE_WAITING)) {
                first = true;
                payload.park(timeout);
            } else if (stateHolder[0] == STATE_WAITING && reference.compareAndSet(p, payload, STATE_WAITING, STATE_EXCHANGED)) {
                p.wakeUp();
                return p.value;
            } else if (System.nanoTime() - startTime > timeout) {
                throw new TimeoutException();
            }
        }
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
