package in.xnnyygn.concurrent;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ParkTest {

    @Test
    @Ignore
    public void test() throws InterruptedException {
        LockSupport.parkNanos(this, TimeUnit.SECONDS.toNanos(1));
        LockSupport.unpark(Thread.currentThread());
        Thread.sleep(5000);
        LockSupport.park(this);
    }

    @Test
    public void test2() throws InterruptedException {
        Thread t = new Thread(() -> {
            System.out.println(Thread.currentThread().isInterrupted());
            LockSupport.park(this);
            System.out.println(Thread.currentThread().isInterrupted());
        });
        t.start();
        Thread.sleep(1000);
        t.interrupt();
        t.join();
    }
}
