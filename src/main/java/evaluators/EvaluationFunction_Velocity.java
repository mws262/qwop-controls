package evaluators;

import game.IState;
import tree.NodeQWOPBase;

import java.util.Objects;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Velocity implements IEvaluationFunction {

    @Override
    public float getValue(NodeQWOPBase<?> nodeToEvaluate) {
        return Objects.requireNonNull(nodeToEvaluate.getState()).getStateVariableFromName(IState.ObjectName.BODY).getDx();
    }

    @Override
    public String getValueString(NodeQWOPBase<?> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Velocity getCopy() {
        return new EvaluationFunction_Velocity();
    }
}
