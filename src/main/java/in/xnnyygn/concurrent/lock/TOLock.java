package in.xnnyygn.concurrent.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TOLock {

    private static final Node UNLOCKED = new Node();
    private final ThreadLocal<Node> cNode = new ThreadLocal<>();
    private final AtomicReference<Node> tail = new AtomicReference<>(null);

    public boolean tryLock(long time, TimeUnit unit) {
        long timeout = unit.toMillis(time);
        long startTime = System.currentTimeMillis();
        // node cannot be reuse until other threads
        // waiting for current node enter critical section,
        // so allocate every time
        Node c = new Node(); // locking = true
        cNode.set(c);
        Node p = tail.getAndSet(c);
        // no previous node
        if (p == null) {
            return true;
        }
        Node pp;
        while (System.currentTimeMillis() - startTime < timeout) {
            pp = p.predecessor;
            // previous node is unlocked
            if (pp == UNLOCKED) {
                return true;
            }
            // try to find first locking node
            if (pp != null) {
                p = pp;
            }
        }
        // timeout
        // try to rollback to previous node if current node is the last node
        // if failed, set its predecessor to previous node
        if (!tail.compareAndSet(c, p)) {
            c.predecessor = p;
        }
        return false;
    }

    public void unlock() {
        Node c = cNode.get();
        // try to set last node to null if current node is the last node
        // if failed, set its predecessor to UNLOCKED
        if (!tail.compareAndSet(c, null)) {
            c.predecessor = UNLOCKED;
        }
    }

    // when created, predecessor = null
    // when unlock, predecessor = UNLOCKED
    // when timeout, predecessor = previous node
    private static class Node {
        volatile Node predecessor = null;
    }
}
