package tree.node.evaluator;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Velocity<C extends Command<?>, S extends IState> implements IEvaluationFunction<C, S> {

    public float scalingFactor = 1f;

    @Override
    public float getValue(NodeGameBase<?, C, S> nodeToEvaluate) {
        return nodeToEvaluate.getState().getCenterDx() * scalingFactor;
    }

    @Override
    public String getValueString(NodeGameBase<?, C, S> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Velocity<C, S> getCopy() {
        return new EvaluationFunction_Velocity<>();
    }

    @Override
    public void close() {}
}
