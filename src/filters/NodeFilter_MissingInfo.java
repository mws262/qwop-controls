package filters;

import java.util.Iterator;
import java.util.List;

import main.INodeFilter;
import main.Node;

/**
 * Very new nodes might not have state or action assigned yet. This will filter those out.
 * @author matt
 *
 */
public class NodeFilter_MissingInfo implements INodeFilter {

	@Override
	public boolean filter(Node node) { // true means keep
		return !(node.state == null || node.action == null);
	}

	@Override
	public void filter(List<Node> nodes) {
		Iterator<Node> iter = nodes.iterator();
		
		while (iter.hasNext()) {
			Node n = iter.next();
			
			if (!filter(n)) {
				iter.remove();
			}
		}
	}
}
