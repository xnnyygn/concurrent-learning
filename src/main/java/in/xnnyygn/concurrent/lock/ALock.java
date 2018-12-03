package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicInteger;

public class ALock {

    private final ThreadLocal<Integer> threadLocalSlotIndex = ThreadLocal.withInitial(() -> -1);
    private final AtomicInteger tail = new AtomicInteger(0);
    private final boolean[] slots;

    public ALock(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        slots = new boolean[capacity];
        slots[0] = true;
    }

    public void lock() {
        int slotIndex = tail.getAndIncrement() % slots.length;
        while (!slots[slotIndex]) {
        }
        threadLocalSlotIndex.set(slotIndex);
    }

    public void unlock() {
        int slotIndex = threadLocalSlotIndex.get();
        if (slotIndex < 0) {
            throw new IllegalStateException("attempt to unlock without lock");
        }
        slots[slotIndex] = false;
        slots[(slotIndex + 1) % slots.length] = true;
    }

}
