package tree.node.evaluator;

import game.action.Command;
import game.state.IState;
import tree.node.NodeQWOPBase;

import java.util.Objects;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Velocity<C extends Command<?>> implements IEvaluationFunction<C> {

    @Override
    public float getValue(NodeQWOPBase<?, C> nodeToEvaluate) {
        return Objects.requireNonNull(nodeToEvaluate.getState()).getStateVariableFromName(IState.ObjectName.BODY).getDx();
    }

    @Override
    public String getValueString(NodeQWOPBase<?, C> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Velocity<C> getCopy() {
        return new EvaluationFunction_Velocity<>();
    }

    @Override
    public void close() {}
}
