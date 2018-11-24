package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CoarseHashSet<T> extends BaseHashSet<T> {

    private final Lock lock;

    public CoarseHashSet(int capacity) {
        super(capacity);
        lock = new ReentrantLock();
    }

    @Override
    protected void acquire(T x) {
        lock.lock();
    }

    @Override
    protected void release(T x) {
        lock.unlock();
    }

    @Override
    protected boolean policy() {
        return setSize / table.length > 4;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resize() {
        int oldCapacity = table.length;
        lock.lock();
        try {
            if (oldCapacity != table.length) {
                return; // someone resized
            }
            int newCapacity = oldCapacity * 2;
            List<T>[] oldTable = table;

            // initialize new table
            table = (List<T>[]) new List[newCapacity];
            for (int i = 0; i < newCapacity; i++) {
                table[i] = new ArrayList<>();
            }

            // transfer elements
            for (List<T> bucket : oldTable) {
                for (T x : bucket) {
                    table[x.hashCode() % table.length].add(x);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
