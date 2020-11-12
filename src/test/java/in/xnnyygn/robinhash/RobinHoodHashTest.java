package in.xnnyygn.robinhash;

import org.junit.Test;

import static org.junit.Assert.*;

public class RobinHoodHashTest {

    @Test
    public void test() {
        RobinHoodHash<Integer, Integer> map = new RobinHoodHash<>(16);
        map.add(1, 3);
        map.add(17, 4);
        map.add(33, 5);
        map.add(0, 7);
        map.add(16, 8);
        assertTrue(map.contains(1));
        assertTrue(map.contains(33));
        assertTrue(map.contains(1));
        assertFalse(map.contains(99));
    }
}