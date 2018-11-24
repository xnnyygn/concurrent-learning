package in.xnnyygn.concurrent.artchp07;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClhLockTest {

    private static final int N_THREADS = 10;
    private volatile int n = 0;

    @Test
    public void test() throws InterruptedException {
        ClhLock lock = new ClhLock();
        Thread[] threads = new Thread[N_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> runWithLock(lock));
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(10, n);
    }

    @Test
    public void test2() {
        ClhLock lock = new ClhLock();
        lock.lock();
        lock.unlock();
        lock.lock();
        lock.unlock();
    }

    private void runWithLock(ClhLock lock) {
        lock.lock();
        try {
            n++;
        } finally {
            lock.unlock();
        }
    }
}