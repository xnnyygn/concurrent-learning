package in.xnnyygn.concurrent.artchp01;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 3. Amend your program so that no philosopher ever starves.
 * 4. Write a program to provide a starvation-free solution for any number of philosophers n.
 *
 * https://en.wikipedia.org/wiki/Dining_philosophers_problem
 *
 * Chandy/Misra solution
 *
 * maybe run in order
 */
public class DinningPhilosopherProblem2 {

    public static void main(String[] args) throws Exception {
        Philosopher p1 = new Philosopher("p1", 1);
        Philosopher p2 = new Philosopher("p2", 1);
        Philosopher p3 = new Philosopher("p3", 1);
        Philosopher p4 = new Philosopher("p4", 1);
        Philosopher p5 = new Philosopher("p5", 1);

        p1.setNeighbor(p2);
        p2.setNeighbor(p3);
        p3.setNeighbor(p4);
        p4.setNeighbor(p5);
        p5.setNeighbor(p1);

        Philosopher[] philosophers = new Philosopher[]{p1, p2, p3, p4, p5};
        for (Philosopher philosopher : philosophers) {
            philosopher.start();
        }
        System.in.read();
        for (Philosopher philosopher : philosophers) {
            philosopher.interrupt();
        }
    }

    private interface Message {
    }

    private static class AbstractMessage implements Message {

        final Philosopher philosopher;

        AbstractMessage(Philosopher philosopher) {
            this.philosopher = philosopher;
        }

        Philosopher getSender() {
            return philosopher;
        }

    }

    private static class ChopstickRequest extends AbstractMessage {


        ChopstickRequest(Philosopher philosopher) {
            super(philosopher);
        }

        @Override
        public String toString() {
            return "ChopstickRequest{" +
                    "philosopher=" + philosopher +
                    '}';
        }

    }

    private static class ChopstickResponse extends AbstractMessage {

        private final boolean result;

        ChopstickResponse(Philosopher philosopher, boolean result) {
            super(philosopher);
            this.result = result;
        }

        @Override
        public String toString() {
            return "ChopstickResponse{" +
                    "philosopher=" + philosopher +
                    ", result=" + result +
                    '}';
        }

    }

    private static class ChopstickReturn extends AbstractMessage {

        private final boolean used;

        ChopstickReturn(Philosopher philosopher, boolean used) {
            super(philosopher);
            this.used = used;
        }

        @Override
        public String toString() {
            return "ChopstickReturn{" +
                    "philosopher=" + philosopher +
                    ", used=" + used +
                    '}';
        }

    }

    private static class Philosopher {

        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        private final ConcurrentLinkedDeque<Message> queue = new ConcurrentLinkedDeque<>();
        private final String name;
        private final Chopstick chopstick;
        private Philosopher neighbor;
        private boolean requested = false;
        private Thread thread;

        Philosopher(String name, int chopstickId) {
            this.name = name;
            this.chopstick = new Chopstick(chopstickId, this);
        }

        void setNeighbor(Philosopher neighbor) {
            this.neighbor = neighbor;
        }

        public void start() {
            thread = new Thread(this::run, "name");
            thread.start();
        }

        private void run() {
            Message message;
            while (true) {
                while ((message = queue.poll()) != null) {
                    System.out.println("philosopher " + name + " handle message " + message);
                    if (message instanceof ChopstickRequest) {
                        Philosopher sender = ((ChopstickRequest) message).getSender();
                        if (chopstick.status == Chopstick.Status.DIRTY) {

                            // clean chopstick before sending
                            chopstick.status = Chopstick.Status.CLEAN;
                            chopstick.user = sender;
                            sendMessage(sender, new ChopstickResponse(this, true));
                        } else {
                            sendMessage(sender, new ChopstickResponse(this, false));
                        }
                    } else if (message instanceof ChopstickResponse) {
                        // assert message.sender == neighbor
                        ChopstickResponse response = (ChopstickResponse) message;
                        if (response.result) {

                            // start to eat
                            System.out.println("philosopher's chopstick " + chopstick);
                            if (chopstick.status == Chopstick.Status.CLEAN && chopstick.user == this) {
                                System.out.println("philosopher " + name + " eats");
                                try {
                                    Thread.sleep(random.nextInt(1000));
                                } catch (InterruptedException e) {
                                    break;
                                }
                                chopstick.status = Chopstick.Status.DIRTY;
                                sendMessage(neighbor, new ChopstickReturn(this, true));
                            } else {
                                sendMessage(neighbor, new ChopstickReturn(this, false));
                            }
                        }
                        requested = false;
                    } else if (message instanceof ChopstickReturn) {
                        if (((ChopstickReturn) message).used) {
                            chopstick.status = Chopstick.Status.DIRTY;
                        }
                        chopstick.user = this;
                    }
                }

                // think
                System.out.println("philosopher " + name + " is thinking");
                try {
                    Thread.sleep(random.nextInt(5000));
                } catch (InterruptedException e) {
                    break;
                }

                // request another chopstick from neighbor
                if (!requested) {
                    sendMessage(neighbor, new ChopstickRequest(this));
                    requested = true;
                }
            }
        }

        private void sendMessage(Philosopher recipient, Message message) {
            System.out.println("send " + message + " to " + recipient);
            recipient.queue.addLast(message);
        }

        void interrupt() {
            if (thread != null) {
                thread.interrupt();
            }
        }

        @Override
        public String toString() {
            return "Philosopher{" +
                    "name='" + name + '\'' +
                    '}';
        }

    }

    private static class Chopstick {

        enum Status {
            DIRTY, CLEAN;
        }

        private final int id;
        private Philosopher user;
        private Status status = Status.DIRTY;

        Chopstick(int id, Philosopher user) {
            this.id = id;
            this.user = user;
        }

        @Override
        public String toString() {
            return "Chopstick{" +
                    "id=" + id +
                    ", status=" + status +
                    ", user=" + user +
                    '}';
        }

    }
}
