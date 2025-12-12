package Project.Common;

public class EliminationPayload extends Payload {
    private boolean eliminated;

    public EliminationPayload() {
        setPayloadType(PayloadType.ELIMINATION);
    }

    // Convenience constructor used by ServerThread
    public EliminationPayload(long clientId, boolean isEliminated) {
        this();                   
        setClientId(clientId);     
        this.eliminated = isEliminated;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    @Override
    public String toString() {
        return super.toString() + " eliminated=" + eliminated;
    }
}
