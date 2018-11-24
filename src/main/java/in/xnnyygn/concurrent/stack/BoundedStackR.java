package in.xnnyygn.concurrent.stack;

import java.util.concurrent.atomic.AtomicInteger;

public class BoundedStackR<T> {

    private static final int ROOM_PUSH = 0;
    private static final int ROOM_POP = 1;
    private final Rooms rooms = new Rooms(2);
    private final AtomicInteger atomicCursor = new AtomicInteger(0);
    private final ThreadLocal<T> threadLocalItemToPush = ThreadLocal.withInitial(() -> null);
    private T[] items;

    @SuppressWarnings("unchecked")
    public BoundedStackR(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        items = (T[]) new Object[capacity];
        rooms.setExitHandler(ROOM_PUSH, this::resizeAndPush);
    }

    @SuppressWarnings("unchecked")
    private void resizeAndPush(Void v) {
        T itemToPush = threadLocalItemToPush.get();
        if (itemToPush == null) {
            return;
        }

        int newCapacity = items.length * 2;
        T[] newItems = (T[]) new Object[newCapacity];
        System.arraycopy(items, 0, newItems, 0, items.length);
        items = newItems;
        items[atomicCursor.getAndIncrement()] = itemToPush;
        threadLocalItemToPush.set(null);
    }

    private void pushLater(T item) {
        threadLocalItemToPush.set(item);
    }

    public void push(T item) throws InterruptedException {
        int cursor;
        rooms.enter(ROOM_PUSH);
        try {
            cursor = atomicCursor.getAndIncrement();
            if (cursor >= items.length) {
                atomicCursor.getAndDecrement();
                pushLater(item);
                return;
            }
            items[cursor] = item;
        } finally {
            rooms.exit();
        }
    }

    public T pop(T item) throws InterruptedException {
        int cursor;
        rooms.enter(ROOM_POP);
        try {
            cursor = atomicCursor.decrementAndGet();
            if (cursor < 0) {
                atomicCursor.getAndIncrement();
                throw new IllegalStateException("empty stack");
            }
            return items[cursor];
        } finally {
            rooms.exit();
        }
    }
}
