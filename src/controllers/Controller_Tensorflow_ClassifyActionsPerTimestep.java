package controllers;

import java.util.List;

import game.State;
import main.Action;
import main.IController;
import main.TensorflowLoader;

public class Controller_Tensorflow_ClassifyActionsPerTimestep extends TensorflowLoader implements IController {

	/** Name of the input in the tflow graph. **/
	public String inputName = "";
	
	/** Name of the output in the tflow graph. **/
	public String outputName = "";
	
	/** Print out each prediction. **/
	public boolean verbose = false;
	
	public Controller_Tensorflow_ClassifyActionsPerTimestep(String pbFile, String directory) {
		super(pbFile, directory);
	}

	@Override
	public Action policy(State state) {
		List<Float> keyClassification = sisoFloatPrediction(state, inputName, outputName);
		
		float probability0 = keyClassification.get(0);
		float probability1 = keyClassification.get(1);
		float probability2 = keyClassification.get(2);
		
		Action chosenAction = null;
		
		// WO
		if (probability0 > probability1 && probability0 > probability2) {
			chosenAction = new Action(1, false, true, true, false);
			if (verbose) System.out.println("WO");
			
		// QP
		}else if (probability1 > probability0 && probability1 > probability2) {
			chosenAction = new Action(1, true, false, false, true);
			if (verbose) System.out.println("QP");

		// None	
		}else if (probability2 > probability0 && probability2 > probability1) {
			chosenAction = new Action(1, false, false, false, false);
			if (verbose) System.out.println("__");
		}
		
		return chosenAction;
	}

}
