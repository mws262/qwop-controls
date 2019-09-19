package tree.stage;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;
import tree.node.NodeGameGraphics;
import tree.node.NodeGameGraphicsBase;
import value.ValueFunction_TensorFlow;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeStage_ValueFunctionUpdate<C extends Command<?>, S extends IState> extends TreeStage<C, S> {

    public final ValueFunction_TensorFlow<C, S> valueFunction;

    @JsonProperty
    public boolean excludeLeaves = false;

    public boolean updateGraphicalLabels = true;

    private int checkpointIndex;
    public final String checkpointName;

    private boolean isFinished = true;

    public TreeStage_ValueFunctionUpdate(@JsonProperty("valueFunction") ValueFunction_TensorFlow<C, S> valueFunction,
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
    public List<NodeGameBase<?, C, S>> getResults() { return null; }

    @Override
    public boolean checkTerminationConditions() {
        return isFinished;
    }

    @Override
    public void initialize(List<TreeWorker<C, S>> treeWorkers, NodeGameExplorableBase<?, C, S> stageRoot) {
        isFinished = false;
        // Update the value function.
        List<NodeGameExplorableBase<?, C, S>> nodesBelow = new ArrayList<>();
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

        if (updateGraphicalLabels && stageRoot instanceof NodeGameGraphicsBase) {
            NodeGameGraphics<C, S> graphicsRootNode = (NodeGameGraphics<C, S>) stageRoot;
            Runnable updateLabels = () -> graphicsRootNode.recurseDownTreeExclusive(node -> {
                if (node != null) {
//                if (node instanceof NodeGameGraphicsBase) { // TODO not sure why I need to do this. Figure out
//                    // something less hacky.
//                    NodeGameGraphicsBase n = (NodeGameGraphicsBase) node;

                    float percDiff = valueFunction.evaluate(node); // Temp disable percent diff for absolute diff.
//                    float percDiff = Math.abs((valueFunction.evaluateActionDistribution(n) - n.getValue())/n.getValue() * 100f);
                    node.nodeLabel = String.format("%.1f, %.1f", node.getValue(), percDiff);
                    Color color = NodeGameGraphicsBase.getColorFromScaledValue(-Math.min(Math.abs(percDiff - node.getValue()), 20) + 20, 20, 0.9f);
                    node.setLabelColor(color);
                    //n.setOverridePointColor(color);
                    node.displayLabel = true;
                }
            });
            new Thread(updateLabels).start();
        }
        isFinished = true;
    }
}
