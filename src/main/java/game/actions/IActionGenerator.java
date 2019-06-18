package game.actions;

import tree.node.NodeQWOPExplorableBase;

import java.util.Set;

/**
 * An IActionGenerator determines which {@link ActionList} should be assigned to a node as potential
 * child nodes to explore.
 *
 * @author matt
 */
public interface IActionGenerator {
    /**
     * Get an {@link ActionList} of potential game.actions to explore from a newly created node as its potential children.
     *
     * @param parentNode Node for which we want to pick potential child game.actions.
     * @return A set of game.actions to try as potential children.
     */
    ActionList getPotentialChildActionSet(NodeQWOPExplorableBase<?> parentNode);

    /**
     * Get a set of all possible game.actions which this generator could return.
     * @return All possibly generated Actions.
     */
    Set<Action> getAllPossibleActions();
}
