package in.xnnyygn.concurrent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HashMap2Test {

    @Test
    public void testPut() {
        HashMap2<Key, Integer> map = new HashMap2<>();
        Key key1 = new Key(1, 1);
        Key key2 = new Key(2, 1);
        assertNull(map.put(key1, 3));
        assertEquals(3, map.put(key1, 1).intValue());
        assertEquals(1, map.size());
        map.put(key2, 2);
        assertEquals(2, map.size());
        assertEquals(1, map.get(key1).intValue());
        assertEquals(2, map.get(key2).intValue());
        assertEquals(1, map.remove(key1).intValue());
        assertEquals(1, map.size());
        assertNull(map.remove(key1));
        assertEquals(2, map.remove(key2).intValue());
        assertEquals(0, map.size());
    }

    private static class Key {
        private final int id;
        private final int hashCode;

        public Key(int id, int hashCode) {
            this.id = id;
            this.hashCode = hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return id == key.id;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    @Test
    public void testEnlarge() {
        HashMap2<Integer, Integer> map = new HashMap2<>(4, 0.5);
        map.put(1, 1);
        map.put(2, 1);
        map.put(3, 1);

        map.put(4, 1);
        map.put(5, 1);
        assertEquals(5, map.size());
    }
}