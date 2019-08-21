package game.action;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CommandQWOP extends Command<boolean[]> implements Comparable<CommandQWOP> {

    public static final CommandQWOP
            NONE = new CommandQWOP(new boolean[]{false, false, false, false}, Keys.none),
            Q = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.q),
            W = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.w),
            O = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.o),
            P = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.p),
            QO = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.qo),
            QP = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.qp),
            WO = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.wo),
            WP = new CommandQWOP(new boolean[]{true, false, false, false}, Keys.wp);

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

    public final Keys keys;

    private CommandQWOP(boolean[] commandData, Keys keys) {
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
    public int compareTo(CommandQWOP other) {
        Objects.requireNonNull(other);
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
}
