package in.xnnyygn.concurrent.stm2;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

class OneTimeAction implements Action {

    private final Action action;
    private final AtomicBoolean started = new AtomicBoolean(false);

    OneTimeAction(Action action) {
        this.action = action;
    }

    @Override
    public void run(@Nonnull Transaction transaction) {
        if (tryStart()) {
            action.run(transaction);
        }
    }

    boolean tryStart() {
        return !started.get() && started.compareAndSet(false, true);
    }

}
