package tree.node.evaluator;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.util.Objects;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Distance<C extends Command<?>, S extends IState> implements IEvaluationFunction<C, S> {

    public float scalingFactor = 1f;

    @Override
    public float getValue(NodeGameBase<?, C, S> nodeToEvaluate) {
        return Objects.requireNonNull(nodeToEvaluate.getState()).getCenterX() * scalingFactor;
    }

    @Override
    public String getValueString(NodeGameBase<?, C, S> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Distance<C, S> getCopy() {
        return new EvaluationFunction_Distance<>();
    }

    @Override
    public void close() {}
}
