package tree.stage;

import tree.sampler.ISampler;
import savers.IDataSaver;
import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * This stage tries to play a fixed number of games with the given sampler.
 *
 * @author matt
 */
public class TreeStage_FixedGames extends TreeStage {

    /**
     * How many games were played before this stage started?
     */
    private long initialGamesPlayed;

    /**
     * Number of games this stage should play.
     */
    private long numGamesToPlay;

    /**
     * Tree stage which executes until a specific number of games has been completed or the root node becomes fully
     * explored ({@link NodeQWOPExplorableBase#isFullyExplored()}).
     * @param numGamesToPlay Number of games to play.
     * @param sampler Sampling strategy on the tree.
     * @param saver Data-saving policy during and after the tree stage.
     */
    public TreeStage_FixedGames(long numGamesToPlay, ISampler sampler, IDataSaver saver) {
        this.numGamesToPlay = numGamesToPlay;
        this.saver = saver;
        this.sampler = sampler;
    }

    @Override
    public void initialize(List<TreeWorker> workers, NodeQWOPExplorableBase<?> stageRoot) {
        initialGamesPlayed = TreeWorker.getTotalGamesPlayed();
        super.initialize(workers, stageRoot);
    }

    @Override
    public List<NodeQWOPBase<?>> getResults() {
        List<NodeQWOPBase<?>> resultList = new ArrayList<>();
        resultList.add(getRootNode()); // No particularly interesting results.
        return resultList;
    }

    @Override
    public boolean checkTerminationConditions() {
        if (getRootNode().isFullyExplored()) return true;
        return (TreeWorker.getTotalGamesPlayed() - initialGamesPlayed + numWorkers) >= numGamesToPlay; // Won't always manage to stop exactly at the right number, but close enough.
    }
}
