package in.xnnyygn.concurrent.exchange;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArenaExchangerTest {

    @Test
    public void test() {
        int mmask = 0xff;
        int seq = mmask + 1;
        int ncpu = Runtime.getRuntime().availableProcessors();
        int full = (ncpu >= (mmask << 1)) ? mmask : ncpu >>> 1;
        System.out.println("mmask: " + mmask);
        System.out.println("seq: " + seq);
        System.out.println("ncpu: " + ncpu);
        System.out.println("full: " + full);
    }
}