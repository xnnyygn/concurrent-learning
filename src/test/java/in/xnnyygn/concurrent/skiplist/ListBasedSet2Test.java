package in.xnnyygn.concurrent.skiplist;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

public class ListBasedSet2Test {

    @Test
    public void test() {
        ListBasedSet2<Integer> set = new ListBasedSet2<>();
        assertTrue(set.add(1));
        assertTrue(set.add(3));
        assertTrue(set.add(2));
        assertFalse(set.remove(4));
        assertTrue(set.contains(3));
        assertTrue(set.remove(3));
        assertFalse(set.contains(3));
    }

    @Test
    public void testBreakOuter() {
        outer:
        for (; ; ) {
            for (int i = 0; i < 4; i++) {
                if (i == 2) {
                    break outer;
                }
            }
        }
        System.out.println("done");
    }

    @Test
    public void testRandomLevel() {
        Map<Integer, Integer> map = new HashMap<>();
        int r = (int) System.currentTimeMillis();
        int n;
        int c;
        for (int i = 0; i < 1000; i++) {
            r ^= r << 13;
            r ^= r >>> 17;
            r ^= r << 5;
            if ((r & 0x80000001) != 0) {
                map.put(-1, map.getOrDefault(-1, 0) + 1);
                continue;
            }

            n = r;
            c = 1;
            while (((n >>>= 1) & 1) != 0) {
                c++;
            }
            map.put(c, map.getOrDefault(c, 0) + 1);
        }
        System.out.println(map);
    }
}