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
	
	/** Max relative depth (i.e. specified relative to the given root node). **/
	private int maxDepth;
	
	/** Max effective depth (i.e. absolute depth relative to the entire tree root). **/
	private int maxEffectiveDepth;
	
	public TreeStage_MaxDepth(ISampler sampler, int maxDepth) {
		this.sampler = sampler;
		this.maxDepth = maxDepth;
	}
	
	@Override
	public void initialize(Node treeRoot, int numWorkers) {
		maxEffectiveDepth = maxDepth + treeRoot.treeDepth;
		super.initialize(treeRoot, numWorkers);
	}
	
	@Override
	public List<Node> getResults() {
		List<Node> resultList = new ArrayList<Node>();
		
		for (Node n : leafList) {
			if (n.treeDepth == maxEffectiveDepth) {
				resultList.add(n);
				return resultList;
			}else if (n.treeDepth > maxEffectiveDepth) {
				Node atDepth = n;
				while (atDepth.treeDepth > maxEffectiveDepth) {
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
		if (rootNode.fullyExplored.get()) return true;
		leafList.clear();
		rootNode.getLeaves(leafList);
		
		for (Node n : leafList) {
			if (n.treeDepth >= maxEffectiveDepth) {
				return true;
			}
		}
		return false;
	}

}
