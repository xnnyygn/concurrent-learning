package in.xnnyygn.concurrent;

import org.junit.Test;

public class ForTest {

    @Test
    public void test() throws InterruptedException {
        restart:
        for(int i = 0; i < 10; i++) {
            if(i == 4) {
                continue restart;
            }
            System.out.println(i);
            Thread.sleep(2000);
        }
    }
}
