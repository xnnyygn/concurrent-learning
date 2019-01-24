package in.xnnyygn.concurrent.stm2;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

/**
 * Transactional value.
 *
 * @param <T> type of value
 */
public class TValue<T> extends AbstractTObject {

    private T value;

    /**
     * Create with initial value.
     *
     * @param value value
     */
    public TValue(T value) {
        this.value = value;
    }

    /**
     * Get value within default transaction manager.
     *
     * @return latest value
     * @see #get(Transaction)
     */
    public T get() {
        return get(Transaction.current());
    }

    /**
     * Get value.
     *
     * @param transaction current transaction
     * @return latest value
     */
    public T get(Transaction transaction) {
        Preconditions.checkNotNull(transaction);
        switch (transaction.getStatus()) {
            case COMMITTED:
                // out of transaction
                return value;
            case ABORTED:
                throw new IllegalStateException("transaction aborted");
            case ACTIVE:
                T value = doGet(transaction);
                checkIfConflict(transaction);
                return value;
            default:
                throw new IllegalStateException("unexpected status " + transaction.getStatus());
        }
    }

    boolean validate(@Nonnull Transaction transaction) {
        return (!lock.isLocked() || lock.isHeldByCurrentThread()) &&
                transaction.getVersion(this) >= version;
    }

    private void checkIfConflict(@Nonnull Transaction transaction) {
        if (!validate(transaction)) {
            throw new ConflictException();
        }
    }

    private T doGet(@Nonnull Transaction transaction) {
        T latestValue = transaction.getLatestValue(this);
        return latestValue != null ? latestValue : value;
    }

    /**
     * Set value within default transaction manager.
     *
     * @param newValue new value
     * @see #set(Transaction, Object)
     */
    public void set(T newValue) {
        set(Transaction.current(), newValue);
    }

    /**
     * Set value.
     *
     * @param transaction current transaction
     * @param newValue    new value
     */
    public void set(Transaction transaction, T newValue) {
        Preconditions.checkNotNull(transaction);
        switch (transaction.getStatus()) {
            case COMMITTED:
                throw new UnsupportedOperationException("value cannot be changed outside of transaction");
            case ABORTED:
                throw new IllegalStateException("transaction aborted");
            case ACTIVE:
                doSet(transaction, newValue);
                checkIfConflict(transaction);
                break;
            default:
                throw new IllegalStateException("unexpected state " + transaction.getStatus());
        }
    }

    private void doSet(@Nonnull Transaction transaction, T newValue) {
        transaction.putValue(this, newValue);
    }

    @SuppressWarnings("unchecked")
    void apply(long version, Object newValue) {
        // assert version > super.version;
        super.version = version;
        value = (T) newValue;
    }

}
