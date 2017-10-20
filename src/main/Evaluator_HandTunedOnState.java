package main;

public class Evaluator_HandTunedOnState implements IEvaluationFunction {

	@Override
	public float getValue(Node nodeToEvaluate) {
		float value = 0.f;
		//value += getAngleValue(nodeToEvaluate);
		value += getDistanceValue(nodeToEvaluate);
		
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
		return -100*nodeToEvaluate.state.body.th;
	}
	
	private float getDistanceValue(Node nodeToEvaluate) {
		return nodeToEvaluate.state.body.x;
	}
}
