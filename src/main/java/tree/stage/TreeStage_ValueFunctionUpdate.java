package tree.stage;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphics;
import tree.node.NodeQWOPGraphicsBase;
import value.ValueFunction_TensorFlow;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeStage_ValueFunctionUpdate<C extends Command<?>> extends TreeStage<C> {

    public final ValueFunction_TensorFlow<C> valueFunction;

    public boolean excludeLeaves = false;
    public boolean updateGraphicalLabels = true;

    private int checkpointIndex;
    public final String checkpointName;

    private boolean isFinished = true;

    public TreeStage_ValueFunctionUpdate(@JsonProperty("valueFunction") ValueFunction_TensorFlow<C> valueFunction,
                                         @JsonProperty("checkpointName") String checkpointName,
                                         @JsonProperty("checkpointIndex") int checkpointIndex) {
        this.valueFunction = valueFunction;
        this.checkpointName = checkpointName;
        this.checkpointIndex = checkpointIndex;
    }

    public String getCheckpointName() {
        return checkpointName;
    }

    @Override
    public List<NodeQWOPBase<?, C>> getResults() { return null; }

    @Override
    public boolean checkTerminationConditions() {
        return isFinished;
    }

    @Override
    public void initialize(List<TreeWorker<C>> treeWorkers, NodeQWOPExplorableBase<?, C> stageRoot) {
        isFinished = false;
        // Update the value function.
        List<NodeQWOPExplorableBase<?, C>> nodesBelow = new ArrayList<>();
        stageRoot.recurseDownTreeExclusive(n -> {
            if (!excludeLeaves || n.getChildCount() > 0) { // Can include or exclude leaves.
                nodesBelow.add(n);
            }
        });
        Collections.shuffle(nodesBelow);
        valueFunction.update(nodesBelow);

        try {
            valueFunction.saveCheckpoint(checkpointName + checkpointIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkpointIndex++;

        if (updateGraphicalLabels && stageRoot instanceof NodeQWOPGraphicsBase) {
            NodeQWOPGraphics graphicsRootNode = (NodeQWOPGraphics) stageRoot;
            Runnable updateLabels = () -> graphicsRootNode.recurseDownTreeExclusive(node -> {
                if (node instanceof NodeQWOPGraphicsBase) { // TODO not sure why I need to do this. Figure out
                    // something less hacky.
                    NodeQWOPGraphicsBase n = (NodeQWOPGraphicsBase) node;

                    float percDiff = valueFunction.evaluate(n); // Temp disable percent diff for absolute diff.
//                    float percDiff = Math.abs((valueFunction.evaluateActionDistribution(n) - n.getValue())/n.getValue() * 100f);
                    n.nodeLabel = String.format("%.1f, %.1f", n.getValue(), percDiff);
                    Color color = NodeQWOPGraphicsBase.getColorFromScaledValue(-Math.min(Math.abs(percDiff - n.getValue()), 20) + 20, 20, 0.9f);
                    n.setLabelColor(color);
                    //n.setOverridePointColor(color);
                    n.displayLabel = true;
                }
            });
            new Thread(updateLabels).start();
        }
        isFinished = true;
    }
}
