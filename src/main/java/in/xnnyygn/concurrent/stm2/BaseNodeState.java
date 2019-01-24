package in.xnnyygn.concurrent.stm2;

abstract class BaseNodeState implements NodeState {

    private final int term;
    private final NodeRole role;

    public BaseNodeState(int term, NodeRole role) {
        this.term = term;
        this.role = role;
    }

    @Override
    public int getTerm() {
        return term;
    }

    @Override
    public NodeRole getRole() {
        return role;
    }

}
