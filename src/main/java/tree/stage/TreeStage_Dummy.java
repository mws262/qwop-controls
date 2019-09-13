package tree.stage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy tree stage that can be triggered manually.
 */
@SuppressWarnings("unused")
public class TreeStage_Dummy<C extends Command<?>, S extends IState> extends TreeStage<C, S> {

    @JsonIgnore
    public boolean isInitialized = false;

    @JsonIgnore
    public boolean terminate = false;

    public List<NodeGameBase<?, C, S>> results = new ArrayList<>();

    @Override
    public List<NodeGameBase<?, C, S>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        return terminate;
    }

    @Override
    public void initialize(List<TreeWorker<C, S>> treeWorkers, NodeGameExplorableBase<?, C, S> stageRoot) {
        isInitialized = true;
    }
}
