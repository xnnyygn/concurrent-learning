package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

public class RefinableHashSet<T> extends BaseHashSet<T> {

    private final AtomicMarkableReference<Thread> atomicOwner;
    private volatile ReentrantLock[] locks;

    public RefinableHashSet(int capacity) {
        super(capacity);
        locks = new ReentrantLock[capacity];
        for (int i = 0; i < capacity; i++) {
            locks[i] = new ReentrantLock();
        }
        atomicOwner = new AtomicMarkableReference<>(null, false);
    }

    @Override
    protected void acquire(T x) {
        boolean[] markHolder = {true};
        Thread me = Thread.currentThread();
        Thread who;
        ReentrantLock[] oldLocks;
        ReentrantLock oldLock;
        while (true) {
            // marked by other thread
            // someone is resizing
            do {
                who = atomicOwner.get(markHolder);
            } while (markHolder[0] && who != me);

            // no one is resizing
            oldLocks = locks;
            oldLock = oldLocks[x.hashCode() % oldLocks.length];
            oldLock.lock();
            who = atomicOwner.get(markHolder);

            // locks not changed
            // not marked or owner by self(reentrant?)
            // otherwise resized or resizing
            if ((!markHolder[0] || who == me) && locks == oldLocks) {
                return;
            }

            // unlock old lock and try again
            oldLock.unlock();
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
        Thread me = Thread.currentThread();

        // condition 1
        // check if other thread is resizing
        // linearization point
        if (!atomicOwner.compareAndSet(null, me, false, true)) {
            return;
        }

        try {
            // condition 2
            // check if other thread has resized
            // linearization point
            // e.g
            // A resize, B resize
            // A starts to resize
            // A pass condition 1
            // B pass condition 2
            // A resized
            // B starts to resize
            // B pass condition 1
            // B will find A has resized
            if (table.length != oldCapacity) {
                return;
            }

            // quiesce
            // wait for add/remove
            for (ReentrantLock lock : locks) {
                while (lock.isLocked()) {
                }
            }

            List<T>[] oldTable = table;
            int newCapacity = 2 * oldCapacity;

            // linearization point
            table = (List<T>[]) new List[newCapacity];
            for (int i = 0; i < newCapacity; i++) {
                table[i] = new ArrayList<>();
            }
            locks = new ReentrantLock[newCapacity];
            for (int j = 0; j < locks.length; j++) {
                locks[j] = new ReentrantLock();
            }
            initializeFrom(oldTable);
        } finally {
            // linearization point
            atomicOwner.set(null, false);
        }
    }

    private void initializeFrom(List<T>[] oldTable) {
        for (List<T> bucket : oldTable) {
            for (T x : bucket) {
                table[x.hashCode() % table.length].add(x);
            }
        }
    }
}
