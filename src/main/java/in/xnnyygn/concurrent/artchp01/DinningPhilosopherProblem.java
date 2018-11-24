package in.xnnyygn.concurrent.artchp01;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 3. Amend your program so that no philosopher ever starves.
 * 4. Write a program to provide a starvation-free solution for any number of philosophers n.
 */
public class DinningPhilosopherProblem {

    public static void main(String[] args) throws Exception {
        Chopstick chopstick1 = new Chopstick(1);
        Chopstick chopstick2 = new Chopstick(2);
        Chopstick chopstick3 = new Chopstick(3);
        Chopstick chopstick4 = new Chopstick(4);
        Chopstick chopstick5 = new Chopstick(5);

        Thread[] threads = new Thread[]{
                new Thread(new Philosopher("p1", chopstick1, chopstick2), "p1"),
                new Thread(new Philosopher("p2", chopstick2, chopstick3), "p2"),
                new Thread(new Philosopher("p3", chopstick3, chopstick4), "p3"),
                new Thread(new Philosopher("p4", chopstick4, chopstick5), "p4"),
                new Thread(new Philosopher("p5", chopstick5, chopstick1), "p5")
        };

        for (Thread t : threads) {
            t.start();
        }

        System.in.read();

        for (Thread t : threads) {
            t.interrupt();
        }
    }

    private static class Philosopher implements Runnable {

        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        private final String name;
        private final Chopstick leftChopstick;
        private final Chopstick rightChopstick;

        Philosopher(String name, Chopstick leftChopstick, Chopstick rightChopstick) {
            this.name = name;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
        }

        @Override
        public void run() {
            while (true) {
                System.out.println("philosopher " + name + " is thinking");
                try {
                    Thread.sleep(random.nextInt(10000));
                } catch (InterruptedException e) {
                    break;
                }
                if (leftChopstick.tryTake()) {
                    System.out.println("philosopher " + name + " takes chopstick " + leftChopstick.id);
                    if (rightChopstick.tryTake()) {
                        System.out.println("philosopher " + name + " takes chopstick " + rightChopstick.id + " and start eating");
                        try {
                            Thread.sleep(random.nextInt(5000));
                        } catch (InterruptedException e) {
                            break;
                        }
                        System.out.println("philosopher " + name + " release chopstick " + rightChopstick.id);
                        rightChopstick.release();
                    } else {
                        System.out.println("philosopher " + name + " cannot take chopstick " + rightChopstick.id);
                    }
                    System.out.println("philosopher " + name + " release chopstick " + leftChopstick.id);
                    leftChopstick.release();
                } else {
                    System.out.println("philosopher " + name + " cannot take chopstick " + leftChopstick.id);
                }
            }
        }

    }

    private static class Chopstick {

        private final ReentrantLock lock = new ReentrantLock();
        private final int id;

        Chopstick(int id) {
            this.id = id;
        }

        boolean tryTake() {
            return lock.tryLock();
        }

        void release() {
            lock.unlock();
        }

        @Override
        public String toString() {
            return "Chopstick" + id;
        }

    }
}
