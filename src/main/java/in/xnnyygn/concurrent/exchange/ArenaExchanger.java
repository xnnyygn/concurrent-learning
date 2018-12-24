package in.xnnyygn.concurrent.exchange;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

@SuppressWarnings("Duplicates")
public class ArenaExchanger<T> {

    private static final int SPINS = 1 << 5;

    private final ThreadLocal<Pair<T>> myPair = ThreadLocal.withInitial(Pair::new);
    private final AtomicReferenceArray<Pair<T>> arena;

    public ArenaExchanger() {
        this(Math.max(Runtime.getRuntime().availableProcessors() >> 1, 1));
    }

    public ArenaExchanger(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        arena = new AtomicReferenceArray<>(capacity);
    }

    public T exchange(T value, long time, TimeUnit unit) throws TimeoutException, InterruptedException {
        final long deadline = System.nanoTime() + unit.toNanos(time);
        final Pair<T> myPair = this.myPair.get();
        myPair.item = value;
        Pair<T> p;
        int i = 0;
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            p = arena.get(i);
            if (p == null) {
                if (arena.compareAndSet(i, null, myPair)) {
                    myPair.index = i;
                    return waitForMatch(myPair, deadline);
                }
                i++;
                if (i >= arena.length()) {
                    i = 0;
                }
                continue;
            }
            if (arena.compareAndSet(i, p, null)) {
                T item = p.item;
                p.match = value;
                p.wakeUp();
                return item;
            }
            if (System.nanoTime() > deadline) {
                myPair.item = null;
                throw new TimeoutException();
            }
        }
    }

    private T waitForMatch(Pair<T> myPair, long deadline) throws TimeoutException, InterruptedException {
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
            if (arena.get(myPair.index) != myPair) {
                spins = SPINS;
            } else if (spins < 0 && nanos > 0) {
                myPair.parkUntil(deadline);
            } else if (nanos < 0 && arena.compareAndSet(myPair.index, myPair, null)) {
                myPair.reset();
                throw new TimeoutException();
            }
        }
    }

    @SuppressWarnings("Duplicates")
    @sun.misc.Contended
    private static final class Pair<T> {

        private static final int THREAD_STATE_NORMAL = 0;
        private static final int THREAD_STATE_PARK = 1;
        private static final int THREAD_STATE_WAKE_UP = 2;

        int index = -1;
        T item = null;
        volatile T match = null;

        final Thread thread;
        final AtomicInteger threadState = new AtomicInteger(THREAD_STATE_NORMAL);

        Pair() {
            this.thread = Thread.currentThread();
        }

        void reset() {
            index = -1;
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
