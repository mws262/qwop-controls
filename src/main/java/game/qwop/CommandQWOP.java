package game.qwop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CommandQWOP extends Command<boolean[]> {

    public static final CommandQWOP
            NONE = new CommandQWOP(new boolean[]{false, false, false, false}, Keys.none),
            Q = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.q),
            W = new CommandQWOP(new boolean[]{false, true, false, false}, Keys.w),
            O = new CommandQWOP(new boolean[]{false, false, true, false}, Keys.o),
            P = new CommandQWOP(new boolean[]{false, false, false, true}, Keys.p),
            QO = new CommandQWOP(new boolean[]{true, false, true, false}, Keys.qo),
            QP = new CommandQWOP(new boolean[]{true, false, false, true}, Keys.qp),
            WO = new CommandQWOP(new boolean[]{false, true, true, false}, Keys.wo),
            WP = new CommandQWOP(new boolean[]{false, true, false, true}, Keys.wp);

    private static final Map<CommandQWOP, Keys> commandToKeys = new LinkedHashMap<>(9);
    static {
        commandToKeys.put(NONE, Keys.none);
        commandToKeys.put(Q, Keys.q);
        commandToKeys.put(W, Keys.w);
        commandToKeys.put(O, Keys.o);
        commandToKeys.put(P, Keys.p);
        commandToKeys.put(QO, Keys.qo);
        commandToKeys.put(QP, Keys.qp);
        commandToKeys.put(WO, Keys.wo);
        commandToKeys.put(WP, Keys.wp);
    }

    private static final Map<Keys, CommandQWOP> keysToCommand = new LinkedHashMap<>(9);
    static {
        keysToCommand.put(Keys.none, NONE);
        keysToCommand.put(Keys.q, Q);
        keysToCommand.put(Keys.w, W);
        keysToCommand.put(Keys.o, O);
        keysToCommand.put(Keys.p, P);
        keysToCommand.put(Keys.qo, QO);
        keysToCommand.put(Keys.qp, QP);
        keysToCommand.put(Keys.wo, WO);
        keysToCommand.put(Keys.wp, WP);
    }

    /**
     * A one-hot, 9-element array for each of the 9 valid key combinations. Kept float for neural network training.
     */
    private static final Map<Keys, Command<float[]>> labelsToOneHot = new LinkedHashMap<>();
    static {
        labelsToOneHot.put(Keys.none, new Command<>(new float[]{0, 0, 0, 0, 0, 0, 0, 0, 1}));
        labelsToOneHot.put(Keys.q, new Command<>(new float[]{1, 0, 0, 0, 0, 0, 0, 0, 0}));
        labelsToOneHot.put(Keys.w, new Command<>(new float[]{0, 1, 0, 0, 0, 0, 0, 0, 0}));
        labelsToOneHot.put(Keys.o, new Command<>(new float[]{0, 0, 1, 0, 0, 0, 0, 0, 0}));
        labelsToOneHot.put(Keys.p, new Command<>(new float[]{0, 0, 0, 1, 0, 0, 0, 0, 0}));
        labelsToOneHot.put(Keys.qo, new Command<>(new float[]{0, 0, 0, 0, 0, 0, 1, 0, 0}));
        labelsToOneHot.put(Keys.qp, new Command<>(new float[]{0, 0, 0, 0, 1, 0, 0, 0, 0}));
        labelsToOneHot.put(Keys.wo, new Command<>(new float[]{0, 0, 0, 0, 0, 1, 0, 0, 0}));
        labelsToOneHot.put(Keys.wp, new Command<>(new float[]{0, 0, 0, 0, 0, 0, 0, 1, 0}));
    }

    /**
     * Potential key combinations.
     */
    public enum Keys {
        q, w, o, p, qp, wo, qo, wp, none
    }

    public static final int NUM_COMMANDS = 9;

    @JsonProperty("keys")
    public final Keys keys;

    private CommandQWOP(@JsonProperty("commandData") boolean[] commandData, @JsonProperty("keys") Keys keys) {
        super(commandData);
        this.keys = keys;
    }

    public static CommandQWOP getCommand(Keys keys) {
        return keysToCommand.get(keys);
    }

    public static CommandQWOP booleansToCommand(boolean q, boolean w, boolean o, boolean p) {
        if (q) {
            if (o) {
                return CommandQWOP.QO;
            }else if (p) {
                return CommandQWOP.QP;
            }else {
                return CommandQWOP.Q;
            } // QW is not valid and will just do Q anyway
        } else if (w) {
            if (o) {
                return CommandQWOP.WO;
            }else if (p) {
                return CommandQWOP.WP;
            } else {
                return CommandQWOP.W;
            }
        } else if (o) {
            return CommandQWOP.O;
        } else if (p) {
            return CommandQWOP.P;
        } else {
            return CommandQWOP.NONE;
        }
    }

    public static Command<float[]> keysToOneHot(Keys keys) {
        return labelsToOneHot.get(keys);
    }

    /**
     * Sorting order is kinda arbitrary. It's fewer keys pressed to more keys pressed, then according to q, w, o, p
     * for left hand, then same for right hand.
     */
    @Override
    public int compareTo(@NotNull Command other) {
        Objects.requireNonNull(other);
        if (!(other instanceof CommandQWOP)) {
            throw new IllegalArgumentException("Command comparison was given a mismatched type: " + other.getClass().getName());
        }
        for (CommandQWOP command : commandToKeys.keySet()) {
            if (this.equals(command)) {
                if (other.equals(command)) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (other.equals(command)) {
                return 1;
            }
        }
        throw new RuntimeException("Command comparison failed.");
    }

    @Override
    public String toString() {
        return (get()[0] ? "q" : "")
                + (get()[1] ? "w" : "")
                + (get()[2] ? "o" : "")
                + (get()[3] ? "p" : "")
                + (keys.equals(Keys.none) ? "none" : "");
    }



    @JsonIgnore
    @Override
    public Command<boolean[]> getThis() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandQWOP that = (CommandQWOP) o;
        return keys == that.keys && Arrays.equals(this.get(), that.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(keys, get());
    }
}
