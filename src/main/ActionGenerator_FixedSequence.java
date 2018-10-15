package main;

import java.util.Map;

public class ActionGenerator_FixedSequence implements IActionGenerator {

    /**
     * These are the possible actions which are generated in a cycle.
     **/
    private ActionSet[] repeatedActions;

    /**
     * These actions are exceptions which will override the repeated actions. Key is the
     * tree depth this applies to. Value is the set of Actions. The keySequence booleans
     * are still obeyed. Usually exceptions are at the beginning of a sequence.
     */
    private final Map<Integer, ActionSet> actionExceptions;

    /**
     * How many actions are in one repeated cycle.
     **/
    private final int cycleLength;

    /**
     * This action generator will produce the nil-WO-nil-QP sequence.
     * actionRepeats will be chosen in repeating order like 1234,1234,1234, or 123,123,123, etc.
     * Note that this starts from tree root EVEN if there are action exceptions.
     * However, action exceptions for specified tree depths will be chosen over the repeating actions
     * if they are assigned.
     *
     * @param repeatedActions
     * @param actionExceptions
     */
    ActionGenerator_FixedSequence(ActionSet[] repeatedActions, Map<Integer, ActionSet> actionExceptions) {

        cycleLength = repeatedActions.length;

        this.repeatedActions = repeatedActions;
        this.actionExceptions = actionExceptions;
    }

    @Override
    public ActionSet getPotentialChildActionSet(Node parentNode) {
        int actionDepth = parentNode.getTreeDepth();

        // Check if this is an exception case.
        if (actionExceptions != null && actionExceptions.containsKey(actionDepth))
            return actionExceptions.get(actionDepth).clone();

        // Otherwise, pick based on cycle.
        return repeatedActions[actionDepth % cycleLength].clone();
    }
}
