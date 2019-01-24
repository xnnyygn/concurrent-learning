package in.xnnyygn.concurrent.stm2;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class Transaction {

    public enum Status {
        ACTIVE, COMMITTED, ABORTING, ABORTED
    }

    private static final int THREAD_STATE_NORMAL = 0;
    private static final int THREAD_STATE_PARKED = 1;
    private static final int THREAD_STATE_WAKE_UP = 2;

    private final AtomicReference<Status> status = new AtomicReference<>();
    private final AtomicLong version = new AtomicLong();

    private final Map<TObject, TObjectLog> objectLogMap = new HashMap<>();

    private final AtomicReference<Action> nextAction = new AtomicReference<>();

    private final AtomicInteger threadState = new AtomicInteger(THREAD_STATE_NORMAL);
    private final Thread thread = Thread.currentThread();

    public void initialize() {
        status.lazySet(Status.ACTIVE);
        version.lazySet(globalVersion.get());
    }

    public void rollback() {
        initialize();
        undo();
        objectLogMap.clear();
        nextAction.lazySet(null);
    }

    private void undo() {
        Iterator<Runnable> iterator;
        for (TObjectLog objectLog : objectLogMap.values()) {
            iterator = objectLog.undoActionIterator();
            while (iterator.hasNext()) {
                iterator.next().run();
            }
        }
    }

    @Nonnull
    public Status getStatus() {
        return status.get();
    }

    public Thread getThread() {
        return thread;
    }

    public long getVersion(@Nonnull Object object) {
        throw new UnsupportedOperationException();
    }

    public long getVersion() {
        return version.get();
    }

    @SuppressWarnings("unchecked")
    public <T> T getLatestValue(@Nonnull TValue<T> object) {
        TObjectLog log = objectLogMap.get(object);
        return log != null ? (T) log.getNewValue() : null;
    }

    public <T> void putValue(@Nonnull TValue<T> object, T newValue) {
        getOrMakeObjectLog(object).setNewValue(newValue);
    }

    public void doWhenRollback(@Nonnull TObject object, @Nonnull Runnable action) {
        getOrMakeObjectLog(object).pushUndoAction(action);
    }

    @Nonnull
    private TObjectLog getOrMakeObjectLog(@Nonnull TObject object) {
        TObjectLog log = objectLogMap.get(object);
        if (log == null) {
            log = new TObjectLog();
            objectLogMap.put(object, log);
        }
        return log;
    }

    public void setNext(@Nonnull Action action) {
        this.nextAction.set(action);
    }

    public void commit() throws InterruptedException {
        List<TValue<?>> valueObjects = new ArrayList<>();
        try {
            // try lock all
            for (TObject object : objectLogMap.keySet()) {
                if (!(object instanceof TValue)) {
                    continue;
                }
                TValue<?> valueObject = (TValue<?>) object;
                if (!valueObject.tryLock(TIMEOUT, TimeUnit.MICROSECONDS)) {
                    throw new ConflictException();
                }
                valueObjects.add(valueObject);
            }

            // validate all
            for (TValue<?> valueObject : valueObjects) {
                if (!valueObject.validate(this)) {
                    throw new ConflictException();
                }
            }

            // apply change
            long version = globalVersion.incrementAndGet();
            TObjectLog log;
            for (TValue<?> valueObject : valueObjects) {
                log = objectLogMap.get(valueObject);
                if (log.hasNewValue()) {
                    valueObject.apply(version, log.getNewValue());
                }
            }

            // change status
            status.lazySet(Status.COMMITTED);
        } finally {
            for (TValue<?> valueObject : valueObjects) {
                valueObject.unlock();
            }
        }
    }

    public boolean abort() {
        // if aborted by self, rollback

        return status.compareAndSet(Status.ACTIVE, Status.ABORTING);
    }

    public boolean compareTo(@Nonnull Transaction transaction) {
        int c = Long.compare(version.get(), transaction.getVersion());
        if (c != 0) {
            return c > 0;
        }
        return Long.compare(thread.getId(), transaction.getThread().getId()) > 0;
    }

    public void await() {
        if (!threadState.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_PARKED)) {
            return;
        }
        LockSupport.park(this);
    }

    // call by other thread
    public void awake() {
        int s = threadState.get();
        if (s == THREAD_STATE_NORMAL && threadState.compareAndSet(THREAD_STATE_NORMAL, THREAD_STATE_WAKE_UP)) {
            return;
        }
        // s must be PARKED
        LockSupport.unpark(thread);
    }

    private static final ThreadLocal<Transaction> myTransaction = ThreadLocal.withInitial(Transaction::new);
    private static final AtomicLong globalVersion = new AtomicLong(0);

    private static final long TIMEOUT = 1000L;

    public static Transaction current() {
        return myTransaction.get();
    }

}
