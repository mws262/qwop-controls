package tree.node.evaluator;

import game.action.Command;
import game.qwop.StateQWOP;
import tree.node.NodeGameBase;

import static game.qwop.IStateQWOP.ObjectName;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Velocity<C extends Command<?>> implements IEvaluationFunction<C> {

    @Override
    public float getValue(NodeGameBase<?, C> nodeToEvaluate) {
        // TODO fix cast
        return ((StateQWOP) nodeToEvaluate.getState()).getStateVariableFromName(ObjectName.BODY).getDx();
    }

    @Override
    public String getValueString(NodeGameBase<?, C> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Velocity<C> getCopy() {
        return new EvaluationFunction_Velocity<>();
    }

    @Override
    public void close() {}
}
