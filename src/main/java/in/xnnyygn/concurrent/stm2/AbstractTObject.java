package in.xnnyygn.concurrent.stm2;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractTObject implements TObject {

    protected final ReentrantLock lock = new ReentrantLock();
    protected long version = 0L;

    protected boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(time, unit);
    }

    protected void unlock() {
        lock.unlock();
    }

}
