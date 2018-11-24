package in.xnnyygn.concurrent.queue;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LongSpoon {

    public static void main(String[] args) throws InterruptedException, IOException {
        Person[] people = new Person[5];
        for (int i = 0; i < people.length; i++) {
            people[i] = new Person("p" + i, people);
        }
        for (Person p : people) {
            p.start();
        }
        System.in.read();
        for (Person p : people) {
            p.stopAndAwait();
        }
    }

    // Person A uses its spoon to feed Person B
    // two or more people may not feed the same person at the same time

    private static class Person implements Runnable {
        private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        private final String name;
        private final Lock lock = new ReentrantLock();
        private final Person[] people;
        private volatile boolean running = false;
        private Thread thread;

        Person(String name, Person[] people) {
            this.name = name;
            this.people = people;
        }

        void lock() {
            lock.lock();
        }

        void unlock() {
            lock.unlock();
        }

        void start() {
            thread = new Thread(this);
            running = true;
            thread.start();
        }

        @Override
        public void run() {
            Person p;
            while (running) {
                p = choosePerson();
                p.lock();
                try {
                    System.out.println(name + ", " + p.name);
                } finally {
                    p.unlock();
                }
                try {
                    Thread.sleep(threadLocalRandom.nextInt(1000) + 1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private Person choosePerson() {
            Person p;
            do {
                p = people[threadLocalRandom.nextInt(people.length)];
            } while (p == this);
            return p;
        }

        void stopAndAwait() throws InterruptedException {
            running = false;
            thread.interrupt();
            thread.join();
        }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
