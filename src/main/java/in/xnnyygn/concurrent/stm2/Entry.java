package in.xnnyygn.concurrent.stm2;

public class Entry implements EntryMeta {
    private final int index;
    private final int term;
    private final byte[] content;

    public Entry(int index, int term, byte[] content) {
        this.index = index;
        this.term = term;
        this.content = content;
    }

    public int getIndex() {
        return index;
    }

    public int getTerm() {
        return term;
    }

    public byte[] getContent() {
        return content;
    }
}
