package tree.stage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * This stage tries to play a fixed number of games with the given sampler.
 *
 * @author matt
 */
public class TreeStage_FixedGames<C extends Command<?>, S extends IState> extends TreeStage<C, S> {

    /**
     * How many games were played before this stage started?
     */
    private long initialGamesPlayed;

    /**
     * Number of games this stage should play.
     */
    public final long numGamesToPlay;

    /**
     * Tree stage which executes until a specific number of games has been completed or the root node becomes fully
     * explored ({@link NodeGameExplorableBase#isFullyExplored()}).
     * @param numGamesToPlay Number of games to play.
     */
    public TreeStage_FixedGames(@JsonProperty("numGamesToPlay") long numGamesToPlay) {
        this.numGamesToPlay = numGamesToPlay;
    }

    @Override
    public void initialize(List<TreeWorker<C, S>> workers, NodeGameExplorableBase<?, C, S> stageRoot) {
        initialGamesPlayed = TreeWorker.getTotalGamesPlayed();
        super.initialize(workers, stageRoot);
    }

    @JsonIgnore
    @Override
    public List<NodeGameBase<?, C, S>> getResults() {
        List<NodeGameBase<?, C, S>> resultList = new ArrayList<>();
        resultList.add(getRootNode()); // No particularly interesting results.
        return resultList;
    }

    @Override
    public boolean checkTerminationConditions() {
        if (getRootNode().isFullyExplored()) return true;
        return (TreeWorker.getTotalGamesPlayed() - initialGamesPlayed + numWorkers) >= numGamesToPlay; // Won't always manage to stop exactly at the right number, but close enough.
    }
}
