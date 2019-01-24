package in.xnnyygn.concurrent.rwlock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class UnfairReadWriteTimedLock1Test {

    /**
     * W -> R -> R -> (R) -> (R) -> W
     * 1 -> 2 -> 3 ->  4  ->  5  -> 1
     */
    @Test
    public void test1() throws InterruptedException {
        ReadWriteLock readWriteLock = new UnfairReadWriteTimedLock2();
        final Lock readLock = readWriteLock.readLock();
        final Lock writeLock = readWriteLock.writeLock();
        Collection<Thread> threads = new ArrayList<>();
        threads.add(new Thread(() -> {
            writeLock.lock();
            try {
                log("writing");
                sleep(100L);
            } finally {
                writeLock.unlock();
            }

            sleep(50L);
            writeLock.lock();
            try {
                log("writing");
            } finally {
                writeLock.unlock();
            }
        }, "T1"));
        threads.add(new Thread(() -> {
            sleep(10L);
            readLock.lock();
            try {
                log("reading");
            } finally {
                readLock.unlock();
            }
        }, "T2"));
        threads.add(new Thread(() -> {
            sleep(10L);
            readLock.lock();
            try {
                log("reading");
            } finally {
                readLock.unlock();
            }
        }, "T3"));
        threads.add(new Thread(() -> {
            sleep(110L);
            readLock.lock();
            try {
                log("reading");
            } finally {
                readLock.unlock();
            }
        }, "T4"));
        threads.add(new Thread(() -> {
            sleep(110L);
            readLock.lock();
            try {
                log("reading");
            } finally {
                readLock.unlock();
            }
        }, "T5"));
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    private void log(String msg) {
        System.out.println(Thread.currentThread().getName() + " " + System.currentTimeMillis() + ":" + msg);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void test2() throws InterruptedException {
        ReadWriteLock readWriteLock = new UnfairReadWriteTimedLock2();
        final Lock readLock = readWriteLock.readLock();
        final Lock writeLock = readWriteLock.writeLock();
        Collection<Thread> threads = new ArrayList<>();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int n = 10;
        threads.add(new Thread(() -> {
            for (int i = 0; i < n; i++) {
                writeLock.lock();
                try {
                    log("writing");
                    sleep(random.nextInt(10));
                } finally {
                    writeLock.unlock();
                }
                sleep(random.nextInt(10));
            }
        }, "T1"));
        Runnable action = () -> {
            for (int i = 0; i < n; i++) {
                readLock.lock();
                try {
                    log("reading");
                    sleep(random.nextInt(10));
                } finally {
                    readLock.unlock();
                }
                sleep(random.nextInt(10));
            }
        };
        threads.add(new Thread(action, "T2"));
        threads.add(new Thread(action, "T3"));
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }
}