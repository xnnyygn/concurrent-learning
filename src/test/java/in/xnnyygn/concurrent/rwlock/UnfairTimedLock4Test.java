package in.xnnyygn.concurrent.rwlock;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static org.junit.Assert.*;

public class UnfairTimedLock4Test {

    @Test
    public void testCondition() throws InterruptedException {
        final Queue<Integer> queue = new Queue<>();
        final int n = 10;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < n; i++) {
                    queue.enqueue(i);
                    Thread.sleep(random.nextInt(500));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                for (int i = 0; i < n; i++) {
                    System.out.println(queue.dequeue());
                    Thread.sleep(random.nextInt(500));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    private static class Queue<T> {
        final Lock lock = new UnfairTimedLock4();
        final Condition notEmpty;

        T[] buffer;
        int head = 0;
        int tail = 0;

        @SuppressWarnings("unchecked")
        Queue() {
            notEmpty = lock.newCondition();
            buffer = (T[]) new Object[16];
        }

        void enqueue(T x) {
            lock.lock();
            try {
                buffer[head++] = x;
                notEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        }

        T dequeue() throws InterruptedException {
            int signaledTimes = 0;
            lock.lock();
            try {
                while (head == tail) {
                    notEmpty.await();
                    signaledTimes++;
                }
                System.out.println("signaled times: " + signaledTimes);
                return buffer[tail++];
            } finally {
                lock.unlock();
            }
        }
    }

}