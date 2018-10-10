package controllers;

import java.awt.Graphics;
import java.util.List;

import game.GameLoader;
import game.State;
import main.Action;
import main.IController;
import main.TensorflowLoader;

public class Controller_Tensorflow_ClassifyActionsPerTimestep extends TensorflowLoader implements IController {

	/** Name of the input in the tflow graph. **/
    private String inputName = "";
	
	/** Name of the output in the tflow graph. **/
    private String outputName = "";
	
	/** Print out each prediction. **/
    private boolean verbose = true;
	
	private int prevAction = 0;
	
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
		
		float thresh = 1f;
		//System.out.println(probability0 + "," + probability1 + "," + probability2);
		// WO
		if ((probability0 > thresh && prevAction == 0) || probability0 > probability1 && probability0 > probability2) {
			chosenAction = new Action(1, false, true, true, false);
			prevAction = 0;
			if (verbose) System.out.println("WO, " + probability0);
			
		// QP
		}else if ((probability1 > thresh && prevAction == 1) || probability1 > probability0 && probability1 > probability2) {
			chosenAction = new Action(1, true, false, false, true);
			prevAction = 1;
			if (verbose) System.out.println("QP, " + probability1);

		// None	
		}else if (probability2 > probability0 && probability2 > probability1) {
//			(probability2 > thresh && prevAction == 2) ||
			chosenAction = new Action(1, false, false, false, false);
			prevAction = 2;
			if (verbose) System.out.println("__, " + probability2);
		}
		
		return chosenAction;
	}

	@Override
	public void draw(Graphics g, GameLoader game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {}

}
