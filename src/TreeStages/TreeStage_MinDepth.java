package TreeStages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import main.ISampler;
import main.Node;
import main.TreeStage;

/**
 * Searches until we meet a minimum depth requirement in all branches
 * sort of like a BFS. This might never terminate with some greedy searches
 * so be careful.
 * 
 * @author matt
 *
 */
public class TreeStage_MinDepth extends TreeStage {

	private List<Node> leafList = new LinkedList<Node>();
	
	private int minDepth;
	
	public TreeStage_MinDepth(ISampler sampler, int minDepth) {
		this.sampler = sampler;
		this.minDepth = minDepth;
	}
	
	@Override
	public List<Node> getResults() {
		List<Node> resultList = new ArrayList<Node>();
		
		for (Node n : leafList) {
			if (n.treeDepth == minDepth) {
				resultList.add(n);
			}else if (n.treeDepth > minDepth) {
				Node atDepth = n;
				while (atDepth.treeDepth > minDepth) {
					atDepth = atDepth.parent;
				}
				resultList.add(n);
			}
		}
		return resultList;
	}

	@Override
	public boolean checkTerminationConditions() {
		Node rootNode = getRootNode();
		leafList.clear();
		rootNode.getLeaves(leafList);
		
		for (Node n : leafList) {
			if (n.treeDepth < minDepth) {
				return false;
			}
		}
		return true;
	}

}
