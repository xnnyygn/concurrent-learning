package in.xnnyygn.concurrent.lock;

import java.util.Random;

/**
 * one-off class
 */
public class Backoff {

    private final Random random = new Random();
    private final int minDelay;
    private final int maxDelay;
    private int limit;

    public Backoff(int minDelay, int maxDelay) {
        if (minDelay <= 0 || minDelay > maxDelay) {
            throw new IllegalArgumentException("min delay <= 0 or min delay > max delay");
        }
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
        limit = minDelay;
    }

    public void backoff() throws InterruptedException {
        int delay = random.nextInt(limit);
        limit = Math.min(maxDelay, 2 * limit);
        Thread.sleep(delay);
    }
}
