package in.xnnyygn.concurrent.stm1;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Reference<T> {

    private final AtomicReference<T> value;
    private final AtomicInteger version = new AtomicInteger(1);

    public Reference(T value) {
        this.value = new AtomicReference<>(value);
    }

    T get() {
        return value.get();
    }

    int getVersion() {
        return version.get();
    }

    void set(T value, int version) {
        this.value.lazySet(value);
        this.version.lazySet(version);
    }

    public T transactionalGet() {
        return TransactionContext.getLocal().read(this);
    }

    public void transactionalSet(T value) {
        TransactionContext.getLocal().write(this, value);
    }

}
