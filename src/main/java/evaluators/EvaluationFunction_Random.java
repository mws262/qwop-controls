package evaluators;

import java.util.Random;

import tree.node.NodeQWOPBase;

/**
 * This evaluation function returns a random value regardless of the given node.
 * This is rarely a good idea, but is ok for testing stuff.
 *
 * @author Matt
 */
public class EvaluationFunction_Random implements IEvaluationFunction {

    /**
     * Random number generator for generating random node scores.
     */
    private static final Random rand = new Random();

    @Override
    public float getValue(NodeQWOPBase<?> nodeToEvaluate) {
        return rand.nextFloat();
    }

    @Override
    public String getValueString(NodeQWOPBase<?> nodeToEvaluate) {
        return "Random score is not particularly meaningful.";
    }

    @Override
    public EvaluationFunction_Random getCopy() {
        return new EvaluationFunction_Random();
    }

}
