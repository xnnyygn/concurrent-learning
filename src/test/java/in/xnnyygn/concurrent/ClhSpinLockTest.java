package in.xnnyygn.concurrent;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClhSpinLockTest {

    private int n = 0;

    @Test
    public void test() throws InterruptedException {
        ClhSpinLock lock = new ClhSpinLock();
        Thread thread1 = new Thread(() -> run(lock), "thread1");
        Thread thread2 = new Thread(() -> run(lock), "thread2");
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }

    private void run(ClhSpinLock lock) {
        for (int i = 0; i < 1000; i++) {
            try {
                lock.lock();
                n++;
                System.out.println(Thread.currentThread().getName() + ":" + n);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

}