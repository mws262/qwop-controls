package tree.stage;

import game.action.Command;
import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy tree stage that can be triggered manually.
 */
public class TreeStage_Dummy<C extends Command<?>> extends TreeStage<C> {

    public boolean isInitialized = false;
    public boolean terminate = false;
    public List<NodeQWOPBase<?, C>> results = new ArrayList<>();

    @Override
    public List<NodeQWOPBase<?, C>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        return terminate;
    }

    @Override
    public void initialize(List<TreeWorker<C>> treeWorkers, NodeQWOPExplorableBase<?, C> stageRoot) {
        isInitialized = true;
    }
}
