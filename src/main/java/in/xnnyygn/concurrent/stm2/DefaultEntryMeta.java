package in.xnnyygn.concurrent.stm2;

public class DefaultEntryMeta implements EntryMeta {

    private final int index;
    private final int term;

    public DefaultEntryMeta(int index, int term) {
        this.index = index;
        this.term = term;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getTerm() {
        return term;
    }

}
