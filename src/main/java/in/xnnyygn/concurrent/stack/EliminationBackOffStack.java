package in.xnnyygn.concurrent.stack;

import java.util.concurrent.TimeoutException;

public class EliminationBackOffStack<T> extends LockFreeStack<T> {

    private static final int CAPACITY = 10;
    private final EliminationArray<T> eliminationArray = new EliminationArray<>(CAPACITY);

    public void push(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value != null");
        }
        Node<T> node = new Node<>(value);
        T otherValue;
        while (true) {
            if (tryPush(node)) {
                return;
            }
            try {
                otherValue = eliminationArray.visit(value, CAPACITY);
                if (otherValue == null) {
                    return;
                }
            } catch (TimeoutException ignore) {
            }
        }
    }

    public T pop() {
        Node<T> top;
        T otherValue;
        while (true) {
            top = tryPop();
            if (top != null) {
                return top.value;
            }
            try {
                otherValue = eliminationArray.visit(null, CAPACITY);
                if (otherValue != null) {
                    return otherValue;
                }
            } catch (TimeoutException ignore) {
            }
        }
    }
}
