package tree.stage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;
import java.util.List;

public class TreeStage_Grouping<C extends Command<?>, S extends IState> extends TreeStage<C, S> {

    public final TreeStage<C, S>[] treeStages;

    private TreeStage<C, S> activeStage;

    private final List<NodeGameBase<?, C, S>> results = new ArrayList<>();

    private boolean isFinished = false;

    public TreeStage_Grouping(@JsonProperty("treeStages") TreeStage<C, S>[] treeStages) {
        Preconditions.checkArgument(treeStages.length > 0, "Must provide at least 1 tree stage to group.",
                treeStages.length);
        this.treeStages = treeStages;
        activeStage = treeStages[0];
    }

    @Override
    public void initialize(List<TreeWorker<C, S>> treeWorkers, NodeGameExplorableBase<?, C, S> stageRoot) {
        results.clear();

        // TODO: test behavior now.
        for (int i = 0; i < treeStages.length; i++) {
            activeStage = treeStages[i];
            activeStage.initialize(treeWorkers, stageRoot);
            List<NodeGameBase<?, C, S>> results = activeStage.getResults();
            if (results != null)
                this.results.addAll(results); // Get the individual stage's results.
            if (i < treeStages.length - 1) {
                treeWorkers.clear();
                for (TreeWorker<C, S> treeWorker : treeWorkers) {
                    treeWorkers.add(treeWorker.getCopy());
                }
            }
        }

        isFinished = true;
    }

    @Override
    @JsonIgnore
    public List<NodeGameBase<?, C, S>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        return isFinished;
    }

    @JsonIgnore
    public TreeStage<C, S> getActiveStage() {
        return activeStage;
    }
}
