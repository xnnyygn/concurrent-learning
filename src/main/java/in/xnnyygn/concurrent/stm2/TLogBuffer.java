package in.xnnyygn.concurrent.stm2;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class TLogBuffer extends AbstractTObject {

    private final LinkedList<Entry> entries = new LinkedList<>();

    private final AtomicReference<Transaction> lastTransaction = new AtomicReference<>();

    public TLogBuffer(int lastIndex, int lastTerm) {
        entries.add(new Entry(lastIndex, lastTerm, new byte[0]));
    }

    public int append(@Nonnull Transaction transaction, int term, byte[] content) {
        Preconditions.checkNotNull(transaction);
        switch (transaction.getStatus()) {
            case COMMITTED:
                throw new UnsupportedOperationException("cannot append out of transaction");
            case ABORTED:
                throw new AbortedException();
        }
        // status == ACTIVE
        Transaction last = lastTransaction.getAndSet(transaction);
        if (last != null) {
            // TODO check last status
            // contention resolver
            // if predecessor.version > local.version, rollback and wait
            if (last.compareTo(transaction)) {
                // abort self
                // ACTIVE -> ABORTING
                transaction.abort();
                // ABORTING -> ABORTED
                throw new WaitingForPredecessorException(last);
                // try to attach to predecessor or wait for predecessor to complete
            } else {
                last.abort();
                // abort other transaction
                // wait for predecessor to complete
            }


//            last.setNext(transaction);
            LockSupport.park(this);
        }
        // 1. last == null
        // 2. unparked by previous thread
        int index = entries.getLast().getIndex() + 1;
        entries.add(new Entry(index, term, content));
        transaction.doWhenRollback(this, entries::removeLast);
        return index;
    }

    public EntryMeta getLastEntryMeta() {
        throw new UnsupportedOperationException();
    }
}
