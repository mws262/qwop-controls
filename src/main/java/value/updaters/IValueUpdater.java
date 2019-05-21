package value.updaters;

import tree.NodeQWOPBase;

/**
 * A rule for updating the estimated value of a {@link tree.NodeQWOPBase} given a node and an update value.
 *
 * @author matt
 */
public interface IValueUpdater {

    /**
     * Provide an updated value for a node.
     * @param valueUpdate An update value.
     * @param node Node to calculate an updated value for.
     * @return The provided node's updated value.
     */
    float update(float valueUpdate, NodeQWOPBase<?> node);
}
