package in.xnnyygn.concurrent.hashset;

import java.util.Arrays;

public class CuckooHashSet<T> {

    private static final int RETRY_LIMIT = 32;
    private int capacity;
    private T[][] table;

    @SuppressWarnings("unchecked")
    public CuckooHashSet(int capacity) {
        this.capacity = capacity;
        table = (T[][]) new Object[2][capacity];
    }

    public boolean add(T x) {
        if (x == null) {
            throw new IllegalArgumentException("x required");
        }
        if (contains(x)) {
            return false;
        }
        if (doAdd(x, table, capacity)) {
            return true;
        }
        resize();
        return add(x);
    }

    private boolean doAdd(T x, T[][] table, int capacity) {
        T v = x;
        int index0, index1;
        int r = 0;
        while (r++ < RETRY_LIMIT) {
            index0 = hash0(v, capacity);
            v = swap0(index0, v, table);
            if (v == null) {
                return true;
            }
            index1 = hash1(v, capacity);
            v = swap1(index1, v, table);
            if (v == null) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        T[][] newTable = (T[][]) new Object[2][newCapacity];
        T x;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < capacity; j++) {
                x = table[i][j];
                if (x != null) {
                    // is it possible to fail?
                    doAdd(x, newTable, newCapacity);
                }
            }
        }
        table = newTable;
        capacity = newCapacity;
    }

    private T swap0(int index, T x, T[][] table) {
        T y = table[0][index];
        table[0][index] = x;
        return y;
    }

    private T swap1(int index, T x, T[][] table) {
        T y = table[1][index];
        table[1][index] = x;
        return y;
    }

    public boolean remove(T x) {
        if (x == null) {
            throw new IllegalArgumentException("x required");
        }
        int index0 = hash0(x, capacity);
        if (x.equals(table[0][index0])) {
            table[0][index0] = null;
            return true;
        }
        int index1 = hash1(x, capacity);
        if (x.equals(table[1][index1])) {
            table[1][index1] = null;
            return true;
        }
        return false;
    }

    public boolean contains(T x) {
        if (x == null) {
            throw new IllegalArgumentException("x required");
        }
        return x.equals(table[0][hash0(x, capacity)]) || x.equals(table[1][hash1(x, capacity)]);
    }

    private int hash0(T x, int capacity) {
        return (x.hashCode() % 7) % capacity;
    }

    private int hash1(T x, int capacity) {
        return (x.hashCode() % 11) % capacity;
    }

    @Override
    public String toString() {
        return "CuckooHashSet{" +
                "capacity=" + capacity +
                ", table=" + Arrays.deepToString(table) +
                '}';
    }
}
