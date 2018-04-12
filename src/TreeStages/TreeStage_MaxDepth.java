package TreeStages;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import main.IDataSaver;
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
	
	public TreeStage_MaxDepth(int maxDepth, ISampler sampler, IDataSaver saver) {
		this.sampler = sampler;
		this.saver = saver;
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
		leafList.clear();
		getRootNode().getLeaves(leafList);
		
		if (getRootNode().fullyExplored.get()) return resultList; // No results. No possible way to recover.
		
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

//	private void tmpMark(Node currNode) {
//		for (Node child : currNode.children) {
//			if (child.isFailed.get()) {
//				child.overrideNodeColor = Color.PINK;
//				child.displayPoint = true;
//			}else {
//				child.overrideNodeColor = null;
//			}
//			tmpMark(child);
//		}
//	}
	@Override
	public boolean checkTerminationConditions() {
		Node rootNode = getRootNode();
		if (rootNode.fullyExplored.get() || rootNode.maxBranchDepth.get() >= maxEffectiveDepth) {
			return true;
		}else {
			return false;
		}
	}
}
