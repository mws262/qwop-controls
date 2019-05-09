package actions;

import tree.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * ActionGenerator which is defined for a single ActionList which is always returned when queried.
 *
 * @author matt
 */
public class ActionGenerator_FixedActions implements IActionGenerator {

    private final ActionList fixedActionList;

    public ActionGenerator_FixedActions(ActionList fixedActionList) {
        this.fixedActionList = fixedActionList;
    }

    @Override
    public ActionList getPotentialChildActionSet(Node parentNode) {
        return fixedActionList.getCopy();
    }

    @Override
    public Set<Action> getAllPossibleActions() {
        return new HashSet<>(fixedActionList);
    }
}
