package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RWStripedHashSet<T> extends BaseHashSet<T> {

    private final Lock readerLock;
    private final Lock writerLock;
    private volatile Lock[] locks;

    public RWStripedHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readerLock = lock.readLock();
        writerLock = lock.writeLock();
    }

    @Override
    protected void acquire(T x) {
        readerLock.lock();
        locks[x.hashCode() % locks.length].lock();
        // acquire two locks
    }

    @Override
    protected void release(T x) {
        locks[x.hashCode() % locks.length].unlock();
        readerLock.unlock();
    }

    @Override
    protected boolean policy() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resize() {
        int oldCapacity = table.length;
        writerLock.lock();
        try {
            if (table.length != oldCapacity) {
                return;
            }
            List<T>[] oldTable = table;
            int newCapacity = oldTable.length * 2;
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

            Lock[] oldLocks = locks;
            locks = new ReentrantLock[oldLocks.length * 2];
        } finally {
            writerLock.unlock();
        }
    }
}
