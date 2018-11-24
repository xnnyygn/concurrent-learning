package in.xnnyygn.concurrent;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConcurrentStackTest {

    @Test
    public void test() {
        ConcurrentStack<Integer> stack = new ConcurrentStack<>();
        assertNull(stack.pop());
        stack.push(1);
        stack.push(2);
        assertEquals(2, stack.pop().intValue());
        assertEquals(1, stack.pop().intValue());
        assertNull(stack.pop());
    }

}