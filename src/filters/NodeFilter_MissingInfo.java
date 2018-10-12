package filters;

import main.Node;

/**
 * Very new nodes might not have state or action assigned yet. This will filter those out.
 *
 * @author matt
 */
public class NodeFilter_MissingInfo implements INodeFilter {

    @Override
    public boolean filter(Node node) { // true means keep
        return !(!node.isStateAvailable() || node.action == null);
    }

}
