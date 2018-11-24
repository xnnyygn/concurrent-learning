package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableHashSet2<T> extends BaseHashSet<T> {

    private final AtomicBoolean atomicResizing = new AtomicBoolean(false);
    private volatile ReentrantLock[] locks;

    public RefinableHashSet2(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    protected void acquire(T x) {
        ReentrantLock[] oldLocks;
        ReentrantLock oldLock;
        while (true) {
            // wait for resizing
            while (atomicResizing.get()) {
            }

            oldLocks = locks;
            oldLock = oldLocks[x.hashCode() % oldLocks.length];
            oldLock.lock();

            // recheck
            if (!atomicResizing.get() && locks == oldLocks) {
                break;
            }

            oldLock.unlock(); // unlock and retry
        }
    }

    @Override
    protected void release(T x) {
        locks[x.hashCode() % locks.length].unlock();
    }

    @Override
    protected boolean policy() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resize() {
        int oldCapacity = table.length;

        // someone is resizing
        if (!atomicResizing.compareAndSet(false, true)) {
            return;
        }

        // check if someone resized
        if (table.length != oldCapacity) {
            atomicResizing.set(false);
            return;
        }

        // resizing
        List<T>[] oldTable = table;
        int newCapacity = oldCapacity * 2;
        table = (List<T>[]) new List[newCapacity];
        locks = new ReentrantLock[newCapacity];
        for (int i = 0; i < newCapacity; i++) {
            table[i] = new ArrayList<>();
            locks[i] = new ReentrantLock();
        }
        for (List<T> bucket : oldTable) {
            for (T x : bucket) {
                table[x.hashCode() % newCapacity].add(x);
            }
        }
        atomicResizing.set(false);
    }
}
