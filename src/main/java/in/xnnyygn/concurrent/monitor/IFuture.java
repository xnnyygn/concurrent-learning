package in.xnnyygn.concurrent.monitor;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface IFuture<T> {

    T get();

    T get(long time, @Nonnull TimeUnit unit) throws TimeoutException;

    void set(T value);

}
