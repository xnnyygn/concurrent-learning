package in.xnnyygn.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class RunOnce2<V> {

    private static final int STATE_DONE = 2;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_INITIAL = 0;
    private final AtomicInteger atomicState = new AtomicInteger(STATE_INITIAL);
    private final ConcurrentStack<Thread> waiters = new ConcurrentStack<>();
    private final Callable<V> callable;
    private V result;
    private ExecutionException executionException;

    public RunOnce2(Callable<V> callable) {
        this.callable = callable;
    }

    public V get() throws ExecutionException {
        int state = atomicState.get();
        if (state == STATE_DONE) {
            return doGet();
        }
        if (state == STATE_INITIAL && atomicState.compareAndSet(STATE_INITIAL, STATE_RUNNING)) {
            try {
                result = callable.call();
            } catch (Exception e) {
                executionException = new ExecutionException(e);
            }
            atomicState.set(STATE_DONE);
            Thread thread;
            while ((thread = waiters.pop()) != null) {
                LockSupport.unpark(thread);
            }
            return doGet();
        }
        waiters.push(Thread.currentThread());
        while (atomicState.get() != STATE_DONE) {
            LockSupport.park(this);
        }
        return doGet();
    }

    private V doGet() throws ExecutionException {
        if (executionException != null) {
            throw executionException;
        }
        return result;
    }

}
