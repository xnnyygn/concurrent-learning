package in.xnnyygn.concurrent.skiplist;

import org.junit.Test;

import static org.junit.Assert.*;

public class SkipListTest {

    @Test
    public void test() {
        SkipList<Integer> list = new SkipList<>();
        list.add(1);
        list.add(3);
        list.add(7);
        list.add(2);
    }
}