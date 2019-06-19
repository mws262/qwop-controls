package game.action;

import distributions.Distribution_Equal;
import tree.node.NodeQWOPExplorableBase;

import java.util.HashSet;
import java.util.Set;

public class ActionGenerator_Null implements IActionGenerator {
    private ActionList nullActionList = new ActionList(new Distribution_Equal());
    private Set<Action> nullActionSet = new HashSet<>();

    @Override
    public ActionList getPotentialChildActionSet(NodeQWOPExplorableBase<?> parentNode) {
        return nullActionList;
    }

    @Override
    public Set<Action> getAllPossibleActions() {
        return nullActionSet;
    }
}
