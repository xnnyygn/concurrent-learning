package in.xnnyygn.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

// double check
public class RunOnce<V> {

    private final Callable<V> callable;
    private V result;
    private ExecutionException executionException;
    private volatile boolean done = false;

    public RunOnce(Callable<V> callable) {
        this.callable = callable;
    }

    public V get() throws ExecutionException {
        if (done) {
            return doGet();
        }
        synchronized (this) {
            if (done) {
                return doGet();
            }
            try {
                result = callable.call();
                return result;
            } catch (Exception e) {
                executionException = new ExecutionException(e);
                throw executionException;
            } finally {
                done = true;
            }
        }
    }

    private V doGet() throws ExecutionException {
        if (executionException != null) {
            throw executionException;
        }
        return result;
    }

}
