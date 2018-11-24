package in.xnnyygn.concurrent.artchp07;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class CompositeLock2 implements Lock {

    private final Node[] nodes;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    protected final AtomicStampedReference<Node> atomicTail = new AtomicStampedReference<>(null, 0);
    private final ThreadLocal<Node> currentNode = new ThreadLocal<>();

    public CompositeLock2(int capacity) {
        nodes = makeNodes(capacity);
    }

    private static Node[] makeNodes(int capacity) {
        Node[] nodes = new Node[capacity];
        for (int i = 0; i < capacity; i++) {
            nodes[i] = new Node();
        }
        return nodes;
    }

    @Override
    public void lock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        Timer timer = new Timer(unit.toMillis(time));
        try {
            Node node = acquireNode(timer);
            Node predecessor = spliceNode(node, timer);
            waitForPredecessor(node, predecessor, timer);
            currentNode.set(node);
            return true;
        } catch (TimeoutException e) {
            System.out.println("thread " + Thread.currentThread() + " timeout");
            return false;
        }
    }

    private Node acquireNode(Timer timer) throws TimeoutException {
        Node node = nodes[random.nextInt(nodes.length)];
        State state;
        Node currentTail;
        int[] stampHolder = new int[1];
        Node predecessor;
        while (true) {
            if (node.atomicState.compareAndSet(State.FREE, State.WAITING)) {
                return node;
            }
            state = node.getState();
            currentTail = atomicTail.get(stampHolder);
            if ((state == State.ABORTED || state == State.RELEASED) && currentTail == node) {
                // previous thread released this node or just aborted
                predecessor = (state == State.ABORTED) ? node.predecessor : null;
                // splice node
                if (atomicTail.compareAndSet(currentTail, predecessor, stampHolder[0], stampHolder[0] + 1)) {
                    node.setState(State.WAITING);
                    return node;
                }
            }
            // backoff
            if (timer.timesUp()) {
                throw new TimeoutException();
            }
        }
    }

    private Node spliceNode(Node node, Timer timer) throws TimeoutException {
        Node predecessor;
        int[] stampHolder = new int[1];
        do {
            predecessor = atomicTail.get(stampHolder);
            if (timer.timesUp()) {
                // node is not in queue, just set it free
                // WAITING -> FREE
                node.setState(State.FREE);
                throw new TimeoutException();
            }
        } while (!atomicTail.compareAndSet(predecessor, node, stampHolder[0], stampHolder[0] + 1));
        return predecessor;
    }

    private void waitForPredecessor(Node node, Node predecessor, Timer timer) throws TimeoutException {
        if (predecessor == null) {
            return;
        }
        // wait for predecessor to release
        // after splice, node state -> aborted or released
        Node nodeWaitingFor = predecessor;
        State state;
        while ((state = nodeWaitingFor.getState()) != State.RELEASED) {
            if (state == State.ABORTED) {
                // ABORTED -> FREE
                nodeWaitingFor.setState(State.FREE);
                // redirect to predecessor of predecessor
                nodeWaitingFor = nodeWaitingFor.predecessor;
            }
            if (timer.timesUp()) {
                // predecessor is not null
                // to redirect node waiting for current to predecessor
                node.predecessor = predecessor;
                node.setState(State.ABORTED);
                throw new TimeoutException();
            }
        }
        // RELEASE -> FREE
        nodeWaitingFor.setState(State.FREE);
    }

    @Override
    public void unlock() {
        Node node = currentNode.get();
        if (node == null) {
            // no node
            throw new IllegalStateException();
        }
        node.setState(State.RELEASED);
    }

    @Override
    public String toString() {
        return "CompositeLock2{" +
                "atomicTail=" + atomicTail +
                ", nodes=" + Arrays.toString(nodes) +
                '}';
    }

    @Override
    @Nonnull
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private enum State {
        FREE, WAITING, ABORTED, RELEASED;
    }

    protected static class Node {
        final AtomicReference<State> atomicState = new AtomicReference<>(State.FREE);
        volatile Node predecessor;

        void setState(State state) {
            atomicState.set(state);
        }

        State getState() {
            return atomicState.get();
        }

        @Override
        public String toString() {
            return "Node{" +
                    "atomicState=" + atomicState +
                    ", predecessor=" + predecessor +
                    '}';
        }
    }

    private static class Timer {
        private final long startTime;
        private final long timeout;

        Timer(long timeout) {
            startTime = System.currentTimeMillis();
            this.timeout = timeout;
        }

        boolean timesUp() {
            return System.currentTimeMillis() - startTime > timeout;
        }
    }
}
