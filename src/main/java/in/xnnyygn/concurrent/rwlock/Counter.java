package in.xnnyygn.concurrent.rwlock;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

    private static final int WRITER_COUNT_MASK = 0xFF00;
    private static final int READER_COUNT_MASK = 0x00FF;
    private static final int WRITER_COUNT_UNIT = 0x100;

    private final AtomicInteger count = new AtomicInteger(0);

    private int getWriterCount(int c) {
        return c & WRITER_COUNT_MASK;
    }

    private int getReaderCount(int c) {
        return c & READER_COUNT_MASK;
    }

    private int increaseReaderCount() {
        int c;
        do {
            c = count.get();
        } while (!count.compareAndSet(c, c + 1));
        return c;
    }

    private int decreaseReaderCount() {
        int c;
        do {
            c = count.get();
            if (getReaderCount(c) == 0) {
                throw new IllegalStateException("no reader");
            }
        } while (!count.compareAndSet(c, c - 1));
        return c;
    }

    private int increaseWriterCount() {
        int c;
        do {
            c = count.get();
        } while (!count.compareAndSet(c, c + WRITER_COUNT_UNIT));
        return c;
    }

    private int decreaseWriterCount() {
        int c;
        do {
            c = count.get();
            if (getWriterCount(c) == 0) {
                throw new IllegalStateException("no writer");
            }
        } while (!count.compareAndSet(c, c - WRITER_COUNT_UNIT));
        return c;
    }

}
