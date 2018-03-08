package evaluators;

import java.util.Random;

import main.IEvaluationFunction;
import main.Node;

/** This evaluation function returns a random value regardless of the given node.
 * This is rarely a good idea, but is ok for testing stuff.
 * @author Matt
 *
 */
public class Evaluator_Random implements IEvaluationFunction {
	Random rand = new Random();
	
	@Override
	public float getValue(Node nodeToEvaluate) {
		return rand.nextFloat();
	}

	@Override
	public String getValueString(Node nodeToEvaluate) {
		return String.valueOf(rand.nextFloat());
	}

}
