package in.xnnyygn.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ConcurrentHashMap2<K, V> {

//    private static final int MAX_CAPACITY = 1 << 30;
//    private final double factor;
//    private volatile Table<K, V> table;
//
//    public ConcurrentHashMap2() {
//        this(16, 0.75f);
//    }
//
//    @SuppressWarnings("unchecked")
//    public ConcurrentHashMap2(int capacity, double factor) {
//        if (capacity <= 0 || capacity > MAX_CAPACITY) {
//            throw new IllegalArgumentException("capacity <= 0");
//        }
//        if (factor <= 0) {
//            throw new IllegalArgumentException("threshold <= 0");
//        }
//        this.factor = factor;
//        this.table = new Table<>(evaluateCapacity(capacity));
//    }
//
//    private static int evaluateCapacity(int capacity) {
//        int numberWithHighestBit = Integer.highestOneBit(capacity);
//        return numberWithHighestBit == capacity ? numberWithHighestBit : numberWithHighestBit << 1;
//    }
//
//    public V put(K key, V value) {
//        ensureCapacityFactor();
//        return table.put(key, value);
//    }
//
//    @SuppressWarnings("unchecked")
//    private void ensureCapacityFactor() {
//        int capacity = table.capacity();
//        if (capacity == MAX_CAPACITY || table.size() < capacity * factor) {
//            return;
//        }
//        Table<K, V> newTable = new Table<>(capacity << 1);
//        Entry<K, V> entry;
//        for (int i = 0; i < table.capacity(); i++) {
//            entry = table.entry(i);
//            while (entry != null) {
//                newTable.put(entry.key, entry.value);
//                entry = entry.next;
//            }
//        }
//        table = newTable;
//    }
//
//    public V get(K key) {
//        return table.get(key);
//    }
//
//    public V remove(K key) {
//        return table.remove(key);
//    }
//
//    private static class Table<K, V> {
//
//        private final AtomicReferenceArray<Entry<K, V>> atomicEntries;
//
//        @SuppressWarnings("unchecked")
//        Table(int capacity) {
//            this.atomicEntries = new AtomicReferenceArray<>(capacity);
//        }
//
//        V put(K key, V value) {
//            int index = entryIndex(key);
//            Entry<K, V> entry = atomicEntries.get(index);
//            if (entry == null) {
//                if (atomicEntries.compareAndSet(index, null, new Entry<>(key, value))) {
//                    return null;
//                }
//                // other thread inserts some entry
//            }
//            V previousValue;
//            while(true){
//                entry = atomicEntries.get(index);
//                if (entry.key.equals(key)) {
//                    previousValue = entry.value;
//                    entry.value = value;
//                    break;
//                }
//                if (!entry.hasNext()) {
//                    previousValue = null;
//                    entry.next = new Entry<>(key, value);
//                    break;
//                }
//                entry = entry.next;
//            }
//            return previousValue;
//        }
//
//        private int entryIndex(K key) {
//            if (key == null) {
//                throw new IllegalArgumentException("key is null");
//            }
//            return key.hashCode() & (entries.length - 1);
//        }
//
//        int capacity() {
//            return entries.length;
//        }
//
//        Entry<K, V> entry(int index) {
//            return entries[index];
//        }
//
//        V get(K key) {
//            Entry<K, V> entry = entries[entryIndex(key)];
//            while (entry != null) {
//                if (entry.key.equals(key)) {
//                    return entry.value;
//                }
//                entry = entry.next;
//            }
//            return null;
//        }
//
//        V remove(K key) {
//            int index = entryIndex(key);
//            Entry<K, V> entry = entries[index];
//            V previousValue = null;
//            Entry<K, V> predecessorEntry = null;
//            while (entry != null) {
//                if (entry.key.equals(key)) {
//                    previousValue = entry.value;
//                    if (predecessorEntry != null) {
//                        predecessorEntry.next = entry.next;
//                    } else {
//                        entries[index] = entry.next;
//                    }
//                    break;
//                }
//                predecessorEntry = entry;
//                entry = entry.next;
//            }
//            return previousValue;
//        }
//
//    }
//
//    private static class Entry<K, V> {
//
//        private final K key;
//        private volatile V value;
//        private final AtomicReference<Entry<K, V>> atomicNext = new AtomicReference<>();
//
//        Entry(K key, V value) {
//            this.key = key;
//            this.value = value;
//        }
//
//        boolean hasNext() {
//            return atomicNext.get() != null;
//        }
//
//    }

}
