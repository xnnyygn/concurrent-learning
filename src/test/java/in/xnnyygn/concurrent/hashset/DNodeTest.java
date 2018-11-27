package in.xnnyygn.concurrent.hashset;

import org.junit.Test;

public class DNodeTest {

    private static class DRoot<T> {
        private final DNode<T> left; // 0
        private DNode<T> right; // 1

        DRoot() {
            left = new DNode<>(0, 0);
        }

        int locate(int index) {
            if (index < 0) {
                throw new IllegalArgumentException("index < 0");
            }
            DNode<T> node = (index & 1) == 0 ? left : right();
            return node.locate(index, 2);
        }

        private DNode<T> right() {
            if (right == null) {
                System.out.println("make node 1");
                right = new DNode<>(1, 1);
            }
            return right;
        }
    }

    private static class DNode<T> {
        private final int index;
        private final int value;
        private DNode<T> left;
        private DNode<T> right;

        DNode(int index, int value) {
            this.index = index;
            this.value = value;
        }

        int locate(int index, int mask) {
            System.out.println("index = " + index + ", mask = " + Integer.toBinaryString(mask));
            if (index == this.index) {
                return value;
            }
            if ((index & mask) == 0) {
                System.out.println("try left");
                return left().locate(index, mask << 1);
            }
            System.out.println("try right");
            return right(mask).locate(index, mask << 1);
        }

        private DNode<T> left() {
            if (left == null) {
                left = new DNode<>(index, value);
            }
            return left;
        }

        private DNode<T> right(int mask) {
            if (right == null) {
                int i = this.index + mask;
                System.out.println("make node " + Integer.toBinaryString(i));
                right = new DNode<>(i, i);
            }
            return right;
        }
    }

    @Test
    public void test() {
        DRoot<Integer> root = new DRoot<>();
        root.locate(4);
        root.locate(13);
        root.locate(5);
    }

}
