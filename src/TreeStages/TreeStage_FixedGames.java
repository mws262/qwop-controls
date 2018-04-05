package TreeStages;

import java.util.ArrayList;
import java.util.List;

import main.ISampler;
import main.Node;
import main.TreeStage;
import main.TreeWorker;

/**
 * This stage tries to play a fixed number of games with the given sampler.
 * 
 * @author matt
 *
 */
public class TreeStage_FixedGames extends TreeStage {
	
	/** How many games were played before this stage started? **/
	private long initialGamesPlayed;
	
	/** Number of games this stage should play. **/
	private long numGamesToPlay;
	
	public TreeStage_FixedGames(ISampler sampler, long numGamesToPlay) {
		this.sampler = sampler;
		this.numGamesToPlay = numGamesToPlay;
	}
	
	@Override
	public void initialize(Node treeRoot, int numWorkers) {
		initialGamesPlayed = TreeWorker.getTotalGamesPlayed();
		super.initialize(treeRoot, numWorkers);
	}
	
	@Override
	public List<Node> getResults() {
		List<Node> resultList = new ArrayList<Node>();
		resultList.add(getRootNode()); // No particularly interesting results.
		return resultList; 
	}

	@Override
	public boolean checkTerminationConditions() {
		return (TreeWorker.getTotalGamesPlayed() - initialGamesPlayed + numWorkers) >= numGamesToPlay; // Won't always manage to stop exactly at the right number, but close enough.
	}

}
