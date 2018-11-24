package in.xnnyygn.concurrent;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

public class LockSupportTest {

    @Test
    @Ignore
    public void test() {
        LockSupport.unpark(Thread.currentThread());
        LockSupport.park(this);
    }

}
