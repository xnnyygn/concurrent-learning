package in.xnnyygn.concurrent.mylock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class Node {
    static final int STATUS_NORMAL = 0;
    static final int STATUS_SIGNAL = 1;
    static final int STATUS_CONDITION = 2;
    static final int STATUS_ABORTED = -1;

    final AtomicReference<Thread> thread;
    final AtomicInteger status;
    final AtomicReference<Node> predecessor = new AtomicReference<>();
    final AtomicReference<Node> successor = new AtomicReference<>();
    /**
     * next node in condition queue
     */
    Node next = null;

    private Node(Thread thread, int status) {
        this.thread = new AtomicReference<>(thread);
        this.status = new AtomicInteger(status);
    }

    static Node createSentinel() {
        return new Node(null, STATUS_NORMAL);
    }

    static Node createNormalForCurrent() {
        return new Node(Thread.currentThread(), STATUS_NORMAL);
    }

    static Node createConditionForCurrent() {
        return new Node(Thread.currentThread(), STATUS_CONDITION);
    }

    boolean isAborted() {
        return status.get() == STATUS_ABORTED;
    }

    boolean ensureSignalStatus() {
        int s = status.get();
        return s == STATUS_SIGNAL || (s == STATUS_NORMAL && status.compareAndSet(STATUS_NORMAL, STATUS_SIGNAL));
    }

    boolean resetSignalStatus() {
        return status.get() == STATUS_SIGNAL && status.compareAndSet(STATUS_SIGNAL, STATUS_NORMAL);
    }
}
