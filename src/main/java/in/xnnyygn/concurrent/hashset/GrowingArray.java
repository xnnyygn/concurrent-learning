package in.xnnyygn.concurrent.hashset;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class GrowingArray<T> {

    private static final int NOT_RESIZED = 0;
    private static final int RESIZING = 1;
    private static final int RESIZED = 2;

    private final int capacity;
    private final T[] array;
    private final AtomicInteger resizeState = new AtomicInteger(NOT_RESIZED);
    private final int nextCapacity;
    private volatile GrowingArray<T> next;

    public GrowingArray(int capacity) {
        this(capacity, capacity);
    }

    @SuppressWarnings("unchecked")
    public GrowingArray(int capacity, int nextCapacity) {
        this.capacity = capacity;
        this.array = (T[]) new Object[capacity];
        this.nextCapacity = nextCapacity;
    }

    public T get(int index) {
        if (index < capacity) {
            return array[index];
        }
        if (isResized()) {
            return next.get(index - capacity);
        }
        throw new IndexOutOfBoundsException("index >= capacity");
    }

    private boolean isResized() {
        return resizeState.get() == RESIZED;
    }

    public void set(int index, T x) {
        if (index < capacity) {
            array[index] = x;
        } else if (isResized()) {
            next.set(index - capacity, x);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public int capacity() {
        return isResized() ? (next.capacity() + capacity) : capacity;
    }

    public void resize() {
        int resize = resizeState.get();
        if (resize == NOT_RESIZED) {
            if (resizeState.compareAndSet(NOT_RESIZED, RESIZING)) {
                next = new GrowingArray<>(nextCapacity, nextCapacity * 2);
                resizeState.set(RESIZED);
            }
        } else if (resize == RESIZED) {
            next.resize();
        }
    }

    @Override
    public String toString() {
        return "GrowingArray{" +
                "array=" + Arrays.toString(array) +
                ", capacity=" + capacity +
                ", nextCapacity=" + nextCapacity +
                ", resizeState=" + resizeState.get() +
                ", next=" + next +
                '}';
    }
}
