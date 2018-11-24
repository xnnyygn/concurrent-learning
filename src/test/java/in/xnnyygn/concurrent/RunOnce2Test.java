package in.xnnyygn.concurrent;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class RunOnce2Test {

    @Test
    @Ignore
    public void test() throws InterruptedException, ExecutionException {
        RunOnce2<Integer> result = new RunOnce2<>(() -> {
            System.out.println("run");
            return 1 + 2;
        });
        Thread thread1 = new Thread(() -> run(result), "thread1");
        Thread thread2 = new Thread(() -> run(result), "thread2");
        thread1.start();
        thread2.start();
        run(result);
        thread1.join();
        thread2.join();
    }

    private <T> void run(RunOnce2<T> result) {
        try {
            System.out.println(Thread.currentThread().getName() + ": " + result.get());
        } catch (ExecutionException ignored) {
        }
    }
}