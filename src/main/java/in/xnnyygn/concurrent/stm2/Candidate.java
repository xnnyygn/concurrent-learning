package in.xnnyygn.concurrent.stm2;

public class Candidate extends BaseNodeState {

    private final int votes;

    public Candidate(int term) {
        this(term, 1);
    }

    public Candidate(int term, int votes) {
        super(term, NodeRole.CANDIDATE);
        this.votes = votes;
    }

    public int getVotes() {
        return votes;
    }

}
