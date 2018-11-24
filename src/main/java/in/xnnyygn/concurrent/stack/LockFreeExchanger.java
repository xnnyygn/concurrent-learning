package in.xnnyygn.concurrent.stack;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class LockFreeExchanger<T> {

    private static final int STATUS_EMPTY = 0;
    private static final int STATUS_SET1 = 1;
    private static final int STATUS_SET2 = 2;
    private final AtomicStampedReference<T> valueAndStatus = new AtomicStampedReference<>(null, STATUS_EMPTY);

    public T exchange(T thisValue, long timeout, TimeUnit unit) throws TimeoutException {
        long start = System.currentTimeMillis();
        long threshold = unit.toMillis(timeout);
        int[] statusHolder = new int[1];
        int status;
        T thatValue;
        boolean valueSet = false;
        // wait -(thread A)-> set 1 -(thread B)-> set 2 -(thread A)-> wait
        // for thread A, valueSet will be true
        // for thread B, valueSet will be false
        while (true) {
            thatValue = valueAndStatus.get(statusHolder);
            status = statusHolder[0];
            if (status == STATUS_EMPTY &&
                    valueAndStatus.compareAndSet(null, thisValue, STATUS_EMPTY, STATUS_SET1)) {
                valueSet = true;
                // if failed to CAS, value set by another thread
            } else if (status == STATUS_SET1 && !valueSet &&
                    valueAndStatus.compareAndSet(thatValue, thisValue, STATUS_SET1, STATUS_SET2)) {
                return thatValue;
            } else if (status == STATUS_SET2 && valueSet) {
                valueAndStatus.set(null, STATUS_EMPTY);
                return thatValue;
            }
            if (System.currentTimeMillis() - start > threshold) {
                // thread A failed to wait for thread B
                if (valueSet) {
                    if (valueAndStatus.compareAndSet(thisValue, null, STATUS_SET1, STATUS_EMPTY)) {
                        throw new TimeoutException();
                    }
                    // thread B appeared
                    thatValue = valueAndStatus.getReference();
                    valueAndStatus.set(null, STATUS_EMPTY);
                    return thatValue;
                }
                throw new TimeoutException();
            }
        }
    }

    public T exchange2(T thisValue, long timeout, TimeUnit unit) throws TimeoutException {
        long start = System.currentTimeMillis();
        long threshold = unit.toMillis(timeout);
        int[] statusHolder = new int[1];
        int status;
        T thatValue;
        while (true) {
            thatValue = valueAndStatus.get(statusHolder);
            status = statusHolder[0];
            if (status == STATUS_EMPTY) {
                if (valueAndStatus.compareAndSet(null, thisValue, STATUS_EMPTY, STATUS_SET1)) {
                    // section thread A
                    // wait for status 2
                    while (System.currentTimeMillis() - start < threshold) {
                        thatValue = valueAndStatus.get(statusHolder);
                        if (statusHolder[0] == STATUS_SET2) {
                            valueAndStatus.set(null, STATUS_EMPTY);
                            return thatValue;
                        }
                    }
                    // failed to wait for thread B
                    if (!valueAndStatus.compareAndSet(thisValue, null, STATUS_SET1, STATUS_EMPTY)) {
                        // thread B appeared when timeout
                        thatValue = valueAndStatus.get(statusHolder);
                        // status must be SET2
                        valueAndStatus.set(null, STATUS_EMPTY);
                        return thatValue;
                    }
                    throw new TimeoutException();
                }
                // set by another thread
            } else if (status == STATUS_SET1) {
                // section thread B
                if (valueAndStatus.compareAndSet(thatValue, thisValue, STATUS_SET1, STATUS_SET2)) {
                    return thatValue;
                }
                // exchanged by another thread
            }
            if (System.currentTimeMillis() - start > threshold) {
                throw new TimeoutException();
            }
        }
    }

}
