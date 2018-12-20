package in.xnnyygn.concurrent.queue;

import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class LinkedQueue2Test {

    private static final int NUM_THREADS = 4;
    private static final int N_ITEMS = 10;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Test
    public void test() throws InterruptedException {
        final LinkedQueue2<Integer> queue = new LinkedQueue2<>();
        Thread[] producers = new Thread[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            producers[i] = new Thread(() -> produce(queue));
        }
        Thread consumer = new Thread(() -> consume(queue));
        for (Thread t : producers) {
            t.start();
        }
        consumer.start();
        for (Thread t : producers) {
            t.join();
        }
        consumer.join();
    }

    private void consume(LinkedQueue2<Integer> queue) {
        int i = 0;
        Integer value;
        while (i < NUM_THREADS * N_ITEMS) {
            value = queue.peek();
            if (value == null) {
                Thread.yield();
            } else {
                value = queue.poll();
                System.out.println(value);
                i++;
            }
        }
    }

    private void produce(LinkedQueue2<Integer> queue) {
        try {
            for (int i = 0; i < N_ITEMS; i++) {
                Thread.sleep(random.nextInt(1000));
                queue.offer(i);
            }
        } catch (InterruptedException ignore) {
        }
    }
}