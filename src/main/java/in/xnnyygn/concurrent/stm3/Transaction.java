package in.xnnyygn.concurrent.stm3;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("WeakerAccess")
public class Transaction {

    public enum Status {
        ACTIVE, ABORTED, COMMITTED
    }

    private static final long TIMEOUT = 1000L;

    private static final AtomicLong globalVersion = new AtomicLong(1L);

    private Status status = Status.ACTIVE;
    private final long version;
    private final Set<TValue<?>> readSet = new HashSet<>();
    private final Map<TValue<?>, Object> writeMap = new HashMap<>();

    public Transaction() {
        version = globalVersion.get();
    }

    public Status getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    @SuppressWarnings("unchecked")
    public <T> T getWrittenValue(@Nonnull TValue<T> object) {
        return (T) writeMap.get(object);
    }

    public <T> void addValue(@Nonnull TValue<T> object) {
        readSet.add(object);
    }

    public <T> void writeValue(@Nonnull TValue<T> object, @Nonnull T newValue) {
        writeMap.put(object, newValue);
    }

    public void rollback() {
        readSet.clear();
        writeMap.clear();
    }

    public void abort() {
        rollback();
        status = Status.ABORTED;
    }

    public void commit() throws InterruptedException {
        List<TValue<?>> lockedValues = new ArrayList<>();
        try {
            for (TValue<?> writtenValue : writeMap.keySet()) {
                if (!writtenValue.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    throw new ConflictException("failed to lock");
                }
                lockedValues.add(writtenValue);
            }
            for (TValue<?> readValue : readSet) {
                if (!readValue.validate(version)) {
                    throw new ConflictException("validation failed");
                }
            }
            long newVersion = globalVersion.incrementAndGet();
            for (TValue<?> writtenValue : writeMap.keySet()) {
                writtenValue.apply(writeMap.get(writtenValue), newVersion);
            }
            readSet.clear();
            writeMap.clear();
            status = Status.COMMITTED;
        } finally {
            for (TValue<?> writtenValue : lockedValues) {
                writtenValue.unlock();
            }
        }
    }

}
