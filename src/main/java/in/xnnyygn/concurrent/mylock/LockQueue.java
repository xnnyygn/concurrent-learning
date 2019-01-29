package in.xnnyygn.concurrent.mylock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicReference;

class LockQueue {
    final AtomicReference<Node> head = new AtomicReference<>();
    final AtomicReference<Node> tail = new AtomicReference<>();

    @Nonnull
    Node enqueue(@Nonnull Node node) {
        Node t;
        while (true) {
            t = tail.get();
            if (t == null) {
                Node sentinel = Node.createSentinel();
                if (head.compareAndSet(null, sentinel)) {
                    tail.set(sentinel);
                }
            } else {
                node.predecessor.lazySet(t);
                if (tail.compareAndSet(t, node)) {
                    t.successor.set(node);
                    return t;
                }
            }
        }
    }

    boolean isFirstCandidate(@Nonnull Node node) {
        return node.predecessor.get() == head.get();
    }

    @Nullable
    Node findNormalSuccessor(@Nonnull Node node) {
        Node s = node.successor.get();
        if (s != null && s.isAborted()) {
            return s;
        }

        s = null;
        Node c = tail.get();
        while (c != node) {
            if (!c.isAborted()) {
                s = c;
            }
            c = c.predecessor.get();
        }
        return s;
    }

    @Nonnull
    Node skipAbortedPredecessor(@Nonnull Node node) {
        Node h = head.get();
        Node p = node.predecessor.get();
        Node c = p;
        while (c != h && c.isAborted()) {
            c = c.predecessor.get();
        }
        if (c != p) {
            node.predecessor.set(c);
        }
        return c;
    }

    boolean contains(@Nonnull Node node) {
        for (Node c = tail.get(); c != null; c = c.predecessor.get()) {
            if (node == c) {
                return true;
            }
        }
        return false;
    }
}
