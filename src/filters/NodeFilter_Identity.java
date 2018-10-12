package filters;

import java.util.List;

import main.Node;

/**
 * Filter which accepts all nodes and rejects none. Useful as a placeholder or as a debugging tool.
 *
 * @author matt
 */
public class NodeFilter_Identity implements INodeFilter {

    @Override
    public void filter(List<Node> nodes) {}
}
