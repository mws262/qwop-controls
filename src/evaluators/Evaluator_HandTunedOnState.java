package evaluators;

import org.jblas.util.Random;

import game.State;
import main.IEvaluationFunction;
import main.Node;

public class Evaluator_HandTunedOnState implements IEvaluationFunction {

	@Override
	public float getValue(Node nodeToEvaluate) {
		if (nodeToEvaluate.state == null) return 0f;
		float value = 0.f;
		value += getAngleValue(nodeToEvaluate);
		value += getDistanceValue(nodeToEvaluate);
		value += getVelocityValue(nodeToEvaluate);
		value += Random.nextFloat(); // Trying to add a little noise for fun.
		
		return value;
	}
	
	public float getValue(State state) {
		float value = 0.f;
		value += state.body.th;
		value += state.body.x;
		value += state.body.dx;
		
		return value;
	}

	@Override
	public String getValueString(Node nodeToEvaluate) {
		String value = "";
		//value += "Angle value: ";
		//value += getAngleValue(nodeToEvaluate);
		value += ", Distance value: ";
		value += getDistanceValue(nodeToEvaluate);
		return value;
	}

	
	private float getAngleValue(Node nodeToEvaluate) {
		return nodeToEvaluate.state.body.th;
	}
	
	private float getDistanceValue(Node nodeToEvaluate) {
		return nodeToEvaluate.state.body.x;
	}
	
	private float getVelocityValue(Node nodeToEvaluate) {
		return nodeToEvaluate.state.body.dx;
	}
	
	@Override
	public Evaluator_HandTunedOnState clone() {
		return new Evaluator_HandTunedOnState();
	}
}
