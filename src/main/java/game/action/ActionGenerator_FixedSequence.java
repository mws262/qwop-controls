package game.action;

import distributions.Distribution;
import distributions.Distribution_Normal;
import tree.node.NodeQWOPExplorableBase;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Assigns potential action choices to nodes. This fixed sequence version has an {@link ActionList} for
 * each subsequent {@link Action} choice, and cycles through these choices. This is a more general version of the fixed
 * nil/nil - WO - nil/nil - QP sequence I've used for quite awhile. Also, action "exceptions" can be specified to
 * override the normal ActionList in the cycle. This can be useful for defining a different set of game.action for the
 * first few steps when the runner is getting moving.
 *
 * @author matt
 */
public class ActionGenerator_FixedSequence implements IActionGenerator {

    /**
     * These are the possible game.action which are generated in a cycle.
     **/
    private ActionList[] repeatedActions;

    /**
     * These game.action are exceptions which will override the repeated game.action. Key is the
     * tree depth this applies to. Value is the set of Actions. The keySequence booleans
     * are still obeyed. Usually exceptions are at the beginning of a sequence.
     */
    private final Map<Integer, ActionList> actionExceptions;

    /**
     * How many game.action are in one repeated cycle.
     **/
    private final int cycleLength;

    /**
     * This action generator will produce the nil-WO-nil-QP sequence. actionRepeats will be chosen in repeating order
     * like 1234,1234,1234, or 123,123,123, etc. Note that this starts from tree root EVEN if there are action
     * exceptions. However, action exceptions for specified tree depths will be chosen over the repeating game.action if
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
        if (actionExceptions != null) {
            for (ActionList as : actionExceptions.values()) {
                allActions.addAll(as);
            }
        }
        return allActions;
    }

    /**
     * Assign the correct generator of game.action based on the baseline options and exceptions.
     * Will assign a broader set of options for "recovery" at the specified starting depth.
     * Pass -1 to disable this.
     */
    public static IActionGenerator makeDefaultGenerator(int recoveryExceptionStart) {
        /* Space of allowed game.action to sample */
        //Distribution<Action> uniform_dist = new Distribution_Equal();

        /* Repeated action 1 -- no keys pressed. */
        Distribution<Action> dist1 = new Distribution_Normal(10f, 2f);
        ActionList actionList1 = ActionList.makeActionList(IntStream.range(1, 25).toArray(), new boolean[]{false, false, false,
                false}, dist1);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> dist2 = new Distribution_Normal(39f, 3f);
        ActionList actionList2 = ActionList.makeActionList(IntStream.range(20, 60).toArray(), new boolean[]{false, true, true,
                false}, dist2);

        /* Repeated action 3 -- No keys pressed. */
        Distribution<Action> dist3 = new Distribution_Normal(10f, 2f);
        ActionList actionList3 = ActionList.makeActionList(IntStream.range(1, 25).toArray(), new boolean[]{false, false, false,
                false}, dist3);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> dist4 = new Distribution_Normal(39f, 3f);
        ActionList actionList4 = ActionList.makeActionList(IntStream.range(20, 60).toArray(), new boolean[]{true, false, false,
                true}, dist4);

        ActionList[] repeatedActions = new ActionList[]{actionList1, actionList2, actionList3, actionList4};

        /////// Action Exceptions for starting up. ////////
        /* Repeated action exceptions 1 -- no keys pressed. */
        Distribution<Action> distE1 = new Distribution_Normal(5f, 1f);
        ActionList actionListE1 = ActionList.makeActionList(IntStream.range(1, 25).toArray(), new boolean[]{false, false, false,
                false}, distE1);

        /*  Repeated action exceptions 2 -- W-O pressed */
        Distribution<Action> distE2 = new Distribution_Normal(34f, 2f);
        ActionList actionListE2 = ActionList.makeActionList(IntStream.range(30, 50).toArray(), new boolean[]{false,
                true, true,
                false}, distE2);

        /*  Repeated action exceptions 3 -- no keys pressed. */
        Distribution<Action> distE3 = new Distribution_Normal(5, 2f);
        ActionList actionListE3 = ActionList.makeActionList(IntStream.range(1, 20).toArray(), new boolean[]{false,
                false, false,
                false}, distE3);

        /*  Repeated action exceptions 4 -- Q-P pressed */
        Distribution<Action> distE4 = new Distribution_Normal(25f, 2f);
        ActionList actionListE4 = ActionList.makeActionList(IntStream.range(15, 30).toArray(), new boolean[]{true,
                false, false,
                true}, distE4);

        /////// Action Exceptions for recovery. ////////
        /*  Repeated action 1 and 3 -- Nothing pressed */
        Distribution<Action> distFalseFalse = new Distribution_Normal(10f, 2f);
        ActionList actionListFalseFalse = ActionList.makeActionList(IntStream.range(1, 50).toArray(), new boolean[]{false, false,
                false, false}, distFalseFalse);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> distWO = new Distribution_Normal(39f, 3f);
        ActionList actionListWO = ActionList.makeActionList(IntStream.range(1, 70).toArray(), new boolean[]{false, true, true,
                false}, distWO);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> distQP = new Distribution_Normal(39f, 3f);
        ActionList actionListQP = ActionList.makeActionList(IntStream.range(1, 70).toArray(), new boolean[]{true, false, false,
                true}, distQP);

        Map<Integer, ActionList> actionExceptions = new HashMap<>();
        actionExceptions.put(0, actionListE1);
        actionExceptions.put(1, actionListE2);
        actionExceptions.put(2, actionListE3);
        actionExceptions.put(3, actionListE4);

        // Put the recovery exceptions in the right spot.
        if (recoveryExceptionStart >= 0) {
            for (int i = 0; i < 4; i++) {
                int sequencePos = (recoveryExceptionStart + i) % 4;

                switch (sequencePos) {
                    case 0:
                        actionExceptions.put(recoveryExceptionStart + i, actionListFalseFalse);
                        break;
                    case 1:
                        actionExceptions.put(recoveryExceptionStart + i, actionListWO);
                        break;
                    case 2:
                        actionExceptions.put(recoveryExceptionStart + i, actionListFalseFalse);
                        break;
                    case 3:
                        actionExceptions.put(recoveryExceptionStart + i, actionListQP);
                        break;
                    default:
                        throw new IllegalStateException("unknown sequence position.");
                }
            }
        }
        // Define the specific way that these allowed game.action are assigned as potential options for nodes.
        return new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
    }

