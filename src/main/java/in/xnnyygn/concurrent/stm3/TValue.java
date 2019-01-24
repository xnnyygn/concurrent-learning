package in.xnnyygn.concurrent.stm3;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TValue<T> {

    private T value;
    private volatile long version = 1L;
    private final ReentrantLock lock = new ReentrantLock();

    public TValue(T value) {
        this.value = value;
    }

    public T get(@Nonnull Transaction transaction) {
        Preconditions.checkNotNull(transaction);
        switch (transaction.getStatus()) {
            case COMMITTED:
                // out of transaction
                return value;
            case ABORTED:
                throw new AbortedException();
            case ACTIVE:
                T value = doGet(transaction);
                checkIfConflict(transaction);
                return value;
            default:
                throw new IllegalStateException("unexpected state " + transaction.getStatus());
        }
    }

    private T doGet(@Nonnull Transaction transaction) {
        // value should not be null
        T writtenValue = transaction.getWrittenValue(this);
        if (writtenValue != null) {
            return writtenValue;
        }
        transaction.addValue(this);
        return value;
    }

    private void checkIfConflict(@Nonnull Transaction transaction) {
        if (lock.isLocked() || version > transaction.getVersion()) {
            throw new ConflictException("conflict with other transaction");
        }
    }

    public boolean validate(long version) {
        if (this.version > version) {
            return false;
        }
        if (lock.isLocked() && !lock.isHeldByCurrentThread()) {
            return false;
        }
        return true;
    }

    public void set(@Nonnull Transaction transaction, @Nonnull T newValue) {
        Preconditions.checkNotNull(transaction);
        Preconditions.checkNotNull(newValue);
        switch (transaction.getStatus()) {
            case COMMITTED:
                throw new UnsupportedOperationException("operation should be in a transaction");
            case ABORTED:
                throw new AbortedException();
            case ACTIVE:
                transaction.writeValue(this, newValue);
                checkIfConflict(transaction);
                break;
            default:
                throw new IllegalStateException("unexpected state " + transaction.getStatus());
        }
    }

    @SuppressWarnings("unchecked")
    void apply(@Nonnull Object newValueObject, long version) {
        Preconditions.checkArgument(version > this.version, "new version must be larger than current version");
        this.value = (T) newValueObject;
        this.version = version;
    }

    boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        return lock.tryLock(time, unit);
    }

    void unlock() {
        lock.unlock();
    }

    @Override
    public String toString() {
        return "TValue{" +
                "value=" + value +
                ", version=" + version +
                '}';
    }

}
