package tree.stage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.List;

public class TreeStage_Grouping extends TreeStage {

    public final TreeStage[] treeStages;

    private int activeStageIdx = 0;

    private TreeStage activeStage;

    private List<TreeWorker> treeWorkers;

    private NodeQWOPExplorableBase<?> stageRoot;

    private List<NodeQWOPBase<?>> results = new ArrayList<>();

    public TreeStage_Grouping(@JsonProperty("treeStages") TreeStage[] treeStages) {
        Preconditions.checkArgument(treeStages.length > 0, "Must provide at least 1 tree stage to group.",
                treeStages.length);
        this.treeStages = treeStages;
        activeStage = treeStages[0];
    }

    @Override
    public void initialize(List<TreeWorker> treeWorkers, NodeQWOPExplorableBase<?> stageRoot) {
        this.treeWorkers = treeWorkers;
        this.stageRoot = stageRoot;
        activeStageIdx = 0;
        activeStage = treeStages[0];
        results.clear();
        activeStage.initialize(treeWorkers, stageRoot);

        // Keep checking termination conditions regularly to make sure that the next stage gets swapped in when the
        // previous is done.
        new Thread(() -> {
            while (!checkTerminationConditions()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    @JsonIgnore
    public List<NodeQWOPBase<?>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        if (activeStage.checkTerminationConditions()) {
            List<NodeQWOPBase<?>> results = activeStage.getResults();
            if (results != null)
                this.results.addAll(results); // Get the individual stage's results.
            // Out of stages.
            if (activeStageIdx == treeStages.length - 1) {
                return true;
            } else { // Move to the next stage.
                activeStageIdx++;
                activeStage = treeStages[activeStageIdx];
                activeStage.initialize(treeWorkers, stageRoot);
            }
        }
        return false;
    }

    @JsonIgnore
    public TreeStage getActiveStage() {
        return activeStage;
    }
}
