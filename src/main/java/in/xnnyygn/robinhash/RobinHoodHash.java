package in.xnnyygn.robinhash;

import javax.annotation.Nonnull;

public class RobinHoodHash<K, V> {

    private final Entry<K, V>[] entries;
    private int maxDistance = 0;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public RobinHoodHash(int capacity) {
        this.entries = (Entry<K, V>[]) new Entry[capacity];
    }

    public boolean add(@Nonnull K key, V value) {
        Entry<K, V> newEntry = new Entry<>(key, value);
        int index = hash(key);
        int distance = 0;
        while (true) {
            Entry<K, V> entry = entries[index];
            if (entry == null) {
                entries[index] = newEntry;
                size++;
                return true;
            }
            // entries[index] != null
            if (entry.key.equals(key)) {
                entry.value = value;
                return true;
            }
            int entryDistance = displacement(entry, index);
            if (entryDistance < distance) {
                // swap new entry and entries[index]
                entries[index] = newEntry;
                newEntry = entry;
                distance = entryDistance + 1; // no need to wrap
            } else {
                distance++;
            }
            index = nextIndex(index);
            maxDistance = Math.max(distance, maxDistance);
        }
    }

    private int nextIndex(int index) {
        return (index + 1) % entries.length;
    }

    private int hash(@Nonnull K key) {
        int h = key.hashCode();
        return Math.abs(h) % entries.length;
    }

    private int displacement(@Nonnull Entry<K, V> entry, int index) {
        int hash = hash(entry.key);
        return (index >= hash) ? (index - hash) : (index + entries.length - hash);
    }

    public boolean contains(@Nonnull K key) {
        for (int index = hash(key), d = 0; d <= maxDistance; d++) {
            Entry<K, V> entry = entries[index];
            if (entry == null) {
                return false;
            }
            if (entry.key.equals(key)) {
                return true;
            }
            index = nextIndex(index);
        }
        return false;
    }

    public boolean remove(@Nonnull K key) {
        for (int index = hash(key), d = 0; d <= maxDistance; d++) {
            Entry<K, V> entry = entries[index];
            if (entry == null) {
                return false;
            }
            if (entry.key.equals(key)) {
                entries[index] = null;
                size--;
                return true;
            }
            index = nextIndex(index);
        }
        return false;
    }

    private static class Entry<K, V> {
        final K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
