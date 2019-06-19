package game.action;

import distributions.Distribution;
import distributions.Distribution_Equal;
import tree.node.NodeQWOPExplorableBase;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Gives the same choice of game.action at every node, with the exception that a new Node must not be given the same key
 * combination options as its parent (i.e. two nodes in a row shouldn't be QP).
 *
 * @author matt
 */
public class ActionGenerator_UniformNoRepeats implements IActionGenerator {

    private final List<ActionList> allActionLists = new ArrayList<>();

    public ActionGenerator_UniformNoRepeats(ActionList[] actions) {
        for (ActionList a : actions) {
            allActionLists.add(a);
        }
    }

    public ActionGenerator_UniformNoRepeats(Collection<ActionList> actions) {
        allActionLists.addAll(actions);
    }

    @Override
    public ActionList getPotentialChildActionSet(NodeQWOPExplorableBase<?> parentNode) {
        ActionList as = new ActionList(new Distribution_Equal());
        for (ActionList allActionList : allActionLists) {
            if (parentNode.getTreeDepth() == 0 || !allActionList.contains(parentNode.getAction())) { // Get all
                // except the set that the parent had.
                as.addAll(allActionList);
            }
        }
        return as;
    }

    @Override
    public Set<Action> getAllPossibleActions() {
        Set<Action> allActions = new HashSet<>();
        for (ActionList as : allActionLists) {
            allActions.addAll(as);
        }
        return allActions;
    }

    static ActionGenerator_UniformNoRepeats makeDefaultGenerator() {
        // All durations.
        boolean[][] keyRange = new boolean[][]{{false, false, false, false}, // All 9 combinations.
                {true, false, false, false},
                {true, false, true, false},
                {true, false, false, true},
                {false, true, false, false},
                {false, true, true, false},
                {false, true, false, true},
                {false, false, true, false},
                {false, false, false, true}};
        Distribution<Action> distribution = new Distribution_Equal();
        List<ActionList> allActionLists = new ArrayList<>();
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 5).toArray(), keyRange[0], distribution));
        //ffff*
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 5).toArray(), keyRange[1], distribution));//tfff
        //allActionLists.add(ActionList.makeActionList(IntStream.range(2, 25).toArray(), keyRange[2], distribution));
        //tftf
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 45).toArray(), keyRange[3], distribution));
        //tfft*
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 5).toArray(), keyRange[4], distribution));//ftff
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 45).toArray(), keyRange[5], distribution));
        //fttf*
        // allActionLists.add(ActionList.makeActionList(IntStream.range(2, 25).toArray(), keyRange[6], distribution));
        //ftft
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 5).toArray(), keyRange[7], distribution));//fftf
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 5).toArray(), keyRange[8], distribution));//ffft

        return new ActionGenerator_UniformNoRepeats(allActionLists);
    }
}
