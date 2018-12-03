package in.xnnyygn.concurrent.lock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

@SuppressWarnings("Duplicates")
public class CompositeLock {

    private static final int MIN_DELAY = 400;
    private static final int MAX_DELAY = 1000;
    private static final int STATE_FREE = 0;
    private static final int STATE_USING = 1;
    private static final int STATE_ABORTED = 2;
    private static final int STATE_UNLOCKED = 3;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final ThreadLocal<Node> threadLocalNode = new ThreadLocal<>();
    final AtomicStampedReference<Node> tail = new AtomicStampedReference<>(null, 0);
    private final Node[] nodes;

    public CompositeLock(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity <= 0");
        }
        nodes = new Node[capacity];
        for (int i = 0; i < capacity; i++) {
            nodes[i] = new Node();
        }
    }

    /**
     * Try to lock.
     *
     * Case 1: A lock, A unlock
     * Node A: FREE -> USING -> UNLOCKED
     * Tail: Node A(UNLOCKED)
     *
     * Case 2: A lock, B lock, A unlock, B unlock
     * Node A: FREE -> USING -> UNLOCKED -> FREE, set by B
     * Node B: FREE -> USING -> wait for A -> UNLOCKED
     * Tail: Node A(FREE) . Node B(UNLOCKED)
     *
     * Case 3: A lock, B lock, A unlock, B timeout
     * Node A: FREE -> USING -> UNLOCKED
     * Node B, FREE -> USING -> wait for A -> ABORTED
     * Tail: Node A(UNLOCKED) <- Node B(ABORTED)
     *
     * Case 3.1c: C lock, C unlock
     * Node A: UNLOCKED -> FREE
     * Node B: ABORTED -> FREE
     * Node C: FREE -> USING -> UNLOCK
     * Tail: Node A(FREE) <- Node B(FREE) . Node C(UNLOCK)
     *
     * Case 3.1b: C lock, C unlock
     * Node A: UNLOCKED -> FREE
     * Node B: ABORTED -> USING -> UNLOCKED
     * Tail: Node A(FREE) <- Node B(UNLOCKED)
     *
     * Case 3.1a: C lock, C unlock
     * Node A: timeout
     *
     * Case 3.2: C lock, D lock
     * Thread C choose Node A, Thread D choose Node B
     * C, waiting
     * D, CAS(T, B, A), Node B: ABORTED -> USING
     *
     * C, CAS(T, A, null), Tail: null, Node A: UNLOCKED -> USING
     * D, CAS(T, A, B) fail
     * D, CAS(T, null, B), Tail: Node B(USING)
     * C, CAS(T, null, A), fail
     * C, CAS(T, B, A), Tail: Node B(USING) . Node A(USING)
     *
     * D, CAS(T, A, B), Tail: Node B
     * C, CAS(T, A, null), fail
     * D, Node A: UNLOCKED -> FREE
     * C, CAS(T, B, A), Tail: Node B(USING) . Node A(USING)
     *
     * C, CAS(T, A, null), Tail: null, Node A: UNLOCKED -> USING
     * C, CAS(T, null, A), Tail: Node A(USING)
     * D, CAS(T, A, B), Tail: Node A(USING) . Node B(USING)
     *
     * ----------------
     *
     * @param time time
     * @param unit unit
     * @return true if locked, otherwise false
     * @throws InterruptedException if interrupted
     */
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        final long timeout = unit.toMillis(time);
        final Backoff backoff = new Backoff(MIN_DELAY, MAX_DELAY);

        Node p; // predecessor
        Node t; // tail
        int[] stamp = {0};

        // acquire node
        final Node n = nodes[random.nextInt(nodes.length)];
        // s: state
        for (int s; ; ) {
            s = n.getState();
            // case: node is free
            if (s == STATE_FREE && n.casState(s, STATE_USING)) {
                break;
            }
            // case: the thread using node aborted or unlocked
            // if node is not the last node,
            // its state will be changed by its successor(other thread)
            if ((s == STATE_ABORTED || s == STATE_UNLOCKED) && (t = tail.get(stamp)) == n) {
                p = ((s == STATE_ABORTED) ? n.predecessor : null);
                // dequeue
                if (tail.compareAndSet(t, p, stamp[0], stamp[0] + 1)) {
                    n.setState(STATE_USING);
                    break;
                }
            }
            backoff.backoff();
            if (System.currentTimeMillis() - startTime > timeout) {
                // failed to take node within time
                return false;
            }
        }
        // post-condition: node.state == USING

        // splice node
        while (true) {
            t = tail.get(stamp);
            if (tail.compareAndSet(t, n, stamp[0], stamp[0] + 1)) {
                break;
            }
            // cannot enqueue
            if (System.currentTimeMillis() - startTime > timeout) {
                n.setState(STATE_FREE);
                return false;
            }
        }
        p = t;
        // no predecessor
        if (p == null) {
            threadLocalNode.set(n);
            return true;
        }
        // post-condition: p != null

        // wait for predecessor to complete
        // ps: predecessor state
        for (int ps; ; ) {
            ps = p.getState();
            // predecessor thread unlocked,
            // it's time to enter critical section
            if (ps == STATE_UNLOCKED) {
                p.setState(STATE_FREE);
                threadLocalNode.set(n);
                return true;
            }
            if (ps == STATE_ABORTED) {
                p.casState(ps, STATE_FREE);
                p = p.predecessor;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                n.predecessor = p;
                n.setState(STATE_ABORTED);
                return false;
            }
        }
    }

    public void unlock() {
        final Node n = threadLocalNode.get();
        if (n == null) {
            throw new IllegalStateException("unlock without lock");
        }

        // state must be USING
        n.state.set(STATE_UNLOCKED);
        threadLocalNode.set(null);
    }

    static class Node {
        // null, no previous thread
        // other, waiting on the predecessor
        volatile Node predecessor = null;

        // lock normal: USING
        // timeout: ABORTED, follow predecessor, do nothing to predecessor
        // unlock: UNLOCK
        final AtomicInteger state = new AtomicInteger(STATE_FREE);

        boolean casState(int expectedState, int newState) {
            return state.compareAndSet(expectedState, newState);
        }

        void setState(int newState) {
            state.set(newState);
        }

        int getState() {
            return state.get();
        }
    }
}
