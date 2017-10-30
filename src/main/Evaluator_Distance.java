package main;

/**
 * Evaluation of a node based on X progress along the track.
 * @author Matt
 *
 */
public class Evaluator_Distance implements IEvaluationFunction {

	@Override
	public float getValue(Node nodeToEvaluate) {
		float value = 0.f;
		
		value += subsequentNodeDistanceValue(nodeToEvaluate);
		
		return(value);
	}

	@Override
	public String getValueString(Node nodeToEvaluate) {
		return String.valueOf(getValue(nodeToEvaluate));
	}
	
	/************ Components of the value so things can by swapped out easily. **********/
	
	/** X distance of body between a node and its parent. **/
	private float subsequentNodeDistanceValue(Node nodeToEvaluate) {
		if (nodeToEvaluate.treeDepth > 0) {
			return nodeToEvaluate.state.body.x;// - nodeToEvaluate.parent.state.body.x; // Difference in x between subsequent nodes. 
		}else {
			return 0.f; // Root gets score of 0.
		}
	}

}
