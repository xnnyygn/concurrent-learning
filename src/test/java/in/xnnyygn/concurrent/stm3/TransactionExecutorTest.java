package in.xnnyygn.concurrent.stm3;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionExecutorTest {

    private final TransactionExecutor executor = new TransactionExecutor();

    @Test
    public void test() throws InterruptedException {
        TValue<Integer> valueA = new TValue<>(10);
        TValue<Integer> valueB = new TValue<>(10);
        Thread[] threads = new Thread[2];
        threads[0] = run1(valueA, valueB);
        threads[1] = run2(valueA, valueB);
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("a: " + valueA);
        System.out.println("b: " + valueB);
    }

    private Thread run1(TValue<Integer> valueA, TValue<Integer> valueB) {
        return new Thread(() -> {
            try {
                executor.execute(transaction -> {
                    int a = valueA.get(transaction);
                    valueB.set(transaction, a * 2);
                });
            } catch (InterruptedException ignored) {
            }
        });
    }

    private Thread run2(TValue<Integer> valueA, TValue<Integer> valueB) {
        return new Thread(() -> {
            try {
                executor.execute(transaction -> {
                    int b = valueB.get(transaction);
                    valueA.set(transaction, b * 2);
                });
            } catch (InterruptedException ignored) {
            }
        });
    }

}