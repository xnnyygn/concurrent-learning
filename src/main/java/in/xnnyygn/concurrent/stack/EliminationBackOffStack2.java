package in.xnnyygn.concurrent.stack;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationBackOffStack2<T> {

    private enum Operation {
        PUSH, POP;
    }

    private static class Value<T> {
        private final Operation operation;
        private final T value;

        Value(Operation operation, T value) {
            this.operation = operation;
            this.value = value;
        }
    }

    private static <T> Value<T> pushOp(T value) {
        return new Value<>(Operation.PUSH, value);
    }

    private static Value<?> POP_OP = new Value<>(Operation.POP, null);

    @SuppressWarnings("unchecked")
    private static <T> Value<T> popOp() {
        return (Value<T>) POP_OP;
    }

    private static class Exchanger<T> {
        private static final int STATUS_EMPTY = 0;
        private static final int STATUS_PUSH = 1;
        private static final int STATUS_POP = 2;
        private final AtomicStampedReference<T> valueAndStatus = new AtomicStampedReference<>(null, STATUS_EMPTY);

        public void push(T thisValue, long timeout, TimeUnit unit) throws TimeoutException {
            long start = System.currentTimeMillis();
            long threshold = unit.toMillis(timeout);
            int[] statusHolder = new int[1];
            int status;
            T thatValue;
            while (true) {
                status = valueAndStatus.getStamp();
                if (status == STATUS_EMPTY) {
                    if (valueAndStatus.compareAndSet(null, thisValue, STATUS_EMPTY, STATUS_PUSH)) {
                        // wait for pop
                        do {
                            status = valueAndStatus.getStamp();
                            if (status == STATUS_POP) {
                                valueAndStatus.set(null, STATUS_EMPTY);
                                return;
                            }
                        } while (System.currentTimeMillis() - start < threshold);
                        if (valueAndStatus.compareAndSet(thisValue, null, STATUS_PUSH, STATUS_EMPTY)) {
                            throw new TimeoutException();
                        }
                        // pop thread has shown up
                        valueAndStatus.set(null, STATUS_EMPTY);
                        return;
                    }
                } else if(status == STATUS_POP) {
                    if(valueAndStatus.compareAndSet(null, thisValue, STATUS_POP, STATUS_PUSH)) {
                        // TODO wait for ?
                        // TODO clean by pop
                    }
                }
            }
        }
    }
}
