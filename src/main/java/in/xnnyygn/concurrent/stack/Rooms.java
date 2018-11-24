package in.xnnyygn.concurrent.stack;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Rooms {
    private final ThreadLocal<Integer> threadLocalEntered = ThreadLocal.withInitial(() -> -1);
    private final Consumer<Void>[] handlers;
    private final int capacity;
    private volatile Thread lastThread = null;
    private final AtomicInteger atomicRoomId = new AtomicInteger(-1);

    @SuppressWarnings("unchecked")
    public Rooms(int m) {
        capacity = m;
        handlers = (Consumer<Void>[]) new Consumer[m];
    }

    public void enter(int i) throws InterruptedException {
        if (i < 0 || i >= capacity) {
            throw new IllegalArgumentException("illegal room id");
        }

        int roomId;
        do {
            roomId = atomicRoomId.get();
        } while (!(roomId == i || (roomId == -1 && atomicRoomId.compareAndSet(-1, i))));

        threadLocalEntered.set(roomId);
        lastThread = Thread.currentThread();
    }

    public boolean exit() {
        if (threadLocalEntered.get() == -1) {
            throw new IllegalStateException("not entered");
        }

        int roomId = atomicRoomId.get();
        if (roomId == -1) {
            throw new IllegalStateException("no thread in room");
        }

        boolean result = false;
        if (lastThread == Thread.currentThread()) {
            Consumer<Void> handler = handlers[roomId];
            if (handler != null) {
                handler.accept(null);
            }
            lastThread = null;
            result = true;
        }
        atomicRoomId.set(-1);
        return result;
    }

    public void setExitHandler(int i, Consumer<Void> h) {
        handlers[i] = h;
    }
}
