package in.xnnyygn.concurrent.artchp07;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class CompositeLock3Test {

    private static final int N_THREADS = 10;
    private int m = 0;

    @Test
    public void testSingleThread() {
        CompositeLock3 lock = new CompositeLock3(4);
        int n = 0;
        boolean locked;
        for (int i = 0; i < 10; i++) {
            locked = false;
            try {
                locked = lock.tryLock(1, TimeUnit.SECONDS);
                if (!locked) {
                    continue;
                }
                n++;
            } catch (InterruptedException ignore) {
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        }
        System.out.println(lock);
        assertEquals(10, n);
    }

    @Test
    public void testMultipleThreads() throws InterruptedException {
        CompositeLock3 lock = new CompositeLock3(3);
        Thread[] threads = makeThreads(lock);
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println(lock);
        assertEquals(10, m);
    }

    private Thread[] makeThreads(CompositeLock3 lock) {
        Thread[] threads = new Thread[N_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> runWithLock(lock));
        }
        return threads;
    }

    private void runWithLock(CompositeLock3 lock) {
        boolean locked = false;
        try {
            locked = lock.tryLock(10, TimeUnit.MILLISECONDS);
            if (!locked) {
                return;
            }
            m++;
        } catch (InterruptedException ignore) {
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}