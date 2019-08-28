package tree.stage;

import game.action.Command;
import tree.TreeWorker;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy tree stage that can be triggered manually.
 */
public class TreeStage_Dummy<C extends Command<?>> extends TreeStage<C> {

    public boolean isInitialized = false;
    public boolean terminate = false;
    public List<NodeGameBase<?, C>> results = new ArrayList<>();

    @Override
    public List<NodeGameBase<?, C>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        return terminate;
    }

    @Override
    public void initialize(List<TreeWorker<C>> treeWorkers, NodeGameExplorableBase<?, C> stageRoot) {
        isInitialized = true;
    }
}
