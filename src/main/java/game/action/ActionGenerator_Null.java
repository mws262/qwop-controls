package game.action;

import distributions.Distribution_Equal;
import tree.node.NodeGameExplorableBase;

import java.util.HashSet;
import java.util.Set;

public class ActionGenerator_Null<C extends Command<?>> implements IActionGenerator<C> {
    private ActionList<C> nullActionList = new ActionList<>(new Distribution_Equal<>());
    private Set<Action<C>> nullActionSet = new HashSet<>();

    @Override
    public ActionList<C> getPotentialChildActionSet(NodeGameExplorableBase<?, C> parentNode) {
        return nullActionList;
    }

    @Override
    public Set<Action<C>> getAllPossibleActions() {
        return nullActionSet;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
