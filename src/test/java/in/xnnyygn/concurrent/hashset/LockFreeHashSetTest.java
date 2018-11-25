package in.xnnyygn.concurrent.hashset;

import org.junit.Test;

import static org.junit.Assert.*;

public class LockFreeHashSetTest {

    @Test
    public void test() {
        LockFreeHashSet<Integer> set = new LockFreeHashSet<>(4);
        set.add(8);
        set.add(9);
        set.add(13);
        set.add(7);
        set.add(10);
        set.add(1);
        System.out.println(set);
    }
}