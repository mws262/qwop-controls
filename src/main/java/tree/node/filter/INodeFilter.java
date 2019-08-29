package tree.node.filter;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameExplorableBase;

import java.util.List;

/**
 * Takes either a single node or a list of nodes and picks which ones to keep based on the implementation's purpose.
 *
 * @author matt
 */
public interface INodeFilter<C extends Command<?>, S extends IState> {

    /**
     * Decide if this node should be included or filtered out. If not overridden, this will default to true.
     *
     * @param node Node to apply filtering rules to.
     * @return Whether this node should be kept. True means keep. False means tree.node.filter out.
     */
    default boolean filter(NodeGameExplorableBase<?, C, S> node){ return true; }

    /**
     * Decide which of these should be kept. Alters the list in place. Default is to call single node tree.node.filter for all
     * elements of the list and remove those which return false.
     *
     * @param nodes A list of nodes to tree.node.filter. This list will be modified in place.
     */
    default void filter(List<NodeGameExplorableBase<?, C, S>> nodes) {
        nodes.removeIf(n -> !filter(n));
    }
}
