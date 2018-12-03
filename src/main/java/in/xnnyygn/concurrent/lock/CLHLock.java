package in.xnnyygn.concurrent.lock;

import java.util.concurrent.atomic.AtomicReference;

public class CLHLock {

    private final ThreadLocal<Node> cNode = ThreadLocal.withInitial(Node::new);
    private final ThreadLocal<Node> pNode = new ThreadLocal<>();
    private final AtomicReference<Node> state = new AtomicReference<>(new Node());

    public void lock() {
        Node c = cNode.get();
        c.locked = true;
        Node p = state.getAndSet(c);
        while (p.locked) {
        }
        pNode.set(p);
    }

    public void unlock() {
        Node p = pNode.get();
        Node c = cNode.get();
        c.locked = false;
        cNode.set(p);
    }

    private static class Node {
        volatile boolean locked = false;
    }
}
