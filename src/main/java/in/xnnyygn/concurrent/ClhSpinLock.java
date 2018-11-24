package in.xnnyygn.concurrent;

import java.util.concurrent.atomic.AtomicReference;

public class ClhSpinLock {

    private final AtomicReference<Node> atomicOwner = new AtomicReference<>(new Node());
    private final ThreadLocal<Node> threadLocalCurrentNode = ThreadLocal.withInitial(Node::new);
    private final ThreadLocal<Node> threadLocalPreviousNode = new ThreadLocal<>();

    public void lock() {
        Node currentNode = threadLocalCurrentNode.get();
        currentNode.locking = true;
        Node previousNode = atomicOwner.getAndSet(currentNode);
        threadLocalPreviousNode.set(previousNode);
        while (previousNode.locking) {
        }
    }

    public void unlock() {
        Node currentNode = threadLocalCurrentNode.get();
        currentNode.locking = false;
        threadLocalCurrentNode.set(threadLocalPreviousNode.get());
    }

    public static class Node {
        volatile boolean locking = false;
    }
}
