package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StripedHashSet<T> extends BaseHashSet<T> {

    private final ReentrantLock[] locks;

    public StripedHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    protected void acquire(T x) {
        locks[x.hashCode() % locks.length].lock();
    }

    @Override
    protected void release(T x) {
        locks[x.hashCode() % locks.length].unlock();
    }

    @Override
    protected boolean policy() {
        return setSize / table.length > 4;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resize() {
        int oldCapacity = table.length;
        for (Lock lock : locks) {
            lock.lock();
        }
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
            for (Lock lock : locks) {
                lock.unlock();
            }
        }
    }

}
