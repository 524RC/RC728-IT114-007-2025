package Project.Common;

import java.util.Objects;

public class ChoicePayload extends Payload {

    public enum Choice {
        FIRE, GRASS, WATER, ELECTRIC, ROCK
    }

    private Choice choice;

    public ChoicePayload(Choice choice) {
        Objects.requireNonNull(choice, "choice cannot be null");
        this.choice = choice;
        setPayloadType(PayloadType.CHOICE);
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
