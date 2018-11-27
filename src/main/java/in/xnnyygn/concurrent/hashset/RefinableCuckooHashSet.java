package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableCuckooHashSet<T> extends PhasedCuckooHashSet<T> {
    private final AtomicMarkableReference<Thread> atomicOwner;
    private volatile ReentrantLock[][] locks;

    public RefinableCuckooHashSet(int capacity) {
        super(capacity);
        atomicOwner = new AtomicMarkableReference<>(null, false);
        locks = new ReentrantLock[2][capacity];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                locks[i][j] = new ReentrantLock();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resize() {
        int oldCapacity = capacity;
        Thread me = Thread.currentThread();
        if (!atomicOwner.compareAndSet(null, me, false, true)) {
            return;
        }
        try {
            if (capacity != oldCapacity) {
                return;
            }
            for (ReentrantLock lock : locks[0]) {
                while (lock.isLocked()) {
                }
            }
            capacity = oldCapacity * 2;
            List<T>[][] oldTable = table;
            table = (List<T>[][]) new List[2][capacity];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < capacity; j++) {
                    table[i][j] = new ArrayList<>();
                }
            }
            for (List<T>[] column : table) {
                for (List<T> cell : column) {
                    for (T x : cell) {
                        add(x);
                    }
                }
            }
        } finally {
            atomicOwner.set(null, false);
        }
    }

    @Override
    protected void acquire(T x) {
        boolean[] markHolder = new boolean[1];
        Thread me = Thread.currentThread();
        Thread who;
        ReentrantLock lock0;
        ReentrantLock lock1;
        ReentrantLock[][] oldLocks;
        while (true) {
            do {
                who = atomicOwner.get(markHolder);
            } while (markHolder[0] && who != me);

            oldLocks = locks;

            lock0 = locks[0][hash0(x, locks[0].length)];
            lock1 = locks[1][hash1(x, locks[1].length)];
            lock0.lock();
            lock1.lock();

            who = atomicOwner.get(markHolder);
            if ((!markHolder[0] || who == me) && locks == oldLocks) {
                break;
            } else {
                lock0.unlock();
                lock1.unlock();
            }
        }
    }

    @Override
    protected void release(T x) {
        locks[0][hash0(x, locks[0].length)].unlock();
        locks[1][hash1(x, locks[1].length)].unlock();
    }
}
