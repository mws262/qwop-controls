package game.actions;

import tree.node.NodeQWOPExplorableBase;

import java.util.*;

/**
 * Assigns potential action choices to nodes. This fixed sequence version has an {@link ActionList} for
 * each subsequent {@link Action} choice, and cycles through these choices. This is a more general version of the fixed
 * nil/nil - WO - nil/nil - QP sequence I've used for quite awhile. Also, action "exceptions" can be specified to
 * override the normal ActionList in the cycle. This can be useful for defining a different set of game.actions for the
 * first few steps when the runner is getting moving.
 *
 * @author matt
 */
public class ActionGenerator_FixedSequence implements IActionGenerator {

    /**
     * These are the possible game.actions which are generated in a cycle.
     **/
    private ActionList[] repeatedActions;

    /**
     * These game.actions are exceptions which will override the repeated game.actions. Key is the
     * tree depth this applies to. Value is the set of Actions. The keySequence booleans
     * are still obeyed. Usually exceptions are at the beginning of a sequence.
     */
    private final Map<Integer, ActionList> actionExceptions;

    /**
     * How many game.actions are in one repeated cycle.
     **/
    private final int cycleLength;

    /**
     * This action generator will produce the nil-WO-nil-QP sequence. actionRepeats will be chosen in repeating order
     * like 1234,1234,1234, or 123,123,123, etc. Note that this starts from tree root EVEN if there are action
     * exceptions. However, action exceptions for specified tree depths will be chosen over the repeating game.actions if
     * they are assigned.
     *
     * @param repeatedActions  An array of {@link ActionList}, with one for every choice in the sequence. Each
     *                         subsequent choice (increasing depth in the tree) cycles through the action sets and
     *                         wraps when at the end of the ActionSets.
     * @param actionExceptions A {@link Map} of integer indexes to {@link ActionList}. When the sequences reach a tree
     *                         depth specified in the map, it will use the ActionList corresponding to this index
     *                         rather than the normal ActionList in the cycle.
     */
    public ActionGenerator_FixedSequence(ActionList[] repeatedActions, Map<Integer, ActionList> actionExceptions) {

        if (repeatedActions.length == 0) {
            throw new IllegalArgumentException("There must be at least 1 repeated action. The array was empty.");
        }

        cycleLength = repeatedActions.length;

        this.repeatedActions = repeatedActions;
        this.actionExceptions = actionExceptions;
    }

    public ActionGenerator_FixedSequence(ActionList[] repeatedActions) {
        this(repeatedActions, null);
    }

    @Override
    public ActionList getPotentialChildActionSet(NodeQWOPExplorableBase<?> parentNode) {
        int actionDepth = parentNode.getTreeDepth();

        // Check if this is an exception case.
        if (actionExceptions != null && actionExceptions.containsKey(actionDepth))
            return actionExceptions.get(actionDepth).getCopy();

        // Otherwise, pick based on cycle.
        return repeatedActions[actionDepth % cycleLength].getCopy();
    }

    @Override
    public Set<Action> getAllPossibleActions() {
        Set<Action> allActions = new HashSet<>();
        for (ActionList as : repeatedActions) {
            allActions.addAll(as);
        }
        for (ActionList as : actionExceptions.values()) {
            allActions.addAll(as);
        }
        return allActions;
    }
}
