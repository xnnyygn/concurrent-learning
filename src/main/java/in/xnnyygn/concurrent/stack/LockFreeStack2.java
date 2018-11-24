package in.xnnyygn.concurrent.stack;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicStampedReference;

public class LockFreeStack2<T> {

    private final AtomicStampedReference<Node<T>> headAndStamp = new AtomicStampedReference<>(null, 0);
    private final ThreadLocal<NodePool<T>> threadLocalNodePool = ThreadLocal.withInitial(NodePool::new);

    public void push(T value) {
        Node<T> node = threadLocalNodePool.get().reuseOrMake(value);
        int[] stampHolder = new int[1];
        Node<T> head;
        do {
            head = headAndStamp.get(stampHolder);
            node.next = head;
        } while (!headAndStamp.compareAndSet(head, node, stampHolder[0], stampHolder[0] + 1));
    }

    public T pop() {
        int[] stampHolder = new int[1];
        Node<T> head;
        Node<T> next;
        do {
            head = headAndStamp.get(stampHolder);
            if (head == null) {
                throw new IllegalStateException("empty stack");
            }
            next = head.next;
        } while (!headAndStamp.compareAndSet(head, next, stampHolder[0], stampHolder[0] + 1));
        T value = head.value;
        threadLocalNodePool.get().recycle(head);
        return value;
    }

    private static class NodePool<T> {
        private final LinkedList<Node<T>> list = new LinkedList<>();

        void recycle(Node<T> node) {
            list.addLast(node);
        }

        Node<T> reuseOrMake(T value) {
            if (list.isEmpty()) {
                return new Node<>(value);
            }
            Node<T> node = list.removeFirst();
            node.value = value;
            return node;
        }
    }

    private static class Node<T> {
        private volatile T value;
        private volatile Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }
}
