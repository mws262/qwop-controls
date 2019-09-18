package tree.node.evaluator;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.util.Random;

/**
 * This evaluation function returns a random value regardless of the given node.
 * This is rarely a good idea, but is ok for testing stuff.
 *
 * @author Matt
 */
public class EvaluationFunction_Random<C extends Command<?>, S extends IState> implements IEvaluationFunction<C, S> {

    /**
     * Random number generator for generating random node scores.
     */
    private static final Random rand = new Random();

    @Override
    public float getValue(NodeGameBase<?, C, S> nodeToEvaluate) {
        return rand.nextFloat();
    }

    @Override
    public String getValueString(NodeGameBase<?, C, S> nodeToEvaluate) {
        return "Random score is not particularly meaningful.";
    }

    @Override
    public EvaluationFunction_Random<C, S> getCopy() {
        return new EvaluationFunction_Random<>();
    }

    @Override
    public void close() {}
}
