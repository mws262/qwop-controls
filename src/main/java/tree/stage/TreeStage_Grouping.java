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

    private TreeStage activeStage;

    private List<TreeWorker> treeWorkers;

    private NodeQWOPExplorableBase<?> stageRoot;

    private List<NodeQWOPBase<?>> results = new ArrayList<>();

    private boolean isFinished = false;

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
        results.clear();

        // TODO: test behavior now.
        for (int i = 0; i < treeStages.length; i++) {
            activeStage = treeStages[i];
            activeStage.initialize(treeWorkers, stageRoot);
            List<NodeQWOPBase<?>> results = activeStage.getResults();
            if (results != null)
                this.results.addAll(results); // Get the individual stage's results.
            if (i < treeStages.length - 1) {
                this.treeWorkers.clear();
                for (int j = 0; j < treeWorkers.size(); j++) {
                    this.treeWorkers.add(treeWorkers.get(j).getCopy());
                }
            }
        }

        isFinished = true;
    }

    @Override
    @JsonIgnore
    public List<NodeQWOPBase<?>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        return isFinished;
    }

    @JsonIgnore
    public TreeStage getActiveStage() {
        return activeStage;
    }
}
