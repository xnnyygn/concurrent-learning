package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class QueueLock1 {

    private final ThreadLocal<Node> currentNode = new ThreadLocal<>();
    private final AtomicReference<Node> head = new AtomicReference<>(null);
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    public void lockShared() {
        while (true) {
            Node h = head.get();
            if (h == null) {
                Node n = new Node(Mode.SHARED);
                if (head.compareAndSet(null, n)) {
                    tail.set(n);
                    currentNode.set(n);
                    return;
                }
                // head is not null
            } else {
                if (h.mode == Mode.SHARED) {
                    int tc = h.threadCount.get();
                    if (tc > 0 && h.threadCount.compareAndSet(tc, tc + 1)) {
                        currentNode.set(h);
                        return;
                    }
                    // tc is 0 or tc changed
                } else {
                    Node n = new Node(Mode.SHARED);
                    // find first shared node from tail
                    Node p = tail.getAndSet(n);
                    // ???
                }
            }
        }
    }

    public void lockExclusive() {
        throw new UnsupportedOperationException();
    }

    public void release() {
        throw new UnsupportedOperationException();
    }

    enum Mode {
        SHARED, EXCLUSIVE;
    }

    static class Node {
        static final AtomicInteger THREAD_COUNT_1 = new AtomicInteger(1);

        final Mode mode;
        final AtomicInteger threadCount;

        Node(Mode mode) {
            this.mode = mode;
            if (mode == Mode.SHARED) {
                threadCount = new AtomicInteger(1);
            } else {
                threadCount = THREAD_COUNT_1;
            }
        }

        int threadCount() {
            return threadCount.get();
        }
    }
}
