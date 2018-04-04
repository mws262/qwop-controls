package filters;

import java.util.List;

import main.INodeFilter;
import main.Node;
import main.Utility;

/**
 * Filter which reduceds the number of nodes in a list. Usually done for visualization
 * or computational reasons.
 * 
 * @author matt
 *
 */
public class NodeFilter_Downsample implements INodeFilter {

	/** Given a list, the downsampler will keep a maximum of this number of nodes. **/
	public int maxNodesToKeep;
	
	/** What strategy is used to downsample? **/
	public Strategy currentStrategy = Strategy.EVENLY_SPACED;
	
	public enum Strategy {
		EVENLY_SPACED, RANDOM
	}
	
	public NodeFilter_Downsample(int maxNodesToKeep) {
		this.maxNodesToKeep = maxNodesToKeep;
	}
	
	@Override
	public boolean filter(Node node) {
		return true;
	}

	@Override
	public void filter(List<Node> nodes) {
		int numNodes = nodes.size();
		if (numNodes > maxNodesToKeep) {
			switch (currentStrategy) {
			case EVENLY_SPACED:		
				float ratio = numNodes/(float)maxNodesToKeep;
				for (int i = 0; i < maxNodesToKeep; i++) {
					nodes.set(i, nodes.get((int)(ratio * i))); // Reassign the ith element with the spaced out one later in the arraylist.	
				}

				for (int i = numNodes; i > maxNodesToKeep; i--) {
					nodes.remove(i - 1);
				}
				break;
				
			case RANDOM:
				while (nodes.size() > maxNodesToKeep) {
					int idxToRemove = Utility.randInt(0, nodes.size() - 1);
					nodes.remove(idxToRemove);
				}
				break;
				
			default:
				throw new RuntimeException("Unknown downsampling strategy.");
			}	
		}
	}
}