    public static IActionGenerator makeExtendedGenerator(int recoveryExceptionStart) {
        /* Space of allowed game.action to sample */
        //Distribution<Action> uniform_dist = new Distribution_Equal();

        /* Repeated action 1 -- no keys pressed. */
        Distribution<Action> dist1 = new Distribution_Normal(10f, 2f);
        ActionList actionList1 = ActionList.makeActionList(IntStream.range(2, 25).toArray(), new boolean[]{false, false,
                false,
                false}, dist1);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> dist2 = new Distribution_Normal(25, 3f);
        ActionList actionList2 = ActionList.makeActionList(IntStream.range(5, 35).toArray(), new boolean[]{false, true,
                true,
                false}, dist2);

        /* Repeated action 3 -- W-O pressed */
        Distribution<Action> dist3 = new Distribution_Normal(10f, 2f);
        ActionList actionList3 = ActionList.makeActionList(IntStream.range(2, 25).toArray(), new boolean[]{false, false,
                false,
                false}, dist3);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> dist4 = new Distribution_Normal(25, 3f);
        ActionList actionList4 = ActionList.makeActionList(IntStream.range(5, 35).toArray(), new boolean[]{true, false,
                false,
                true}, dist4);

        ActionList[] repeatedActions = new ActionList[]{actionList1, actionList2, actionList3, actionList4};

        /////// Action Exceptions for starting up. ////////
        /* Repeated action exceptions 1 -- no keys pressed. */
        Distribution<Action> distE1 = new Distribution_Normal(5f, 1f);
        ActionList actionListE1 = ActionList.makeActionList(IntStream.range(1, 35).toArray(), new boolean[]{false, false,
                false,
                false}, distE1);

        /*  Repeated action exceptions 2 -- W-O pressed */
        Distribution<Action> distE2 = new Distribution_Normal(34f, 2f);
        ActionList actionListE2 = ActionList.makeActionList(IntStream.range(5, 45).toArray(), new boolean[]{false, true,
                true,
                false}, distE2);

        /*  Repeated action exceptions 3 -- no keys pressed. */
        Distribution<Action> distE3 = new Distribution_Normal(24f, 2f);
        ActionList actionListE3 = ActionList.makeActionList(IntStream.range(1, 35).toArray(), new boolean[]{false, false,
                false,
                false}, distE3);

        /*  Repeated action exceptions 4 -- Q-P pressed */
        Distribution<Action> distE4 = new Distribution_Normal(49f, 2f);
        ActionList actionListE4 = ActionList.makeActionList(IntStream.range(5, 45).toArray(), new boolean[]{true, false,
                false,
                true}, distE4);

        /////// Action Exceptions for recovery. ////////
        /*  Repeated action 1 and 3 -- Nothing pressed */
        Distribution<Action> distFalseFalse = new Distribution_Normal(10f, 2f);
        ActionList actionListFalseFalse = ActionList.makeActionList(IntStream.range(1, 50).toArray(), new boolean[]{false, false,
                false, false}, distFalseFalse);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> distWO = new Distribution_Normal(39f, 3f);
        ActionList actionListWO = ActionList.makeActionList(IntStream.range(1, 70).toArray(), new boolean[]{false, true, true,
                false}, distWO);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> distQP = new Distribution_Normal(39f, 3f);
        ActionList actionListQP = ActionList.makeActionList(IntStream.range(1, 70).toArray(), new boolean[]{true, false, false,
                true}, distQP);

        Map<Integer, ActionList> actionExceptions = new HashMap<>();
        actionExceptions.put(0, actionListE1);
        actionExceptions.put(1, actionListE2);
        actionExceptions.put(2, actionListE3);
        actionExceptions.put(3, actionListE4);

        // Put the recovery exceptions in the right spot.
        if (recoveryExceptionStart >= 0) {
            for (int i = 0; i < 4; i++) {
                int sequencePos = (recoveryExceptionStart + i) % 4;

                switch (sequencePos) {
                    case 0:
                        actionExceptions.put(recoveryExceptionStart + i, actionListFalseFalse);
                        break;
                    case 1:
                        actionExceptions.put(recoveryExceptionStart + i, actionListWO);
                        break;
                    case 2:
                        actionExceptions.put(recoveryExceptionStart + i, actionListFalseFalse);
                        break;
                    case 3:
                        actionExceptions.put(recoveryExceptionStart + i, actionListQP);
                        break;
                    default:
                        throw new RuntimeException("unknown sequence position.");
                }
            }
        }
        // Define the specific way that these allowed game.action are assigned as potential options for nodes.
        return new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
    }
}
