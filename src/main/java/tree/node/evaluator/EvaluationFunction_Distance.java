package tree.node.evaluator;

import game.action.Command;
import tree.node.NodeGameBase;

import java.util.Objects;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Distance<C extends Command<?>> implements IEvaluationFunction<C> {

    public float scalingFactor = 1f;

    @Override
    public float getValue(NodeGameBase<?, C> nodeToEvaluate) {
        return Objects.requireNonNull(nodeToEvaluate.getState()).getCenterX() * scalingFactor;
    }

    @Override
    public String getValueString(NodeGameBase<?, C> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Distance<C> getCopy() {
        return new EvaluationFunction_Distance<>();
    }

    @Override
    public void close() {}
}
