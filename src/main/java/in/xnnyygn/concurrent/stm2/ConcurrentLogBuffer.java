package in.xnnyygn.concurrent.stm2;

import java.util.concurrent.atomic.AtomicReference;

public class ConcurrentLogBuffer {

    private final AtomicReference<Node> head;
    private final AtomicReference<Node> tail;

    public ConcurrentLogBuffer(int lastIndex, int lastTerm) {
        final Node node = new Node(new Entry(lastIndex, lastTerm, new byte[0]));
        head = new AtomicReference<>(node);
        tail = new AtomicReference<>(node);
    }

    public int append(int term, byte[] content) {
        Node t;
        Node n;
        Node m;
        while (true) {
            t = tail.get();
            n = t.next.get();
            if (n != null) {
                tail.compareAndSet(t, n);
                continue;
            }
            m = new Node(new Entry(t.entry.getIndex() + 1, term, content));
            if (t.next.compareAndSet(null, m)) {
                tail.compareAndSet(t, m);
                return m.entry.getIndex();
            }
        }
    }

    public EntryMeta getLastLogMeta() {
        Node c = tail.get();
        Node n;
        while ((n = c.next.get()) != null) {
            c = n;
        }
        return c.entry;
    }

    // TODO find log by id, skip list?
    // TODO list logs after
    // TODO remove logs after -> slave, no append
    // TODO remove logs before -> master/slave snapshot
    // TODO remove head -> commit
    // W+W
    // R+W
    // R+R

    private static class Node {
        final Entry entry;
        final AtomicReference<Node> next = new AtomicReference<>(null);

        Node(Entry entry) {
            this.entry = entry;
        }
    }
}
