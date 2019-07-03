package tree.stage;

import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy tree stage that can be triggered manually.
 */
public class TreeStage_Dummy extends TreeStage {

    public boolean isInitialized = false;
    public boolean terminate = false;
    public List<NodeQWOPBase<?>> results = new ArrayList<>();

    @Override
    public List<NodeQWOPBase<?>> getResults() {
        return results;
    }

    @Override
    public boolean checkTerminationConditions() {
        return terminate;
    }

    @Override
    public void initialize(List<TreeWorker> treeWorkers, NodeQWOPExplorableBase<?> stageRoot) {
        isInitialized = true;
    }
}
