package actions;

import tree.Node;

import java.util.Set;

/**
 * An IActionGenerator determines which {@link ActionList} should be assigned to {@link Node nodes} as potential
 * child nodes to explore.
 *
 * @author matt
 */
public interface IActionGenerator {
    /**
     * Get an {@link ActionList} of potential actions to explore from a newly created node as its potential children.
     *
     * @param parentNode Node for which we want to pick potential child actions.
     * @return A set of actions to try as potential children.
     */
    ActionList getPotentialChildActionSet(Node parentNode);

    /**
     * Get a set of all possible actions which this generator could return.
     * @return All possibly generated Actions.
     */
    Set<Action> getAllPossibleActions();

}
