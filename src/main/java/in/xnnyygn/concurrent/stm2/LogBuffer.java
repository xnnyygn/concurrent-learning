package in.xnnyygn.concurrent.stm2;

import javax.annotation.Nullable;
import java.util.LinkedList;

public class LogBuffer {

    private final LinkedList<Entry> entries = new LinkedList<>();

    public void append(int index, int term, byte[] content) {
        entries.add(new Entry(index, term, content));
    }

    @Nullable
    public Entry getLast() {
        return entries.getLast();
    }

}
