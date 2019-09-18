package game.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import distributions.Distribution;
import distributions.Distribution_Equal;
import game.qwop.CommandQWOP;
import tree.node.NodeGameExplorableBase;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Gives the same choice of game.command at every node, with the exception that a new Node must not be given the same key
 * combination options as its parent (i.e. two nodes in a row shouldn't be QP).
 *
 * @author matt
 */
public class ActionGenerator_UniformNoRepeats<C extends Command<?>> implements IActionGenerator<C> {

    private final List<ActionList<C>> allActionLists = new ArrayList<>();

    @SafeVarargs
    public ActionGenerator_UniformNoRepeats(ActionList<C> ... actions) {
        allActionLists.addAll(Arrays.asList(actions));
    }

    public ActionGenerator_UniformNoRepeats(List<ActionList<C>> actionLists) {
        allActionLists.addAll(actionLists);
    }

    @JsonCreator
    public ActionGenerator_UniformNoRepeats(@JsonProperty("allActionLists") Collection<ActionList<C>> actions) {
        allActionLists.addAll(actions);
    }

    @Override
    public ActionList<C> getPotentialChildActionSet(NodeGameExplorableBase<?, C, ?> parentNode) {
        ActionList<C> as = new ActionList<>(new Distribution_Equal<>());
        for (ActionList<C> allActionList : allActionLists) {
            if (parentNode.getTreeDepth() == 0 || !allActionList.contains(parentNode.getAction())) { // Get all
                // except the set that the parent had.
                as.addAll(allActionList);
            }
        }
        return as;
    }

    @Override
    @JsonIgnore
    public Set<Action<C>> getAllPossibleActions() {
        Set<Action<C>> allActions = new HashSet<>();
        for (ActionList<C> as : allActionLists) {
            allActions.addAll(as);
        }
        return allActions;
    }

    @SuppressWarnings("unused")
    public List<ActionList<C>> getAllActionLists() {
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

    public static ActionGenerator_UniformNoRepeats<CommandQWOP> makeDefaultGenerator() {
        Distribution<Action<CommandQWOP>> distribution = new Distribution_Equal<>();
        List<ActionList<CommandQWOP>> allActionLists = new ArrayList<>();
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

        return new ActionGenerator_UniformNoRepeats<>(allActionLists);
    }
}
