package TreeStages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import savers.IDataSaver;
import samplers.ISampler;
import main.Node;
import main.TreeStage;
import main.TreeWorker;

/**
 * Goes until any branch reaches a certain tree depth.
 *
 * @author matt
 */
public class TreeStage_MaxDepth extends TreeStage {
    private List<Node> leafList = new LinkedList<>();

    /**
     * Max relative depth (i.e. specified relative to the given root node).
     **/
    private int maxDepth;

    /**
     * Max effective depth (i.e. absolute depth relative to the entire tree root).
     **/
    private int maxEffectiveDepth;

    /**
     * How many games were played previous to the initialization of this stage?
     **/
    private long gamesPlayedAtStageStart;

    /**
     * Alternate termination condition: We played more than this number of games without getting to the desired max
     * depth.
     **/
    public long terminateAfterXGames = 120000;

    public TreeStage_MaxDepth(int maxDepth, ISampler sampler, IDataSaver saver) {
        this.maxDepth = maxDepth;
        this.sampler = sampler;
        this.saver = saver;
    }

    @Override
    public void initialize(List<TreeWorker> treeWorkers, Node stageRoot) {
        maxEffectiveDepth = maxDepth + stageRoot.treeDepth;
        gamesPlayedAtStageStart = TreeWorker.getTotalGamesPlayed();
        super.initialize(treeWorkers, stageRoot);
    }

    @Override
    public List<Node> getResults() {
        List<Node> resultList = new ArrayList<>();
        leafList.clear();
        getRootNode().getLeaves(leafList);

        if (getRootNode().fullyExplored.get() || (TreeWorker.getTotalGamesPlayed() - gamesPlayedAtStageStart) > terminateAfterXGames)
            return resultList; // No results. No possible way to recover.

        for (Node n : leafList) {
            if (n.treeDepth == maxEffectiveDepth) {
                resultList.add(n);
                return resultList;
            } else if (n.treeDepth > maxEffectiveDepth) {
                Node atDepth = n;
                while (atDepth.treeDepth > maxEffectiveDepth) {
                    atDepth = atDepth.getParent();
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
        // Also terminate if it's been too long and we haven't found anything.
        return rootNode.fullyExplored.get() || rootNode.maxBranchDepth.get() >= maxEffectiveDepth ||
                (TreeWorker.getTotalGamesPlayed() - gamesPlayedAtStageStart) > terminateAfterXGames;
    }
}
