package tree;

import actions.Action;
import actions.ActionSet;
import actions.IActionGenerator;

public class NodeRollout extends Node {

    public static IActionGenerator rolloutActionGenerator;

    public NodeRollout(Node parent, Action action) {
        super(parent, action, false);
    }
    /**
     * If we've assigned a potentialActionGenerator, this can auto-add potential child actions. Ignores duplicates.
     */
    @Override
    synchronized void autoAddUncheckedActions() {
        // If we've set rules to auto-select potential children, do so.
        if (rolloutActionGenerator != null) {
            ActionSet potentialActions = rolloutActionGenerator.getPotentialChildActionSet(this);

            // If no unchecked actions have been previously added (must have assigned a sampling distribution to do so),
            // then just use the new one outright.
            if (uncheckedActions == null) {
                uncheckedActions = potentialActions;
            } else { // Otherwise, just use the existing distribution, but add the new actions anyway.
                for (Action potentialAction : potentialActions) {
                    if (!uncheckedActions.contains(potentialAction)) {
                        uncheckedActions.add(potentialAction);
                    }
                }
            }
        }
    }

}
