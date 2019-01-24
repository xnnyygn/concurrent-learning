package in.xnnyygn.concurrent.stm2;

public class Leader extends BaseNodeState {

    public Leader(int term) {
        super(term, NodeRole.LEADER);
    }

}
