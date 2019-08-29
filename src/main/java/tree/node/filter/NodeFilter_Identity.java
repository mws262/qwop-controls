package tree.node.filter;

import game.action.Command;
import game.state.IState;

/**
 * Filter which accepts all nodes and rejects none. Useful as a placeholder or as a debugging tool.
 *
 * @author matt
 */
@SuppressWarnings("unused")
public class NodeFilter_Identity<C extends Command<?>, S extends IState> implements INodeFilter<C, S> {}
