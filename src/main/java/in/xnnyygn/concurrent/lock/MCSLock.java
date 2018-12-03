package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicReference;

public class MCSLock {

    private final ThreadLocal<Node> cNode = ThreadLocal.withInitial(Node::new);
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    public void lock() {
        Node c = cNode.get();
        Node p = tail.getAndSet(c);
        if (p != null) {
            c.waiting = true;
            p.next = c;
            while (c.waiting) {
            }
        }
    }

    public void unlock() {
        Node c = cNode.get();
        if (c.next == null) {
            if (tail.compareAndSet(c, null)) {
                // no next thread
                return;
            }
            // wait for next to be set
            while (c.next == null) {
            }
        }
        c.next.waiting = false;
        c.next = null;
    }

    private static class Node {
        volatile boolean waiting = false;
        volatile Node next = null;
    }
}
