package in.xnnyygn.concurrent.exchange;

import org.junit.Test;

import static org.junit.Assert.*;

public class JucExchangerTest {

    @Test
    public void test() {
        JucExchanger<Integer> exchanger = new JucExchanger<>();
        exchanger.arenaExchange(1, false, 0L);
    }
}