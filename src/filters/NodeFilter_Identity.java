package filters;

import java.util.List;

import main.INodeFilter;
import main.Node;

/**
 * Filter which accepts all nodes and rejects none.
 *
 * @author matt
 */
public class NodeFilter_Identity implements INodeFilter {

    @Override
    public boolean filter(Node node) {
        return true;
    }

    @Override
    public void filter(List<Node> nodes) {
    }

}
