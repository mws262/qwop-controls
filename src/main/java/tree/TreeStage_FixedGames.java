package tree;

import java.util.ArrayList;
import java.util.List;

import savers.IDataSaver;
import samplers.ISampler;

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
