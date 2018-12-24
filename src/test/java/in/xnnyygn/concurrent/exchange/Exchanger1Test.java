package in.xnnyygn.concurrent.exchange;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class Exchanger1Test {

    @Test
    public void testExchangeNormal() throws InterruptedException {
        final Exchanger1<Integer> exchanger = new Exchanger1<>();
        Thread t1 = new Thread(() -> {
            try {
                Integer result = exchanger.exchange(1, 1, TimeUnit.SECONDS);
                assertEquals(Integer.valueOf(2), result);
            } catch (TimeoutException e) {
                fail("cannot exchange, timeout");
            } catch (InterruptedException e) {
                fail("cannot exchange, interrupted");
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                Integer result = exchanger.exchange(2, 1, TimeUnit.SECONDS);
                assertEquals(Integer.valueOf(1), result);
            } catch (TimeoutException e) {
                fail("cannot exchange, timeout");
            } catch (InterruptedException e) {
                fail("cannot exchange, interrupted");
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    @Test
    public void testExchangeTimeout() throws InterruptedException {
        final Exchanger1<Integer> exchanger = new Exchanger1<>();
        Thread t1 = new Thread(() -> {
            try {
                exchanger.exchange(1, 1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                assertTrue(true);
            } catch (InterruptedException e) {
                fail("cannot exchange, interrupted");
            }
        });
        t1.start();
        t1.join();
    }

    @Test
    public void testExchangeInterrupted() throws InterruptedException {
        final Exchanger1<Integer> exchanger = new Exchanger1<>();
        Thread t1 = new Thread(() -> {
            try {
                exchanger.exchange(1, 1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("cannot exchange, timeout");
            } catch (InterruptedException e) {
                assertTrue(true);
            }
        });
        t1.start();
        Thread.sleep(500L);
        t1.interrupt();
        t1.join();
    }

}