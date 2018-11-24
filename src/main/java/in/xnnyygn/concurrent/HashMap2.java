package in.xnnyygn.concurrent;

// hash entries + linked list
// TODO resize
public class HashMap2<K, V> {

    private static final int MAX_CAPACITY = 1 << 30;
    private final double factor;
    private Table<K, V> table;

    public HashMap2() {
        this(16, 0.75f);
    }

    @SuppressWarnings("unchecked")
    public HashMap2(int capacity, double factor) {
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        if (factor <= 0) {
            throw new IllegalArgumentException("threshold <= 0");
        }
        this.factor = factor;
        this.table = new Table<>(evaluateCapacity(capacity));
    }

    private static int evaluateCapacity(int capacity) {
        int numberWithHighestBit = Integer.highestOneBit(capacity);
        return numberWithHighestBit == capacity ? numberWithHighestBit : numberWithHighestBit << 1;
    }

    public V put(K key, V value) {
        ensureCapacityFactor();
        return table.put(key, value);
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacityFactor() {
        int capacity = table.capacity();
        if (capacity == MAX_CAPACITY || table.size() < capacity * factor) {
            return;
        }
        Table<K, V> newTable = new Table<>(capacity << 1);
        Entry<K, V> entry;
        for (int i = 0; i < table.capacity(); i++) {
            entry = table.entry(i);
            while (entry != null) {
                newTable.put(entry.key, entry.value);
                entry = entry.next;
            }
        }
        table = newTable;
    }

    public V get(K key) {
        return table.get(key);
    }

    public V remove(K key) {
        return table.remove(key);
    }

    public int size() {
        return table.size();
    }

    private static class Table<K, V> {

        private final Entry<K, V>[] entries;
        private int cachedSize;

        @SuppressWarnings("unchecked")
        Table(int capacity) {
            entries = new Entry[capacity];
            cachedSize = 0;
        }

        V put(K key, V value) {
            int index = entryIndex(key);
            Entry<K, V> entry = entries[index];
            if (entry == null) {
                entries[index] = new Entry<>(key, value);
                cachedSize++;
                return null;
            }
            V previousValue;
            while (true) {
                if (entry.key.equals(key)) {
                    previousValue = entry.value;
                    entry.value = value;
                    break;
                }
                if (entry.next == null) {
                    previousValue = null;
                    entry.next = new Entry<>(key, value);
                    cachedSize++;
                    break;
                }
                entry = entry.next;
            }
            return previousValue;
        }

        private int entryIndex(K key) {
            if (key == null) {
                throw new IllegalArgumentException("key is null");
            }
            return key.hashCode() & (entries.length - 1);
        }

        int capacity() {
            return entries.length;
        }

        Entry<K, V> entry(int index) {
            return entries[index];
        }

        int size() {
            return cachedSize;
        }

        V get(K key) {
            Entry<K, V> entry = entries[entryIndex(key)];
            while (entry != null) {
                if (entry.key.equals(key)) {
                    return entry.value;
                }
                entry = entry.next;
            }
            return null;
        }

        V remove(K key) {
            int index = entryIndex(key);
            Entry<K, V> entry = entries[index];
            V previousValue = null;
            Entry<K, V> predecessorEntry = null;
            while (entry != null) {
                if (entry.key.equals(key)) {
                    previousValue = entry.value;
                    if (predecessorEntry != null) {
                        predecessorEntry.next = entry.next;
                    } else {
                        entries[index] = entry.next;
                    }
                    cachedSize--;
                    break;
                }
                predecessorEntry = entry;
                entry = entry.next;
            }
            return previousValue;
        }

    }

    private static class Entry<K, V> {

        private final K key;
        private V value;
        private Entry<K, V> next;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

    }

}
