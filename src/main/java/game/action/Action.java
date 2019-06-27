package game.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.*;

/**
 * Contains the keypresses and durations for a single action. Works like an uneditable {@link java.util.Queue}. Call
 * {@link Action#poll()} to get the keystrokes at each timestep execution. Call {@link Action#hasNext()} to make
 * sure there are timesteps left in this action. Call {@link Action#reset()} to restore the duration of the action back to
 * original.
 * <p>
 * Note that when constructed, game.action may not be changed or polled. They serve as an "immutable" backup of the
 * action. To get a pollable version of the {@link Action}, call {@link Action#getCopy()}.
 *
 * @author Matt
 * @see java.util.Queue
 * @see ActionList
 */
public class Action implements Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * Potential key combinations.
     */
    public enum Keys {
        q, w, o, p, qp, wo, qo, wp, none
    }

    /**
     * A one-hot, 9-element array for each of the 9 valid key combinations. Kept float for neural network training.
     */
    private final static Map<Keys, float[]> labelsToOneHot = new HashMap<>();

    /**
     * Association between the key labels to the q, w, o, p boolean buttons pressed.
     */
    private final static Map<Keys, boolean[]> labelsToButtons = new HashMap<>();

    /**
     * Total number of Box2d timesteps that this key combination should be held.
     **/
    private final int timestepsTotal;

    /**
     * Number of timesteps left to hold this command.
     **/
    private int timestepsRemaining;

    /**
     * Which of the QWOP keys are pressed?
     **/
    private final boolean[] keysPressed = new boolean[4];

    /**
     * Is this the immutable original or a derived, mutable copy. A little bit hacky, but a way of avoiding threading
     * issues without major modifications.
     **/
    private boolean isExecutableCopy = false;

    static {
        labelsToOneHot.put(Keys.q, new float[] {1, 0, 0, 0, 0, 0, 0, 0, 0});
        labelsToOneHot.put(Keys.w, new float[] {0, 1, 0, 0, 0, 0, 0, 0, 0});
        labelsToOneHot.put(Keys.o, new float[] {0, 0, 1, 0, 0, 0, 0, 0, 0});
        labelsToOneHot.put(Keys.p, new float[] {0, 0, 0, 1, 0, 0, 0, 0, 0});
        labelsToOneHot.put(Keys.qp, new float[] {0, 0, 0, 0, 1, 0, 0, 0, 0});
        labelsToOneHot.put(Keys.wo, new float[] {0, 0, 0, 0, 0, 1, 0, 0, 0});
        labelsToOneHot.put(Keys.qo, new float[] {0, 0, 0, 0, 0, 0, 1, 0, 0});
        labelsToOneHot.put(Keys.wp, new float[] {0, 0, 0, 0, 0, 0, 0, 1, 0});
        labelsToOneHot.put(Keys.none, new float[] {0, 0, 0, 0, 0, 0, 0, 0, 1});

        labelsToButtons.put(Keys.q, new boolean[]{true, false, false, false});
        labelsToButtons.put(Keys.w, new boolean[]{false, true, false, false});
        labelsToButtons.put(Keys.o, new boolean[]{false, false, true, false});
        labelsToButtons.put(Keys.p, new boolean[]{false, false, false, true});
        labelsToButtons.put(Keys.qp, new boolean[]{true, false, false, true});
        labelsToButtons.put(Keys.wo, new boolean[]{false, true, true, false});
        labelsToButtons.put(Keys.qo, new boolean[]{true, false, true, false});
        labelsToButtons.put(Keys.wp, new boolean[]{false, true, false, true});
        labelsToButtons.put(Keys.none, new boolean[]{false, false, false, false});
    }

    /**
     * Create an action containing the time to hold and the key combination.
     *
     * @param totalTimestepsToHold Number of timesteps to hold the keys associated with this Action.
     * @param keysPressed          4-element boolean array representing whether the Q, W, O, and P keys are pressed.
     *                             True means pressed down. False means not pressed.
     */
    public Action(int totalTimestepsToHold, boolean[] keysPressed) {
        this(totalTimestepsToHold, validateBooleanKeys(keysPressed)[0], keysPressed[1], keysPressed[2], keysPressed[3]);

    }

    /**
     * Create an action containing the time to hold and the key combination.
     *
     * @param totalTimestepsToHold Number of timesteps to hold the keys associated with this Action.
     * @param Q                    Whether the Q key is pressed during this action.
     * @param W                    Whether the W key is pressed during this action.
     * @param O                    Whether the O key is pressed during this action.
     * @param P                    Whether the P key is pressed during this action.
     */
    public Action(int totalTimestepsToHold, boolean Q, boolean W, boolean O, boolean P) {
        if (totalTimestepsToHold < 0)
            throw new IllegalArgumentException("New QWOP Action must have non-negative duration. Given: " + totalTimestepsToHold);
        this.timestepsTotal = totalTimestepsToHold;
        keysPressed[0] = Q;
        keysPressed[1] = W;
        keysPressed[2] = O;
        keysPressed[3] = P;
        timestepsRemaining = timestepsTotal;
    }

    /**
     * Create an action containing the time to hold and the key combination.
     *
     * @param totalTimestepsToHold Number of timesteps to hold the keys associated with this Action.
     * @param keysPressed Keys pressed during this action.
     */
    public Action(int totalTimestepsToHold, Keys keysPressed) {
        this(totalTimestepsToHold, Action.keysToBooleans(keysPressed));
    }

    /**
     * Return the keys for this action and decrement the timestepsRemaining.
     *
     * @return A 4-element array containing true/false for whether each of the Q, W, O, and P keys are pressed.
     */
    public boolean[] poll() {
        if (!isExecutableCopy)
            throw new RuntimeException("Trying to execute the base version of the Action. Due to multi-threading, " +
                    "this REALLY screws with the counters in the action. Call getCopy for the version you should use.");
        if (timestepsRemaining <= 0)
            throw new IndexOutOfBoundsException("Tried to poll an action which was already completed. Call hasNext() " +
                    "to check before calling poll().");
        timestepsRemaining--;

        return keysPressed;
    }

    /**
     * Return the keys pressed in this action without changing the number of timesteps remaining in this action.
     *
     * @return A 4-element array containing true/false for whether each of the Q, W, O, and P keys are pressed in
     * this action.
     */
    public boolean[] peek() {
        return keysPressed;
    }

    public Keys getKeys() {
        return Action.booleansToKeys(peek());
    }

    /**
     * Check whether this action is finished (i.e. internal step counter hit zero).
     *
     * @return Whether this action is finished.
     */
    public boolean hasNext() {
        return timestepsRemaining > 0;
    }

    /**
     * Reset the number of timesteps in this action remaining. Do this before repeating an action.
     **/
    public void reset() {
        if (!isExecutableCopy) throw new RuntimeException("Tried to reset the base copy of an action." +
                "This is not inherently wrong, but it will do nothing. Could indicate logic flaws in the caller.");
        timestepsRemaining = timestepsTotal;
    }

    /**
     * Get the number of timesteps left to hold this key combination.
     *
     * @return The number of timesteps remaining in this action.
     */
    @JsonIgnore
    public int getTimestepsRemaining() {
        return timestepsRemaining;
    }

    /**
     * Get the total duration of this action in timesteps.
     *
     * @return Total duration of this action (timesteps).
     */
    @JsonProperty("duration")
    public int getTimestepsTotal() {
        return timestepsTotal;
    }

    /**
     * Check if this action is equal to another in regards to keypresses and durations. Completely overrides default
     * equals, so when doing ArrayList checks, this will be the only way to judge. Note that the game.action do not need
     * to have the same number of timesteps remaining to be judged as equal as long as their timestep totals and keys
     * are the same.
     *
     * @param other An action to check whether it is equivalent to this action.
     * @return Whether the other action is equivalent to this one.
     */
    @Override
    public boolean equals(Object other) {
        if((other == null) || (other.getClass() != this.getClass())) {
            return false;
        }
        Action otherAction = (Action) other;

        EqualsBuilder builder = new EqualsBuilder();
        builder.append(keysPressed, otherAction.keysPressed);
        builder.append(getTimestepsTotal(), otherAction.getTimestepsTotal());
        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(keysPressed);
        builder.append(getTimestepsTotal());
        return builder.toHashCode();
    }

    /**
     * Return a string with the current action keys, total time to hold, and time remaining. This method does not
     * print, it just returns the string for the caller to use.
     *
     * @return String of information about this action.
     */
    @Override
    public String toString() {
        String reportString = " Keys pressed: ";
        reportString += keysPressed[0] ? "Q" : "";
        reportString += keysPressed[1] ? "W" : "";
        reportString += keysPressed[2] ? "O" : "";
        reportString += keysPressed[3] ? "P" : "";

        reportString += "; Timesteps elapsed/total: " + timestepsRemaining + "/" + timestepsTotal;

        return reportString;
    }

    /**
     * Get a copy of this action. This avoid multi-threading issues.
     *
     * @return A poll-able copy of this Action with all timesteps of the duration remaining.
     */
    @JsonIgnore
    public synchronized Action getCopy() {
        Action copiedAction = new Action(timestepsTotal, keysPressed);
        copiedAction.isExecutableCopy = true;
        return copiedAction;
    }

    /**
     * Is this a mutable copy of the original action? Important if we plan to use this as a pollable queue. If the
     * action is not mutable, then you must get a copy with {@link Action#getCopy()}.
     *
     * @return Returns whether this action can be polled. If false, then it is the original version of the action.
     */
    @JsonIgnore
    public boolean isMutable() {
        return isExecutableCopy;
    }

    /**
     * Take a list of game.action and combine adjacent game.action which have the same keypresses.
     * These mostly arise when doing control on a timestep-by-timestep basis. Only timestepsTotal are
     * used. Timesteps remaining are not preserved. 0-duration game.action are squashed away.
     * An empty array input or one containing nothing but 0 length game.action will produce an exception.
     *
     * @param inActions A list of game.action which we wish to consolidate.
     * @return A new list of game.action which is the consolidated version of the input action list.
     * @throws IllegalArgumentException When trying to consolidate a list of game.action containing nothing but 0-length
     *                                  game.action.
     */
    public static List<Action> consolidateActions(List<Action> inActions) {
        List<Action> outActions = new ArrayList<>();

        // Single element input.
        if (inActions.size() == 1) {
            if (inActions.get(0).getTimestepsTotal() == 0) { // Single element input + 0-duration -> exception.
                throw new IllegalArgumentException("Input action list had only one element, and this one element had " +
                        "0 duration.");
            }
            outActions.add(inActions.get(0));
            return outActions;
        }

        int consolidations = 0;
        // Combine adjacent same button game.action.
        for (int i = 0; i < inActions.size() - 1; ) {
            Action a1 = inActions.get(i);
            Action a2 = inActions.get(i + 1);

            if (a1.getTimestepsTotal() == 0) {
                // Eliminate 0-duration game.action.
                i++;
                if (inActions.size() - 1 == i && a2.getTimestepsTotal() != 0) outActions.add(a2);
            } else if (Arrays.equals(a1.peek(), a2.peek())) {
                outActions.add(new Action(a1.getTimestepsTotal() + a2.getTimestepsTotal(), a1.peek()));
                consolidations++;
                i += 2;
            } else {
                outActions.add(a1);
                i++;
                if (inActions.size() - 1 == i && a2.getTimestepsTotal() != 0) outActions.add(a2);
            }
        }

        // Recurse until no more combinations are made.
        if (consolidations == 0) {
            if (outActions.isEmpty()) {
                throw new IllegalArgumentException("Tried to consolidate a multi-element list of Actions. All had " +
                        "0-duration, so consolidation does not make sense.");
            }
            return outActions;
        } else {
            return consolidateActions(outActions);
        }
    }

    /**
     * Get a one-hot, 9-element representation of a valid key combination for use in neural networks.
     * @param keys One of the 9 valid key combinations.
     * @return One-hot, 9-element representation of a key combination.
     */
    @SuppressWarnings("WeakerAccess")
    public static float[] keysToOneHot(Keys keys) {
        return labelsToOneHot.get(keys);
    }

    /**
     * Get the boolean representation of the keys pressed from the enum label of the keys.
     * @param keys Keys representing the command.
     * @return 4-element boolean array for keys pressed (QWOP order).
     */
    public static boolean[] keysToBooleans(Keys keys) {
        return labelsToButtons.get(keys);
    }

    /**
     * Given boolean key representation, get the enum representation for that key combination.
     * @param q Is Q pressed?
     * @param w Is W pressed?
     * @param o Is O pressed?
     * @param p Is P pressed?
     * @return The enum Keys representation of that command.
     */
    @SuppressWarnings("WeakerAccess")
    public static Keys booleansToKeys(boolean q, boolean w, boolean o, boolean p) {
        if (q) {
            if (o) {
                return Keys.qo;
            }else if (p) {
                return Keys.qp;
            }else {
                return Keys.q;
            } // QW is not valid and will just do Q anyway
        } else if (w) {
            if (o) {
                return Keys.wo;
            }else if (p) {
                return Keys.wp;
            } else {
                return Keys.w;
            }
        } else if (o) {
            return Keys.o;
        } else if (p) {
            return Keys.p;
        } else {
            return Keys.none;
        }
    }

    /**
     * See {@link Action#booleansToKeys(boolean, boolean, boolean, boolean)}.
     * @param command 4-element boolean representation of keys pressed.
     * @return The enum key representation of that command.
     */
    @SuppressWarnings("WeakerAccess")
    public static Keys booleansToKeys(boolean[] command) {
        return booleansToKeys(validateBooleanKeys(command)[0], command[1], command[2], command[3]);
    }

    /**
     * Validate the length of a boolean keypress array. Throws an {@link IllegalArgumentException} if not 4 elements.
     * @param keys Keys pressed. Should have 4 elements.
     * @return The exact given array.
     */
    private static boolean[] validateBooleanKeys(boolean[] keys) {
        if (keys.length != 4)
            throw new IllegalArgumentException("A QWOP action should have booleans for exactly 4 keys. Tried to " +
                    "create one with a boolean array of size: " + keys.length);
        return keys;
    }
}
