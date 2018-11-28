package in.xnnyygn.concurrent.hashset;

import org.junit.Test;

import static org.junit.Assert.*;

public class GrowingArrayTest {

    @Test
    public void test() {
        GrowingArray<Integer> array = new GrowingArray<>(1);
        assertEquals(1, array.capacity());
        array.set(0, 1);
        array.resize();
        assertEquals(2, array.capacity());
        array.set(1, 2);
        array.resize();
        assertEquals(4, array.capacity());
        array.set(2, 3);
        System.out.println(array);
    }

}