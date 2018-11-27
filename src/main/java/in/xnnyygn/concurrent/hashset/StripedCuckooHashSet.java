package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedCuckooHashSet<T> extends PhasedCuckooHashSet<T> {

    private final ReentrantLock[][] locks;

    public StripedCuckooHashSet(int capacity) {
        super(capacity);
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
        for (Lock lock : locks[0]) {
            lock.lock();
        }
        try {
            if (capacity != oldCapacity) {
                return;
            }
            List<T>[][] oldTable = table;
            int newCapacity = oldCapacity * 2;
            table = new List[2][newCapacity];
            int i, j;
            for (i = 0; i < 2; i++) {
                for (j = 0; j < newCapacity; j++) {
                    table[i][j] = new ArrayList<>();
                }

            }
            for (i = 0; i < 2; i++) {
                for (j = 0; j < oldCapacity; j++) {
                    for (T x : oldTable[i][j]) {
                        add(x);
                    }
                }
            }
        } finally {
            for (Lock lock : locks[0]) {
                lock.unlock();
            }
        }
    }

    @Override
    protected void acquire(T x) {
        locks[0][hash0(x, locks[0].length)].lock();
        locks[1][hash1(x, locks[1].length)].lock();
    }

    @Override
    protected void release(T x) {
        locks[0][hash0(x, locks[0].length)].unlock();
        locks[1][hash1(x, locks[1].length)].unlock();
    }
}
