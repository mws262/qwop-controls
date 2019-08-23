package tree.node.filter;

import game.action.Command;

/**
 * Filter which accepts all nodes and rejects none. Useful as a placeholder or as a debugging tool.
 *
 * @author matt
 */
@SuppressWarnings("unused")
public class NodeFilter_Identity<C extends Command<?>> implements INodeFilter<C> {}
