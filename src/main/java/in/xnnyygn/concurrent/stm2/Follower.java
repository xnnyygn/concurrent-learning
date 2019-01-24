package in.xnnyygn.concurrent.stm2;

public class Follower extends BaseNodeState {

    private final String votedFor;
    private final String leaderId;

    public Follower(int term, String votedFor, String leaderId) {
        super(term, NodeRole.FOLLOWER);
        this.votedFor = votedFor;
        this.leaderId = leaderId;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public String getLeaderId() {
        return leaderId;
    }

}
