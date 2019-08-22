package game.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        allActionLists.addAll(Arrays.asList(actions));
    }

    @JsonCreator
    public ActionGenerator_UniformNoRepeats(@JsonProperty("allActionLists") Collection<ActionList> actions) {
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
    @JsonIgnore
    public Set<Action> getAllPossibleActions() {
        Set<Action> allActions = new HashSet<>();
        for (ActionList as : allActionLists) {
            allActions.addAll(as);
        }
        return allActions;
    }

    public List<ActionList> getAllActionLists() {
        return allActionLists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionGenerator_UniformNoRepeats that = (ActionGenerator_UniformNoRepeats) o;
        return allActionLists.equals(that.allActionLists);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allActionLists);
    }

    public static ActionGenerator_UniformNoRepeats makeDefaultGenerator() {
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
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 15).toArray(), CommandQWOP.NONE, distribution));
        //ffff*
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 10).toArray(), CommandQWOP.Q, distribution));
        //tfff
        //allActionLists.add(ActionList.makeActionList(IntStream.range(2, 25).toArray(), CommandQWOP.QO.,
        // distribution));
        //tftf
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 30).toArray(), CommandQWOP.QP, distribution));
        //tfft*
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 10).toArray(), CommandQWOP.W, distribution));
        //ftff
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 30).toArray(), CommandQWOP.WO, distribution));
        //fttf*
        // allActionLists.add(ActionList.makeActionList(IntStream.range(2, 25).toArray(), CommandQWOP.WP,
        // distribution));
        //ftft
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 10).toArray(), CommandQWOP.O, distribution));
        //fftf
        allActionLists.add(ActionList.makeActionList(IntStream.range(2, 10).toArray(), CommandQWOP.P, distribution));
        //ffft

        return new ActionGenerator_UniformNoRepeats(allActionLists);
    }
}
