package filters;

import java.util.List;

import tree.Node;

/**
 * Filter which accepts all nodes and rejects none. Useful as a placeholder or as a debugging tool.
 *
 * @author matt
 */
@SuppressWarnings("unused")
public class NodeFilter_Identity implements INodeFilter {

    @Override
    public void filter(List<Node> nodes) {}
}
