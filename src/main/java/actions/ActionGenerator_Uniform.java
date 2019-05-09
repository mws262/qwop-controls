package actions;

import distributions.Distribution;
import distributions.Distribution_Equal;
import tree.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Gives the same choice of actions at every node, with the exception that a new Node must not be given the same key
 * combination options as its parent (i.e. two nodes in a row shouldn't be QP).
 *
 * @author matt
 */
public class ActionGenerator_Uniform implements IActionGenerator {

    private List<ActionList> allActionLists;

    public ActionGenerator_Uniform() {
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
        allActionLists = new ArrayList<>();
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[0], distribution));//ffff*
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[1], distribution));//tfff
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[2], distribution));//tftf
        allActionLists.add(ActionList.makeActionSet(IntStream.range(10, 45).toArray(), keyRange[3], distribution));//tfft*
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[4], distribution));//ftff
        allActionLists.add(ActionList.makeActionSet(IntStream.range(10, 45).toArray(), keyRange[5], distribution));//fttf*
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[6], distribution));//ftft
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[7], distribution));//fftf
        allActionLists.add(ActionList.makeActionSet(IntStream.range(2, 25).toArray(), keyRange[8], distribution));//ffft
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
}
