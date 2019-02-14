package actions;

import tree.Node;

/**
 * ActionGenerator which is defined for a single ActionSet which is always returned when queried.
 *
 * @author matt
 */
public class ActionGenerator_FixedActions implements IActionGenerator {

    private final ActionSet fixedActionSet;

    public ActionGenerator_FixedActions(ActionSet fixedActionSet) {
        this.fixedActionSet = fixedActionSet;
    }

    @Override
    public ActionSet getPotentialChildActionSet(Node parentNode) {
        return fixedActionSet.getCopy();
    }
}
