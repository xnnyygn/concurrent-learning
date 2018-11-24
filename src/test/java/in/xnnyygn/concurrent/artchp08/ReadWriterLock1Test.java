package in.xnnyygn.concurrent.artchp08;

import org.junit.Test;

import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;

public class ReadWriterLock1Test {

    private int n = 0;

    @Test
    public void test() throws InterruptedException {
        ReadWriterLock1 lock = new ReadWriterLock1();
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Lock writeLock = lock.writeLock();
                writeLock.lock();
                try {
                    n++;
                } finally {
                    writeLock.unlock();
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

    @Test
    public void test2() {
        ReadWriterLock1 lock = new ReadWriterLock1();
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        writeLock.lock();
    }
}