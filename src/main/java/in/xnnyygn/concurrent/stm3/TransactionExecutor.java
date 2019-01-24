package in.xnnyygn.concurrent.stm3;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

public class TransactionExecutor {

    // abort by self? ConflictException
    // abort by other thread AbortedException
    // abort by action explicit: AbortException
    // abort by action, exception
    public void execute(@Nonnull Action action) throws InterruptedException {
        Preconditions.checkNotNull(action);
        Transaction transaction;
        while (true) {
            transaction = new Transaction();
            try {
                action.run(transaction);
                transaction.commit();
                return;
            } catch (ConflictException | AbortedException e) {
                System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
                // conflict: rollback and run again
                // aborted by other thread: rollback and run again
                transaction.rollback();
            } catch (InterruptedException | RuntimeException e) {
                transaction.abort();
                throw e;
            }
        }
    }

}
