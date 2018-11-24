package in.xnnyygn.concurrent.artchp07;

import org.junit.Test;

import static org.junit.Assert.*;

public class McsLockTest {

    private static final int N_THREADS = 10;
    private volatile int n = 0;

    @Test
    public void test() throws InterruptedException {
        final McsLock lock = new McsLock();
        Thread[] threads = new Thread[N_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    n++;
                } finally {
                    lock.unlock();
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(10, n);
    }
}