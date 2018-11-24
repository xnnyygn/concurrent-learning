package in.xnnyygn.concurrent;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class RunOnceTest {

    @Test
    public void testGet() throws ExecutionException {
        RunOnce<Integer> result = new RunOnce<>(() -> {
//            System.out.println("run");
            return 1 + 2;
        });
        assertEquals(3, result.get().intValue());
        assertEquals(3, result.get().intValue());
    }

}