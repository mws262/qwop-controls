package main;

/**
 * An IActionGenerator determines which {@link ActionSet} should be assigned to {@link Node nodes} as potential
 * child nodes to explore.
 *
 * @author matt
 */
public interface IActionGenerator {
    /**
     * Get an {@link ActionSet} of potential actions to explore from a newly created node as its potential children.
     *
     * @param parentNode Node for which we want to pick potential child actions.
     * @return A set of actions to try as potential children.
     */
    ActionSet getPotentialChildActionSet(Node parentNode);
}
