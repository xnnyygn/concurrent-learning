package in.xnnyygn.concurrent.stm2;

public class WaitingForPredecessorException extends RuntimeException {

    private final Transaction predecessor;

    public WaitingForPredecessorException(Transaction predecessor) {
        this.predecessor = predecessor;
    }

    public Transaction getPredecessor() {
        return predecessor;
    }

}
