package tree.stage;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Goes until any branch reaches a specified tree depth.
 *
 * @author matt
 */
public class TreeStage_MaxDepth<C extends Command<?>, S extends IState> extends TreeStage<C, S> {

    private List<NodeGameExplorableBase<?, C, S>> leafList = new LinkedList<>();

    /**
     * Max relative depth (i.e. specified relative to the given root node).
     */
    public final int maxDepth;

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
    @JsonProperty("maxGames")
    public final int maxGames;

    /**
     * Tree stage which searches until a certain tree depth is achieved anywhere on the tree, the root node becomes
     * fully-explored ({@link NodeGameExplorableBase#isFullyExplored()}), or some maximum number of games threshold is
     * met.
     * @param maxDepth Depth to search until.
     */
    public TreeStage_MaxDepth(@JsonProperty("maxDepth") int maxDepth,
                              @JsonProperty("maxGames") int maxGames) {
        this.maxDepth = maxDepth;
        this.maxGames = maxGames;
    }

    @Override
    public void initialize(List<TreeWorker<C, S>> treeWorkers, NodeGameExplorableBase<?, C, S> stageRoot) {
        maxEffectiveDepth = maxDepth + stageRoot.getTreeDepth();
        gamesPlayedAtStageStart = TreeWorker.getTotalGamesPlayed();
        super.initialize(treeWorkers, stageRoot);
    }

    @Override
    public List<NodeGameBase<?, C, S>> getResults() {
        List<NodeGameBase<?, C, S>> resultList = new ArrayList<>();
        leafList.clear();
        getRootNode().applyToLeavesBelow(leafList::add);

        if (getRootNode().isFullyExplored() || (TreeWorker.getTotalGamesPlayed() - gamesPlayedAtStageStart) > maxGames)
            return resultList; // No results. No possible way to recover.

        assert leafList.size() > 1;

        for (NodeGameBase<?, C, S> n : leafList) {
            if (n.getTreeDepth() == maxEffectiveDepth) {
                resultList.add(n);
                return resultList;
            } else if (n.getTreeDepth() > maxEffectiveDepth) {
                NodeGameBase<?, C, S> atDepth = n;
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
        NodeGameExplorableBase<?, C, S> rootNode = getRootNode();
        // Also terminate if it's been too long and we haven't found anything.
        return rootNode.isFullyExplored() || rootNode.getMaxBranchDepth() >= maxEffectiveDepth ||
                (TreeWorker.getTotalGamesPlayed() - gamesPlayedAtStageStart) > maxGames;
    }
}
