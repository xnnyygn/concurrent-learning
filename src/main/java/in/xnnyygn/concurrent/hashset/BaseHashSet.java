package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;

// closed-address
public abstract class BaseHashSet<T> {

    protected List<T>[] table;
    protected int setSize; // ?

    @SuppressWarnings("unchecked")
    public BaseHashSet(int capacity) {
        setSize = 0;
        table = (List<T>[]) new List[capacity];
        for (int i = 0; i < capacity; i++) {
            table[i] = new ArrayList<>();
        }
    }

    public boolean add(T x) {
        boolean result;
        acquire(x);
        try {
            int bucketNo = x.hashCode() % table.length;
            result = table[bucketNo].add(x);
            if (result) {
                setSize++;
            }
        } finally {
            release(x);
        }
        if (policy()) {
            resize();
        }
        return result;
    }

    public boolean remove(T x) {
        boolean result;
        acquire(x);
        try {
            int bucketNo = x.hashCode() % table.length;
            result = table[bucketNo].remove(x);
            if (result) {
                setSize--;
            }
        } finally {
            release(x);
        }
        return result;
    }

    public boolean contains(T x) {
        acquire(x);
        try {
            int bucketNo = x.hashCode() % table.length;
            return table[bucketNo].contains(x);
        } finally {
            release(x);
        }
    }

    protected abstract void acquire(T x);

    protected abstract void release(T x);

    protected abstract boolean policy();

    protected abstract void resize();

}
