package game.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains the keypresses and durations for a single command. Works like an uneditable {@link java.util.Queue}. Call
 * {@link Action#poll()} to get the keystrokes at each timestep execution. Call {@link Action#hasNext()} to make
 * sure there are timesteps left in this command. Call {@link Action#reset()} to restore the duration of the command back to
 * original.
 * <p>
 * Note that when constructed, game.command may not be changed or polled. They serve as an "immutable" backup of the
 * command. To get a pollable version of the {@link Action}, call {@link Action#getCopy()}.
 *
 * @author Matt
 * @see java.util.Queue
 * @see ActionList
 */
public class Action<C extends Command<?>> implements Comparable<Action<C>>, Serializable {

    private static final long serialVersionUID = 2L;

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
    private final C command;

    /**
     * Is this the immutable original or a derived, mutable copy. A little bit hacky, but a way of avoiding threading
     * issues without major modifications.
     **/
    private boolean isExecutableCopy = false;

    /**
     * Create an command containing the time to hold and the key combination.
     *
     * @param totalTimestepsToHold Number of timesteps to hold the keys associated with this Action.
     */
    @JsonCreator
    public Action(
            @JsonProperty("duration") int totalTimestepsToHold,
            @JsonProperty("command") C command) {
        if (totalTimestepsToHold < 0)
            throw new IllegalArgumentException("New QWOP Action must have non-negative duration. Given: " + totalTimestepsToHold);

        this.timestepsTotal = totalTimestepsToHold;
        timestepsRemaining = timestepsTotal;
        this.command = command;
    }

    /**
     * Return the keys for this command and decrement the timestepsRemaining.
     */
    public C poll() {
        if (!isExecutableCopy)
            throw new RuntimeException("Trying to execute the base version of the Action. Due to multi-threading, " +
                    "this REALLY screws with the counters in the command. Call getCopy for the version you should use.");
        if (timestepsRemaining <= 0)
            throw new IndexOutOfBoundsException("Tried to poll an command which was already completed. Call hasNext() " +
                    "to check before calling poll().");
        timestepsRemaining--;

        return command;
    }

    /**
     * Return the keys pressed in this command without changing the number of timesteps remaining in this command.
     *
     * @return A 4-element array containing true/false for whether each of the Q, W, O, and P keys are pressed in
     * this command.
     */
    public C peek() {
        return command;
    }

    /**
     * Check whether this command is finished (i.e. internal step counter hit zero).
     *
     * @return Whether this command is finished.
     */
    public boolean hasNext() {
        return timestepsRemaining > 0;
    }

    /**
     * Reset the number of timesteps in this command remaining. Do this before repeating an command.
     **/
    public void reset() {
        if (!isExecutableCopy) throw new RuntimeException("Tried to resetGame the base copy of an command." +
                "This is not inherently wrong, but it will do nothing. Could indicate logic flaws in the caller.");
        timestepsRemaining = timestepsTotal;
    }

    /**
     * Get the number of timesteps left to hold this key combination.
     *
     * @return The number of timesteps remaining in this command.
     */
    @JsonIgnore
    public int getTimestepsRemaining() {
        return timestepsRemaining;
    }

    /**
     * Get the total duration of this command in timesteps.
     *
     * @return Total duration of this command (timesteps).
     */
    @JsonProperty("duration")
    public int getTimestepsTotal() {
        return timestepsTotal;
    }

    @JsonProperty("command")
    public C getCommand() {
        return command;
    }

    /**
     * Check if this command is equal to another in regards to keypresses and durations. Completely overrides default
     * equals, so when doing ArrayList checks, this will be the only way to judge. Note that the game.command do not need
     * to have the same number of timesteps remaining to be judged as equal as long as their timestep totals and keys
     * are the same.
     *
     * @param other An command to check whether it is equivalent to this command.
     * @return Whether the other command is equivalent to this one.
     */
    @Override
    public boolean equals(Object other) {
        if((other == null) || (other.getClass() != this.getClass())) {
            return false;
        }
        Action otherAction = (Action) other;

        EqualsBuilder builder = new EqualsBuilder();
        builder.append(command, otherAction.command);
        builder.append(getTimestepsTotal(), otherAction.getTimestepsTotal());
        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(command);
        builder.append(getTimestepsTotal());
        return builder.toHashCode();
    }

    /**
     * First sort by keys pressed, then by timestep durations.
     */
    @Override
    public int compareTo(@NotNull Action<C> o) {
        Objects.requireNonNull(o);
        int commandCompare = getCommand().compareTo(o.getCommand());
        if (commandCompare != 0) {
            return commandCompare;
        } else {
            return Integer.compare(getTimestepsTotal(), o.getTimestepsTotal());
        }
    }

    /**
     * Return a string with the current command keys, total time to hold, and time remaining. This method does not
     * print, it just returns the string for the caller to use.
     *
     * @return String of information about this command.
     */
    @Override
    public String toString() {
        return command.toString() + "; Timesteps elapsed/total: " + timestepsRemaining + "/" + timestepsTotal;
    }

    /**
     * Get a copy of this command. This avoid multi-threading issues.
     *
     * @return A poll-able copy of this Action with all timesteps of the duration remaining.
     */
    @JsonIgnore
    public synchronized Action<C> getCopy() {
        Action<C> copiedAction = new Action<>(timestepsTotal, command);
        copiedAction.isExecutableCopy = true;
        return copiedAction;
    }

    /**
     * Is this a mutable copy of the original command? Important if we plan to use this as a pollable queue. If the
     * command is not mutable, then you must get a copy with {@link Action#getCopy()}.
     *
     * @return Returns whether this command can be polled. If false, then it is the original version of the command.
     */
    @JsonIgnore
    public boolean isMutable() {
        return isExecutableCopy;
    }

    /**
     * Take a list of game.command and combine adjacent game.command which have the same keypresses.
     * These mostly arise when doing control on a timestep-by-timestep basis. Only timestepsTotal are
     * used. Timesteps remaining are not preserved. 0-duration game.command are squashed away.
     * An empty array input or one containing nothing but 0 length game.command will produce an exception.
     *
     * @param inActions A list of game.command which we wish to consolidate.
     * @return A new list of game.command which is the consolidated version of the input command list.
     * @throws IllegalArgumentException When trying to consolidate a list of game.command containing nothing but 0-length
     *                                  game.command.
     */
    public static <C extends Command<?>> List<Action<C>> consolidateActions(List<Action<C>> inActions) {
        List<Action<C>> outActions = new ArrayList<>();

        // Single element input.
        if (inActions.size() == 1) {
            if (inActions.get(0).getTimestepsTotal() == 0) { // Single element input + 0-duration -> exception.
                throw new IllegalArgumentException("Input command list had only one element, and this one element had " +
                        "0 duration.");
            }
            outActions.add(inActions.get(0));
            return outActions;
        }

        int consolidations = 0;
        // Combine adjacent same button game.command.
        for (int i = 0; i < inActions.size() - 1; ) {
            Action<C> a1 = inActions.get(i);
            Action<C> a2 = inActions.get(i + 1);

            if (a1.getTimestepsTotal() == 0) {
                // Eliminate 0-duration game.command.
                i++;
                if (inActions.size() - 1 == i && a2.getTimestepsTotal() != 0) outActions.add(a2);
            } else if (a1.peek().equals(a2.peek())) {
                outActions.add(new Action<>(a1.getTimestepsTotal() + a2.getTimestepsTotal(), a1.peek()));
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
}
