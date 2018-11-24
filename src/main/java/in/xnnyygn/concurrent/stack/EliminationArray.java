package in.xnnyygn.concurrent.stack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EliminationArray<T> {

    private static final int DURATION = 100;
    private final LockFreeExchanger<T>[] exchangers;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @SuppressWarnings("unchecked")
    public EliminationArray(int capacity) {
        exchangers = (LockFreeExchanger<T>[]) new LockFreeExchanger[capacity];
        for (int i = 0; i < capacity; i++) {
            exchangers[i] = new LockFreeExchanger<>();
        }
    }

    public T visit(T value, int range) throws TimeoutException {
        if (range > exchangers.length) {
            throw new IllegalArgumentException("range > capacity");
        }
        int slot = random.nextInt(range);
        return exchangers[slot].exchange2(value, DURATION, TimeUnit.MILLISECONDS);
    }
}
