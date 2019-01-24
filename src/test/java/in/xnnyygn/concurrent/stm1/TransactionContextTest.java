package in.xnnyygn.concurrent.stm1;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

public class TransactionContextTest {

    @Test
    public void test() throws InterruptedException {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Reference<Integer> account1 = new Reference<>(100);
        final Reference<Integer> account2 = new Reference<>(200);
        runParallel(4, () -> {
            int amount1 = account1.transactionalGet();
            int amount2 = account2.transactionalGet();
            account1.transactionalSet(amount1 - 10);
            account2.transactionalSet(amount2 + 10);
            return null;
        });
        assertEquals(60, account1.get().intValue());
        assertEquals(240, account2.get().intValue());
    }

    private void runParallel(int n, Callable<Void> function) throws InterruptedException {
        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(() -> TransactionContext.run(function));
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

}