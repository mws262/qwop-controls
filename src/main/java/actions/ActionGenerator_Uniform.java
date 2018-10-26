package actions;

import distributions.Distribution;
import distributions.Distribution_Equal;
import tree.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Gives the same choice of actions at every node, with the exception that a new Node must not be given the same key
 * combination options as its parent (i.e. two nodes in a row shouldn't be QP).
 *
 * @author matt
 */
public class ActionGenerator_Uniform implements IActionGenerator {

    private List<ActionSet> allActionSets;

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
        allActionSets = new ArrayList<>();
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[0], distribution));//ffff*
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[1], distribution));//tfff
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[2], distribution));//tftf
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(18, 45).toArray(), keyRange[3], distribution));//tfft*
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[4], distribution));//ftff
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(18, 45).toArray(), keyRange[5], distribution));//fttf*
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[6], distribution));//ftft
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[7], distribution));//fftf
        allActionSets.add(ActionSet.makeActionSet(IntStream.range(5, 25).toArray(), keyRange[8], distribution));//ffft
    }

    @Override
    public ActionSet getPotentialChildActionSet(Node parentNode) {
        ActionSet as = new ActionSet(new Distribution_Equal());
        for (ActionSet allActionSet : allActionSets) {
            if (parentNode.getTreeDepth() == 0 || !allActionSet.contains(parentNode.getAction())) { // Get all
                // except the set that the parent had.
                as.addAll(allActionSet);
            }
        }
        return as;
    }
}
