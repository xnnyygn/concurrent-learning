package in.xnnyygn.concurrent.monitor;

import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleFuture2Test {

    @Test
    public void test() throws InterruptedException {
        SimpleFuture2<String> f = new SimpleFuture2<>();
        Thread t1 = new Thread(() -> setValue(f));
        Thread t2 = new Thread(() -> getValue(f));
        t2.start();
        t1.start();
        t1.join();
        t2.join();
        System.out.println(f.scheduler);
    }

    private void setValue(SimpleFuture2<String> f) {
        f.set("foo");
    }

    private void getValue(SimpleFuture2<String> f) {
        try {
            System.out.println(f.get());
        } catch (InterruptedException ignore) {
        }
    }
}