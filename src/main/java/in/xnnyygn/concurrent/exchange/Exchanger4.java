package in.xnnyygn.concurrent.exchange;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class Exchanger4<T> {

    private static final int THREAD_STATE_NORMAL = 0;
    private static final int THREAD_STATE_PARK = 1;
    private static final int THREAD_STATE_WAKE_UP = 2;

    private static final int SPINS = 1 << 5;

    private final ThreadLocal<Pair<T>> myPair = ThreadLocal.withInitial(Pair::new);
    private final AtomicReference<Pair<T>> slot = new AtomicReference<>(null);

    public T exchange(T value, long time, TimeUnit unit) throws TimeoutException, InterruptedException {
        final long deadline = System.nanoTime() + unit.toNanos(time);
        final Pair<T> myPair = this.myPair.get();
        myPair.item = value;
        Pair<T> p;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            p = slot.get();
            if (p == null && slot.compareAndSet(null, myPair)) { // pair first
                return waitForMatchSpinPark(myPair, deadline);
            } else if (slot.compareAndSet(p, null)) { // pair second
                assert p != null;
                // item will be reset, so read item to local variable before setting match
                T item = p.item;
                p.match = value;
                p.wakeUp();
                return item;
            } else if (System.nanoTime() > deadline) {
                myPair.item = null;
                throw new TimeoutException();
            }
        }
    }

    private T waitForMatchPark(Pair<T> myPair, long deadline) throws TimeoutException, InterruptedException {
        myPair.parkUntil(deadline);
        T m = myPair.match;
        if (m != null) {
            myPair.reset();
            return m;
        }
        if (slot.compareAndSet(myPair, null)) {
            myPair.reset();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            } else {
                throw new TimeoutException();
            }
        }
        m = myPair.match;
        myPair.reset();
        return m;
    }

    private T waitForMatchSpinPark(Pair<T> myPair, long deadline) throws TimeoutException, InterruptedException {
        int spins = SPINS;
        T m;
        long nanos;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            m = myPair.match;
            if (m != null) {
                myPair.reset();
                return m;
            }
            spins--;
            nanos = deadline - System.nanoTime();
            if (slot.get() != myPair) {
                // slot changed, match will come
                spins = SPINS;
            } else if (spins < 0 && nanos > 0) { // spin -> park
                myPair.parkUntil(deadline);
            } else if (nanos < 0 && slot.compareAndSet(myPair, null)) { // timeout
                myPair.reset();
                throw new TimeoutException();
            }
        }
    }

    private static final class Pair<T> {
        T item = null;
        volatile T match = null;

        final Thread thread;
        final AtomicInteger threadState = new AtomicInteger(THREAD_STATE_NORMAL);

        Pair() {
            this.thread = Thread.currentThread();
        }

        void reset() {
            item = null;
            match = null; // lazy set is also ok
            int ts = threadState.get();
            if (ts != THREAD_STATE_NORMAL) {
                threadState.set(THREAD_STATE_NORMAL); // lazy set is also ok
            }
        }

        void parkUntil(long deadline) {
            long nanos = deadline - System.nanoTime();
            if (nanos <= 0) {
                return;
            }
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
