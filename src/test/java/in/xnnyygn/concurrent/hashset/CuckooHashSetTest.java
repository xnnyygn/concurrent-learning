package in.xnnyygn.concurrent.hashset;

import org.junit.Test;

import static org.junit.Assert.*;

public class CuckooHashSetTest {

    @Test
    public void test() {
        CuckooHashSet<Integer> set = new CuckooHashSet<>(4);
        set.add(1);
        set.add(7);
        set.add(3);
        set.add(10);
        set.add(11);
        assertTrue(set.contains(11));
        assertTrue(set.contains(10));
        assertTrue(set.contains(3));
        assertTrue(set.contains(7));
        assertTrue(set.contains(1));
        System.out.println(set);
    }

}