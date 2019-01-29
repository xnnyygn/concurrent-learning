package in.xnnyygn.concurrent.mylock;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConditionQueueTest {

    @Test
    public void testEnqueue1() {
        ConditionQueue q = new ConditionQueue();
        Node node = Node.createConditionForCurrent();
        q.enqueue(node);
        assertNull(node.next);
        assertEquals(node, q.head);
        assertEquals(node, q.tail);
    }

    @Test
    public void testEnqueue2() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createConditionForCurrent();
        Node node2 = Node.createConditionForCurrent();
        q.enqueue(node1);
        q.enqueue(node2);
        assertEquals(node2, node1.next);
        assertNull(node2.next);
        assertEquals(node1, q.head);
        assertEquals(node2, q.tail);
    }

    @Test
    public void testEnqueue3() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createConditionForCurrent();
        Node node2 = Node.createConditionForCurrent();
        Node node3 = Node.createConditionForCurrent();
        q.enqueue(node1);
        q.enqueue(node2);
        q.enqueue(node3);
        assertEquals(node2, node1.next);
        assertEquals(node3, node2.next);
        assertNull(node3.next);
        assertEquals(node1, q.head);
        assertEquals(node3, q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes1() {
        ConditionQueue q = new ConditionQueue();
        q.removeNonConditionNodes();
        assertNull(q.head);
        assertNull(q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes2() {
        ConditionQueue q = new ConditionQueue();
        Node node = Node.createConditionForCurrent();
        q.enqueue(node);
        q.removeNonConditionNodes();
        assertEquals(node, q.head);
        assertEquals(node, q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes3() {
        ConditionQueue q = new ConditionQueue();
        Node node = Node.createSentinel();
        q.enqueue(node);
        assertEquals(node, q.head);
        assertEquals(node, q.tail);
        q.removeNonConditionNodes();
        assertNull(q.head);
        assertNull(q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes4() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createSentinel();
        Node node2 = Node.createConditionForCurrent();
        q.enqueue(node1);
        q.enqueue(node2);
        assertEquals(node2, node1.next);
        assertNull(node2.next);
        assertEquals(node1, q.head);
        assertEquals(node2, q.tail);
        q.removeNonConditionNodes();
        assertNull(node1.next);
        assertNull(node2.next);
        assertEquals(node2, q.head);
        assertEquals(node2, q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes5() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createConditionForCurrent();
        Node node2 = Node.createSentinel();
        q.enqueue(node1);
        q.enqueue(node2);
        assertEquals(node2, node1.next);
        assertNull(node2.next);
        assertEquals(node1, q.head);
        assertEquals(node2, q.tail);
        q.removeNonConditionNodes();
        assertNull(node1.next);
        assertNull(node2.next);
        assertEquals(node1, q.head);
        assertEquals(node1, q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes6() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createSentinel();
        Node node2 = Node.createConditionForCurrent();
        Node node3 = Node.createConditionForCurrent();
        q.enqueue(node1);
        q.enqueue(node2);
        q.enqueue(node3);
        assertEquals(node2, node1.next);
        assertEquals(node3, node2.next);
        assertNull(node3.next);
        assertEquals(node1, q.head);
        assertEquals(node3, q.tail);
        q.removeNonConditionNodes();
        assertNull(node1.next);
        assertEquals(node3, node2.next);
        assertNull(node3.next);
        assertEquals(node2, q.head);
        assertEquals(node3, q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes7() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createConditionForCurrent();
        Node node2 = Node.createSentinel();
        Node node3 = Node.createConditionForCurrent();
        q.enqueue(node1);
        q.enqueue(node2);
        q.enqueue(node3);
        assertEquals(node2, node1.next);
        assertEquals(node3, node2.next);
        assertNull(node3.next);
        assertEquals(node1, q.head);
        assertEquals(node3, q.tail);
        q.removeNonConditionNodes();
        assertEquals(node3, node1.next);
        assertNull(node2.next);
        assertNull(node3.next);
        assertEquals(node1, q.head);
        assertEquals(node3, q.tail);
    }

    @Test
    public void testRemoveNonConditionNodes8() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createConditionForCurrent();
        Node node2 = Node.createConditionForCurrent();
        Node node3 = Node.createSentinel();
        q.enqueue(node1);
        q.enqueue(node2);
        q.enqueue(node3);
        assertEquals(node2, node1.next);
        assertEquals(node3, node2.next);
        assertNull(node3.next);
        assertEquals(node1, q.head);
        assertEquals(node3, q.tail);
        q.removeNonConditionNodes();
        assertEquals(node2, node1.next);
        assertNull(node2.next);
        assertNull(node3.next);
        assertEquals(node1, q.head);
        assertEquals(node2, q.tail);
    }

    @Test
    public void testDequeue1() {
        ConditionQueue q = new ConditionQueue();
        assertNull(q.dequeue());
    }

    @Test
    public void testDequeue2() {
        ConditionQueue q = new ConditionQueue();
        Node node = Node.createConditionForCurrent();
        q.enqueue(node);
        assertEquals(node, q.head);
        assertEquals(node, q.tail);
        assertEquals(node, q.dequeue());
        assertNull(q.head);
        assertNull(q.tail);
    }

    @Test
    public void testDequeue3() {
        ConditionQueue q = new ConditionQueue();
        Node node1 = Node.createConditionForCurrent();
        Node node2 = Node.createConditionForCurrent();
        q.enqueue(node1);
        q.enqueue(node2);
        assertEquals(node1, q.head);
        assertEquals(node2, q.tail);
        assertEquals(node1, q.dequeue());
        assertEquals(node2, q.head);
        assertEquals(node2, q.tail);
    }
}