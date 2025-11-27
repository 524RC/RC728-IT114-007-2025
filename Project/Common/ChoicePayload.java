package Project.Common;

public class ChoicePayload extends Payload {

    public enum Choice {
        ROCK, PAPER, SCISSORS
    }

    private Choice choice;

    public ChoicePayload(Choice choice) {
        setPayloadType(PayloadType.CHOICE); // <- must match server expectation
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
