package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jblas.util.Random;

import controllers.Controller_AskServer;
import controllers.Controller_NearestNeighborApprox;
import controllers.Controller_Null;
import data.SaveableActionSequence;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import game.GameLoader;
import game.State;

/**
 * Playback runs or sections of runs saved in SaveableSingleRun files.
 * 
 * @author matt
 *
 */

@SuppressWarnings("serial")
public class MAIN_Controlled extends JFrame implements Runnable, ActionListener{

	private GameLoader game = new GameLoader();

	/** Window width **/
	private static int windowWidth = 1920;

	/** Window height **/
	private static int windowHeight = 1000;

	/** Controller to use. Defaults to Controller_Null and should usually be reassigned. **/
	private IController controller = new Controller_Null();

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	private int xOffsetPixels = 960;
	private int yOffsetPixels = 500;

	/** Runner coordinates to pixels. **/
	private float runnerScaling = 10f;

	/** Place to load any 'prefix' run data in the form of a SaveableSingleGame **/
	private File prefixSave = new File(Utility.getExcutionPath() + "saved_data/4_25_18/steadyRunPrefix.SaveableSingleGame");

	/** Will do the loaded prefix (open loop) to this tree depth before letting the controller take over. **/
	private int doPrefixToDepth = 2;

	private List<Node> leafNodes = new ArrayList<Node>();

	private SaveableFileIO<SaveableActionSequence> actionSaver = new SaveableFileIO<SaveableActionSequence>();

	private String savePath = Utility.getExcutionPath() + "saved_data/individual_expansions_todo/";

	private ActionQueue actionQueue = new ActionQueue();

	public static void main(String[] args) {
		MAIN_Controlled mc = new MAIN_Controlled();

		//		// CONTROLLER -- Neural net picks keys.
		//		Controller_Tensorflow_ClassifyActionsPerTimestep cont = new Controller_Tensorflow_ClassifyActionsPerTimestep("frozen_model.pb", "./python/logs/");
		//		cont.inputName = "tfrecord_input/split";
		//		cont.outputName = "softmax/Softmax";


		// CONTROLLER -- Approximate nearest neighbor.
//		File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");
//
//		File[] allFiles = saveLoc.listFiles();
//		if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());
//
//		List<File> exampleDataFiles = new ArrayList<File>();
//		for (File f : allFiles){
//			if (f.getName().contains("TFRecord")) {
//				System.out.println("Found save file: " + f.getName());
//				exampleDataFiles.add(f);
//				break;
//			}
//		}
//		Controller_NearestNeighborApprox cont = new Controller_NearestNeighborApprox(exampleDataFiles);
//		mc.controller = cont;
//		mc.setup();
//		mc.doControlled();
		
		
		// Have the server do the calculations for me.
		Controller_NearestNeighborApprox subCont = new Controller_NearestNeighborApprox(new ArrayList<File>());
		//subCont.comparePreviousStates = false;
//		subCont.upperSetLimit = 50;
//		subCont.lowerSetLimit = 50;
		subCont.penalizeEndOfSequences = false;
		//subCont.enableVoting = true;
		//subCont.maxPenaltyForEndOfSequence = 0f;
		subCont.previousStatePenaltyMult = 0.9f;
		subCont.enableTrajectorySnapping = false;
		subCont.trajectorySnappingThreshold = 5f;
		Controller_AskServer serverCont = new Controller_AskServer(subCont);

		mc.controller = serverCont;
		mc.setup();
		mc.doControlled();
		
		serverCont.closeAll();
	}

	public void setup() {
		Panel panel = new Panel();
		this.setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		panel.setVisible(true);
		Thread graphicsThread = new Thread(this);
		graphicsThread.start(); // Makes it smoother by updating the graphics faster than the timestep updates.

		JButton saveButton = new JButton();
		saveButton.setText("Save actions");
		saveButton.addActionListener(this);
		saveButton.setVisible(true);
		saveButton.setPreferredSize(new Dimension(1000,50));
		add(saveButton, BorderLayout.PAGE_END);

		game.mainRunnerColor = Color.ORANGE;
		panel.setBackground(Color.BLACK);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(windowWidth, windowHeight));
		setContentPane(this.getContentPane());
		pack();
		setVisible(true); 
		repaint();
	}

	public void doControlled() {

		// Recreate prefix part of this tree.
		SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
		Node rootNode = new Node();
		Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(prefixSave.getAbsolutePath()), rootNode, -1);
		leafNodes.clear();
		rootNode.getLeaves(leafNodes);
		Node endNode = leafNodes.get(0);

		// Back up the tree in order to skip the end of the prefix.
		while (endNode.treeDepth > doPrefixToDepth) {
			endNode = endNode.parent;
		}
		// Run prefix part.
		actionQueue.addSequence(endNode.getSequence());
		while (!actionQueue.isEmpty()) {
			executeNextOnQueue();
			//			
			//			if (actionQueue.peekNextAction() == null) { // Experimental -- makes the final action end early so it doesn't throw it over to the controller right at a transition.
			//				actionQueue.pollCommand();
			//			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// Enter controller mode.
		while (true) {
			long initTime = System.currentTimeMillis();
			State state = game.getCurrentState();
			System.out.println(state.body.x);
			Action nextAction = controller.policy(state);
			actionQueue.addAction(nextAction);

			while (!actionQueue.isEmpty()) {

//				try {
//					if (Random.nextFloat() > 0.5f) {
//						game.applyBodyImpulse(Random.nextFloat() - 0.5f, Random.nextFloat() - 0.5f);
//					}
//					//game.applyBodyTorque(-2f);
//				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
//						| NoSuchMethodException | SecurityException | InstantiationException e1) {
//					e1.printStackTrace();
//				}

				executeNextOnQueue();
				try {
					Thread.sleep(Long.max(20 - (System.currentTimeMillis() - initTime), 0l));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			repaint();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	/** Pop the next action off the queue and execute one timestep. **/
	private void executeNextOnQueue() {
		if (!actionQueue.isEmpty()) {
			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			boolean Q = nextCommand[0];
			boolean W = nextCommand[1]; 
			boolean O = nextCommand[2];
			boolean P = nextCommand[3];
			game.stepGame(Q,W,O,P);
		}
	}

	private class Panel extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			if (!game.initialized) return;
			super.paintComponent(g);
			if (game != null) {
				game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
				controller.draw(g, game, runnerScaling, xOffsetPixels, yOffsetPixels); // Optionally, the controller may want to draw some stuff for debugging.
				//				keyDrawer(g, Q, W, O, P);
				//				drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand() == "Save actions") {
			Action[] acts = actionQueue.getActionsInCurrentRun();
			List<Action> actsConsolidated = Action.consolidateActions(Arrays.asList(acts));

			Action[] actsOut = new Action[actsConsolidated.size()];
			SaveableActionSequence actionSequence = new SaveableActionSequence(actsConsolidated.toArray(actsOut));
			actionSaver.storeObjectsOrdered(actionSequence, savePath + "actions_" + Utility.getTimestamp() + ".SaveableActionSequence", false);
		}
	}	
}
