package game.cartpole;

import game.action.Command;
import org.nd4j.base.Preconditions;

public class CommandCartPole extends Command<boolean[]> {

    public static final CommandCartPole
        LEFT = new CommandCartPole(new boolean[] {true, false}),
        RIGHT = new CommandCartPole(new boolean[] {false, true});

    public enum Actuation {
        left, right
    }

    private final float[] oneHotEncoding;


    private CommandCartPole(boolean[] commandData) {
        super(commandData);
        oneHotEncoding = new float[] {
                commandData[0] ? 1f : 0f,
                commandData[1] ? 1f : 0f
        };
    }

    public float[] toOneHot() {
        return oneHotEncoding;
    }

    public static CommandCartPole getByIndex(int index) {
        Preconditions.checkArgument(index >= 0 && index < 2, "Index out of bounds.", index);
        return (index == 0) ? LEFT : RIGHT;
    }
}
