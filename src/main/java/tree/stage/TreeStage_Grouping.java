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

    @JsonProperty("treeStages")
    public final List<TreeStage<C, S>> treeStages;

    private TreeStage<C, S> activeStage;

    private final List<NodeGameBase<?, C, S>> results = new ArrayList<>();

    private boolean isFinished = false;

    public TreeStage_Grouping(@JsonProperty("treeStages") List<TreeStage<C, S>> treeStages) {
        Preconditions.checkArgument(treeStages.size() > 0, "Must provide at least 1 tree stage to group.",
                treeStages.size());
        this.treeStages = treeStages;
        activeStage = treeStages.get(0);
    }

    @Override
    public void initialize(List<TreeWorker<C, S>> treeWorkers, NodeGameExplorableBase<?, C, S> stageRoot) {
        results.clear();

        // TODO: test behavior now.
        for (int i = 0; i < treeStages.size(); i++) {
            activeStage = treeStages.get(i);
            activeStage.initialize(treeWorkers, stageRoot);
            List<NodeGameBase<?, C, S>> results = activeStage.getResults();
            if (results != null)
                this.results.addAll(results); // Get the individual stage's results.
            if (i < treeStages.size() - 1) {
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
}
