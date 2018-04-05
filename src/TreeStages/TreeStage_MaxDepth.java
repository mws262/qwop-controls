package TreeStages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import main.ISampler;
import main.Node;
import main.TreeStage;

/**
 * Goes until any branch reaches a certain tree depth.
 * 
 * @author matt
 *
 */
public class TreeStage_MaxDepth extends TreeStage {
	private List<Node> leafList = new LinkedList<Node>();
	
	private int maxDepth;
	
	public TreeStage_MaxDepth(ISampler sampler, int maxDepth) {
		this.sampler = sampler;
		this.maxDepth = maxDepth;
	}
	
	@Override
	public List<Node> getResults() {
		List<Node> resultList = new ArrayList<Node>();
		
		for (Node n : leafList) {
			if (n.treeDepth == maxDepth) {
				resultList.add(n);
				return resultList;
			}else if (n.treeDepth > maxDepth) {
				Node atDepth = n;
				while (atDepth.treeDepth > maxDepth) {
					atDepth = atDepth.parent;
				}
				resultList.add(n);
				return resultList;
			}
		}
		throw new RuntimeException("Tried to get tree stage results before the stage was complete.");
	}

	@Override
	public boolean checkTerminationConditions() {
		Node rootNode = getRootNode();
		leafList.clear();
		rootNode.getLeaves(leafList);
		
		for (Node n : leafList) {
			if (n.treeDepth >= maxDepth) {
				return true;
			}
		}
		return false;
	}

}
