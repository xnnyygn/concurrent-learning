package in.xnnyygn.concurrent.dequeue;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicStampedReference;

// Anish Arora, Robert Blumofe, and Greg Plaxton [15]
@SuppressWarnings("Duplicates")
public class BoundedDeque<T> implements DoubleEndedQueue<T> {
    private final T[] elements;
    private volatile int bottom = 0;
    private final AtomicStampedReference<Integer> top = new AtomicStampedReference<>(0, 1);

    @SuppressWarnings("unchecked")
    public BoundedDeque(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        this.elements = (T[]) new Object[capacity];
    }

    @Override
    public void pushBottom(@Nonnull T e) {
        Preconditions.checkNotNull(e);
        int b = bottom;
        elements[b] = e;
        bottom = b + 1;
    }

    /*
    @Override
    @Nullable
    public T popTop() {
        int[] stampHolder = new int[1];
        int t;
        while (true) {
            t = top.get(stampHolder);
            if (t == bottom) {
                // no element
                return null;
            }
            if (top.compareAndSet(t, t + 1, stampHolder[0], stampHolder[0] + 1)) {
                return removeElement(t);
            }
        }
    }

    @Override
    @Nullable
    public T popBottom() {
        int[] stampHolder = new int[1];
        int t;
        int b;
        while (true) {
            t = top.get(stampHolder);
            b = bottom;
            if (t == b) {
                // no element
                return null;
            }
            if (t == b - 1) {
                // last element
                if (top.compareAndSet(t, t + 1, stampHolder[0], stampHolder[0] + 1)) {
                    return removeElement(t);
                }
                continue;
            }
            // CAS top?
            bottom = b - 1;
            return removeElement(b - 1);
        }
    }
    */

    // popTop changes top only
    @Override
    @Nullable
    public T popTop() {
        int[] stampHolder = new int[1];
        int t = top.get(stampHolder);
        // bottom may be reset to 0
        if (t >= bottom) {
            return null;
        }
        T e = elements[t];
        if (top.compareAndSet(t, t + 1, stampHolder[0], stampHolder[0] + 1)) {
            return e;
        }
        // top has been changed by popBottom, which means there's no element
        return null;
    }

    // only popBottom/pushBottom can change bottom
    @Override
    @Nullable
    public T popBottom() {
        // change bottom firstly
        int oldBottom = bottom;
        if (oldBottom == 0) {
            return null;
        }
        // oldBottom > 0
        int b = oldBottom - 1;
        bottom = b;

        T e = elements[b];
        int[] stampHolder = new int[1];
        int t = top.get(stampHolder);
        if (t < b) {
            /*
             * popTop
             *   top < bottom?
             *   elements[top]
             *   increase top
             *   return element
             *
             * popBottom
             *   decrease bottom
             *   elements[bottom']
             *   top? < bottom'
             *   return element
             *
             * top >= bottom'
             *
             * top < bottom', top + 1 < bottom'
             */
            return e;
        }
        if (t == b) {
            /*
             * popTop
             *   top? < bottom?
             *   elements[top?]
             *   increase top and return element
             *
             * element -> top
             *
             * popBottom
             *   decrease bottom
             *   elements[bottom']
             *   top? == bottom'
             *   increase top and return element
             *
             * element -> bottom'
             *
             * if top changed, failed
             * CAS successfully, get the chance to return element
             */
            bottom = 0;
            if (top.compareAndSet(t, 0, stampHolder[0], stampHolder[0] + 1)) {
                return e;
            }
        }
        /*
         * 1. t > b top > bottom, popTop increase, bottom decrease
         * 2. element taken by popTop -> no element
         */
        top.set(0, stampHolder[0] + 1);
        return null;
    }

    public boolean isEmpty() {
        int t = top.getReference();
        return t < bottom;
    }

//    private T removeElement(int index) {
//        T x = elements[index];
//        elements[index] = null;
//        return x;
//    }
}
