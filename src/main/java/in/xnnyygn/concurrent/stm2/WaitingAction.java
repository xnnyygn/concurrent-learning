package in.xnnyygn.concurrent.stm2;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

class WaitingAction implements Action {

    private static final int STATE_NEW = 0;
    private static final int STATE_PARKED = 1;
    private static final int STATE_WAKE_UP = 2;

    private final Thread thread;
    private final AtomicInteger state = new AtomicInteger(STATE_NEW);

    public WaitingAction(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void run(@Nonnull Transaction transaction) {
        int s = state.get();
        if (s == STATE_NEW && state.compareAndSet(STATE_NEW, STATE_WAKE_UP)) {
            return;
        }
        // state must be PARKED
        LockSupport.unpark(thread);
    }

    // call by current thread
    boolean tryWait() {
        if (!state.compareAndSet(STATE_NEW, STATE_PARKED)) {
            return false;
        }
        LockSupport.park(this);
        return true;
    }

}
