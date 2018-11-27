package in.xnnyygn.concurrent.hashset;

import java.util.ArrayList;
import java.util.List;

public abstract class PhasedCuckooHashSet<T> {

    static final int PROBE_SIZE = 4;
    static final int THRESHOLD = 2;
    static final int LIMIT = 32;

    volatile int capacity;
    volatile List<T>[][] table;

    @SuppressWarnings("unchecked")
    public PhasedCuckooHashSet(int capacity) {
        this.capacity = capacity;
        table = (List<T>[][]) new List[2][capacity];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                table[i][j] = new ArrayList<>();
            }
        }
    }

    public boolean remove(T x) {
        acquire(x);
        try {
            List<T> set0 = table[0][hash0(x, capacity)];
            if (set0.remove(x)) {
                return true;
            }
            List<T> set1 = table[1][hash1(x, capacity)];
            return set1.remove(x);
        } finally {
            release(x);
        }
    }

    public boolean add(T x) {
        boolean failedToAdd = false;
        int i = -1;
        int hi = -1;
        acquire(x);
        try {
            if (contains(x)) {
                return false;
            }
            int index0 = hash0(x, capacity);
            List<T> set0 = table[0][index0];
            if (set0.size() < THRESHOLD) {
                set0.add(x);
                return true;
            }
            int index1 = hash1(x, capacity);
            List<T> set1 = table[1][index1];
            if (set1.size() < THRESHOLD) {
                set1.add(x);
                return true;
            }
            if (set0.size() < PROBE_SIZE) {
                set0.add(x);
                i = 0;
                hi = index0;
            } else if (set1.size() < PROBE_SIZE) {
                set1.add(x);
                i = 1;
                hi = index1;
            } else {
                failedToAdd = true;
            }
        } finally {
            release(x);
        }
        if (failedToAdd) {
            resize();
            add(x);
            return true;
        }
        if (!relocate(i, hi)) {
            resize();
        }
        return true;
    }

    protected boolean relocate(int i, int hi) {
        if (i != 0 && i != 1) {
            throw new IllegalArgumentException("i must be 0 or 1");
        }
        int j = 1 - i;
        int hj;
        List<T> setI;
        List<T> setJ;
        T x;
        for (int r = 0; r < LIMIT; r++) {
            setI = table[i][hi];
            x = setI.get(0);
            hj = (i == 0) ? hash1(x, capacity) : hash0(x, capacity);

            acquire(x);
            try {
                setJ = table[j][hj];
                // x may be removed by another thread
                if (setI.remove(x)) {
                    if (setJ.size() < THRESHOLD) {
                        setJ.add(x);
                        return true;
                    }
                    if (setJ.size() >= PROBE_SIZE) {
                        setI.add(x);
                        return false;
                    }
                    // size < probe size
                    // swap i and j
                    setJ.add(x);
                    i = 1 - i;
                    j = 1 - j;
                    hi = hj;
                } else if (setI.size() < THRESHOLD) { // set i is ok
                    return true;
                }
            } finally {
                release(x);
            }
        }
        return false;
    }

    protected abstract void resize();

    public boolean contains(T x) {
        int index0 = hash0(x, capacity);
        if (table[0][index0].contains(x)) {
            return true;
        }
        int index1 = hash1(x, capacity);
        return table[1][index1].contains(x);
    }

    protected int hash0(T x, int capacity) {
        return (x.hashCode() % 7) % capacity;
    }

    protected int hash1(T x, int capacity) {
        return (x.hashCode() % 11) % capacity;
    }

    protected abstract void acquire(T x);

    protected abstract void release(T x);

}
