package Project.Common;

public class PointsPayload extends Payload {

    private int points;

    public PointsPayload() {
        setPayloadType(PayloadType.POINTS);
    }

    public PointsPayload(long clientId, int points) {
        setPayloadType(PayloadType.POINTS);
        setClientId(clientId); // inherited from Payload
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int p) {
        this.points = p;
    }

    @Override
    public String toString() {
        return super.toString() + " points=" + points;
    }
}
