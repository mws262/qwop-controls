package filters;

import tree.Node;

/**
 * Very new nodes might not have state or action assigned yet. This will filter those out. Note that this will filter
 * the root node out.
 *
 * @author matt
 */
public class NodeFilter_MissingInfo implements INodeFilter {

    @Override
    public boolean filter(Node node) { // true means keep
        return !(node == null || node.isStateUnassigned() || node.action == null);
    }

}
