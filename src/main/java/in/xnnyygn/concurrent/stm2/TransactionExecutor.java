package in.xnnyygn.concurrent.stm2;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

// run by another thread is complicated
public class TransactionExecutor {

    public void execute(@Nonnull Action action) throws InterruptedException {
        Preconditions.checkNotNull(action);
        Transaction current = new Transaction();
        while (true) {
            try {
                action.run(current);
                current.commit();
                break;
            } catch (ConflictException ignored) {
                // conflict
                // rollback and try again
                current.rollback();
            } catch (WaitingForPredecessorException e) {
                current.rollback();
                Transaction predecessor = e.getPredecessor();
//                predecessor.setNext(current);
                if(predecessor.getStatus() == Transaction.Status.COMMITTED) {
                    throw new UnsupportedOperationException();
                }
                return;
            } catch (InterruptedException | RuntimeException e) {
                current.rollback();
                throw e;
            }
        }
    }

}
