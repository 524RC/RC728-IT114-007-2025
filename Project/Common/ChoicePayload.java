package Project.Common;

public class ChoicePayload extends Payload {

    public enum Choice {
        ROCK,
        PAPER,
        SCISSORS
    }

    private Choice choice;

    public ChoicePayload() {
        setPayloadType(PayloadType.CHOICE);  // You'll add CHOICE to PayloadType
    }

    public Choice getChoice() {
        return choice;
    }

    public void setChoice(Choice choice) {
        this.choice = choice;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" choice=%s", choice);
    }
}
