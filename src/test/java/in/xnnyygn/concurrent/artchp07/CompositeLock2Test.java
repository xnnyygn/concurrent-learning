package in.xnnyygn.concurrent.artchp07;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class CompositeLock2Test {

    private static final int N_THREADS = 10;
    private int n;

    @Test
    public void test() throws InterruptedException {
        CompositeLock2 lock = new CompositeLock2(3);
        Thread[] threads = makeThreads(lock);
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println(lock);
        assertEquals(10, n);
    }

    private Thread[] makeThreads(CompositeLock2 lock) {
        Thread[] threads = new Thread[N_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> runWithLock(lock));
        }
        return threads;
    }

    private void runWithLock(CompositeLock2 lock) {
        boolean locked = false;
        try {
            locked = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if (!locked) {
                return;
            }
            n++;
        } catch (InterruptedException ignore) {
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

}