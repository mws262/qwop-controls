package filters;

import tree.Node;

import java.util.List;

/**
 * Takes either a single node or a list of nodes and picks which ones to keep based on the implementation's purpose.
 *
 * @author matt
 */
public interface INodeFilter {

    /**
     * Decide if this node should be included or filtered out. If not overridden, this will default to true.
     *
     * @param node Node to apply filtering rules to.
     * @return Whether this node should be kept. True means keep. False means filter out.
     */
    default boolean filter(Node node){ return true; }

    /**
     * Decide which of these should be kept. Alters the list in place. Default is to call single node filter for all
     * elements of the list and remove those which return false.
     *
     * @param nodes A list of nodes to filter. This list will be modified in place.
     */
    default void filter(List<Node> nodes) {
        nodes.removeIf(n -> !filter(n));
    }
}
