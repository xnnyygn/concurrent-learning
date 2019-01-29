package in.xnnyygn.concurrent.mylock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class ConditionQueue {
    Node head = null;
    Node tail = null;

    void enqueue(@Nonnull Node node) {
        // assert node.status == CONDITION
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
    }

    void removeNonConditionNodes() {
        Node p = null; // previous non-condition node
        Node l = null; // last condition node
        Node c = head; // current
        Node n; // next
        while (c != null) {
            n = c.next;
            if (c.status.get() != Node.STATUS_CONDITION) {
                if (p == null) {
                    p = n;
                    head = n;
                } else {
                    p.next = n;
                }
                c.next = null;
            } else {
                p = c;
                l = c;
            }
            c = n;
        }
        tail = l;
    }

    @Nullable
    Node dequeue() {
        if (head == null) {
            return null;
        }
        Node node = head;
        head = head.next;
        if (tail == node) {
            tail = null;
        }
        node.next = null;
        return node;
    }
}
