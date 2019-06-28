package tree.stage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.*;

/**
 * If we want to switch and change between different tree stage goals.
 *
 * @author matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TreeStage_FixedGames.class, name = "fixed_games"),
        @JsonSubTypes.Type(value = TreeStage_MaxDepth.class, name = "max_depth"),
        @JsonSubTypes.Type(value = TreeStage_MinDepth.class, name = "min_depth"),
        @JsonSubTypes.Type(value = TreeStage_SearchForever.class, name = "search_forever")
})
public abstract class TreeStage {

    /** **/
    private NodeQWOPExplorableBase<?> stageRoot;

    /**
     * Each stage gets its own workers to avoid contamination. Probably could combine later if necessary.
     */
    private final List<TreeWorker> workers = new ArrayList<>();

    /**
     * Number of TreeWorkers to be used.
     */
    protected int numWorkers;

    private Logger logger = LogManager.getLogger(TreeStage.class);

    public void initialize(List<TreeWorker> treeWorkers, NodeQWOPExplorableBase<?> stageRoot) {
        numWorkers = treeWorkers.size();
        if (numWorkers < 1)
            throw new RuntimeException("Tried to assign a tree stage an invalid number of workers: " + numWorkers);

        workers.clear();
        workers.addAll(treeWorkers);
        this.stageRoot = stageRoot;

        for (TreeWorker tw : treeWorkers) {
            tw.setRoot(stageRoot);
            tw.startWorker();
        }

        while (!checkTerminationConditions()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("Stage termination conditions met.");
        workers.forEach(tw -> tw.terminateStageComplete(getRootNode().getRoot(), getResults()));
    }

    /**
     * Query the stage for its final results.
     */
    @JsonIgnore
    public abstract List<NodeQWOPBase<?>> getResults();

    /**
     * Check through the tree for termination conditions.
     */
    public abstract boolean checkTerminationConditions();

    /**
     * Check if workers are running.
     */
    public boolean areWorkersRunning() {
        synchronized (workers) {
            Iterator<TreeWorker> iterator = workers.iterator();
            if (!iterator.hasNext())
                return true; // It's stupid if the termination condition gets caught before any threads have a chance to get going.
            while (iterator.hasNext()) {
                if (iterator.next().isRunning()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the root node that this stage is operating from. It cannot change from an external caller's perspective,
     * so no set method.
     */
    @JsonIgnore
    public NodeQWOPExplorableBase<?> getRootNode() {
        return stageRoot;
    }

    @JsonIgnore
    public int getNumberOfWorkers() {
        return numWorkers;
    }
}
