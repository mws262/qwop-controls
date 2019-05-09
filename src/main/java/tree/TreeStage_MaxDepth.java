package tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import savers.IDataSaver;
import samplers.ISampler;

/**
 * Goes until any branch reaches a certain tree depth.
 *
 * @author matt
 */
public class TreeStage_MaxDepth extends TreeStage {
    private List<NodeQWOPExplorableBase<?>> leafList = new LinkedList<>();

    /**
     * Max relative depth (i.e. specified relative to the given root node).
     */
    private int maxDepth;

    /**
     * Max effective depth (i.e. absolute depth relative to the entire tree root).
     */
    private int maxEffectiveDepth;

    /**
     * How many games were played previous to the initialization of this stage?
     */
    private long gamesPlayedAtStageStart;

    /**
     * Alternate termination condition: We played more than this number of games without getting to the desired max
     * depth.
     */
    public long terminateAfterXGames = 120000;

    public TreeStage_MaxDepth(int maxDepth, ISampler sampler, IDataSaver saver) {
        this.maxDepth = maxDepth;
        this.sampler = sampler;
        this.saver = saver;
    }

    @Override
    public void initialize(List<TreeWorker> treeWorkers, NodeQWOPExplorableBase<?> stageRoot) {
        maxEffectiveDepth = maxDepth + stageRoot.getTreeDepth();
        gamesPlayedAtStageStart = TreeWorker.getTotalGamesPlayed();
        super.initialize(treeWorkers, stageRoot);
    }

    @Override
    public List<NodeQWOPBase<?>> getResults() {
        List<NodeQWOPBase<?>> resultList = new ArrayList<>();
        leafList.clear();
        getRootNode().applyToLeaves(leafList::add);

        if (getRootNode().isFullyExplored() || (TreeWorker.getTotalGamesPlayed() - gamesPlayedAtStageStart) > terminateAfterXGames)
            return resultList; // No results. No possible way to recover.

        assert leafList.size() > 1;

        for (NodeQWOPBase<?> n : leafList) {
            if (n.getTreeDepth() == maxEffectiveDepth) {
                resultList.add(n);
                return resultList;
            } else if (n.getTreeDepth() > maxEffectiveDepth) {
                NodeQWOPBase<?> atDepth = n;
                while (atDepth.getTreeDepth() > maxEffectiveDepth) {
                    atDepth = atDepth.getParent();
                }
                resultList.add(n);
                return resultList;
            }
        }
        throw new RuntimeException("Tried to get tree stage results before the stage was complete.");
    }

    @Override
    public boolean checkTerminationConditions() {
        NodeQWOPExplorableBase<?> rootNode = getRootNode();
        // Also terminate if it's been too long and we haven't found anything.
        return rootNode.isFullyExplored() || rootNode.getMaxBranchDepth() >= maxEffectiveDepth ||
                (TreeWorker.getTotalGamesPlayed() - gamesPlayedAtStageStart) > terminateAfterXGames;
    }
}
