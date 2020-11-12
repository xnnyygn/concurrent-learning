package in.xnnyygn.concurrent.dequeue;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicStampedReference;

@SuppressWarnings("Duplicates")
public class UnboundedDeque<T> implements DoubleEndedQueue<T> {

    private static final int LOG_CAPACITY = 4;
    private volatile int bottom = 0;
    private final AtomicStampedReference<Integer> top = new AtomicStampedReference<>(0, 1);
    private CircularArray<T> elements = new CircularArray<>(LOG_CAPACITY);

    @Override
    public void pushBottom(T x) {
        Preconditions.checkNotNull(x);
        int b = bottom;
        if (b > elements.capacity()) {
            elements = elements.resize(top.getReference(), b);
        }
        elements.set(b, x);
        bottom = b + 1;
    }

    @Override
    public T popTop() {
        int[] stampHolder = new int[1];
        int t = top.get(stampHolder);
        if (t >= bottom) {
            return null;
        }
        T e = elements.get(t);
        if (top.compareAndSet(t, t + 1, stampHolder[0], stampHolder[0] + 1)) {
            return e;
        }
        return null;
    }

    @Override
    public T popBottom() {
        int oldBottom = bottom;
        if (oldBottom == 0) {
            return null;
        }
        int b = oldBottom - 1;
        bottom = b;
        T e = elements.get(b);
        int[] stampHolder = new int[1];
        int t = top.get(stampHolder);
        if (t < b) {
            return e;
        }
        if (t == b) {
            bottom = 0;
            if (top.compareAndSet(t, 0, stampHolder[0], stampHolder[0] + 1)) {
                return e;
            }
        }
        top.set(0, stampHolder[0] + 1);
        return null;
    }

    @Override
    public boolean isEmpty() {
        int t = top.getReference();
        return t < bottom;
    }

    private static class CircularArray<T> {
        final int logCapacity;
        final T[] elements;

        @SuppressWarnings("unchecked")
        CircularArray(int logCapacity) {
            this.logCapacity = logCapacity;
            this.elements = (T[]) new Object[1 << logCapacity];
        }

        T get(int i) {
            return elements[i % capacity()];
        }

        int capacity() {
            return 1 << logCapacity;
        }

        void set(int i, T e) {
            elements[i % capacity()] = e;
        }

        @Nonnull
        CircularArray<T> resize(int top, int bottom) {
            CircularArray<T> newArray = new CircularArray<>(logCapacity + 1);
            System.arraycopy(elements, top, newArray.elements, 0, bottom - top);
            return newArray;
        }
    }
}
