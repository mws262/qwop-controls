package main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controllers.Controller_NearestNeighborApprox;
import controllers.Controller_Null;
import data.SaveableActionSequence;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import game.GameLoader;
import game.State;
import ui.GLPanelGeneric;
import ui.ScreenCapture;

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
	private int xOffsetPixels = 675;
	private int yOffsetPixels = 450;

	/** Runner coordinates to pixels. **/
	private float runnerScaling = 25f;

	/** Place to load any 'prefix' run data in the form of a SaveableSingleGame **/
	private File prefixSave = new File(Utility.getExcutionPath() + "saved_data/5_1_18_tmp2/single_run_2018-05-23_10-26-05.SaveableSingleGame");///4_25_18/steadyRunPrefix.SaveableSingleGame");

	/** Will do the loaded prefix (open loop) to this tree depth before letting the controller take over. **/
	private int doPrefixToDepth = 2;

	private List<Node> leafNodes = new ArrayList<Node>();

	private SaveableFileIO<SaveableActionSequence> actionSaver = new SaveableFileIO<SaveableActionSequence>();

	private String savePath = Utility.getExcutionPath() + "saved_data/individual_expansions_todo/";

	private ActionQueue actionQueue = new ActionQueue();

	private Color backgroundColor = Color.DARK_GRAY; //new Color(GLPanelGeneric.darkBackground[0],GLPanelGeneric.darkBackground[1],GLPanelGeneric.darkBackground[2]);

	/** Screen capture settings. **/
	ScreenCapture screenCap;
	private boolean doScreenCapture = true;

	Panel mainViewPanel;

	public static void main(String[] args) {
		MAIN_Controlled mc = new MAIN_Controlled();
		mc.setup();
		//		// CONTROLLER -- Neural net picks keys.
		//		Controller_Tensorflow_ClassifyActionsPerTimestep cont = new Controller_Tensorflow_ClassifyActionsPerTimestep("frozen_model.pb", "./python/logs/");
		//		cont.inputName = "tfrecord_input/split";
		//		cont.outputName = "softmax/Softmax";


		// CONTROLLER -- Approximate nearest neighbor.
		File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");

		File[] allFiles = saveLoc.listFiles();
		if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

		List<File> exampleDataFiles = new ArrayList<File>();
		int count = 0;
		for (File f : allFiles){
			if (f.getName().contains("TFRecord") && !f.getName().contains("recovery")) {
				System.out.println("Found save file: " + f.getName());
				if (count <20 ) { 
				exampleDataFiles.add(f);
				}

				count++;
			}
		}
		Controller_NearestNeighborApprox cont = new Controller_NearestNeighborApprox(exampleDataFiles);
		mc.controller = cont;
		mc.doControlled();

		//		// Have the server do the calculations for me.
		//		Controller_NearestNeighborApprox subCont = new Controller_NearestNeighborApprox(new ArrayList<File>());
		//		//subCont.comparePreviousStates = false;
		////		subCont.upperSetLimit = 50;
		////		subCont.lowerSetLimit = 50;
		//		subCont.penalizeEndOfSequences = false;
		//		//subCont.enableVoting = true;
		//		//subCont.maxPenaltyForEndOfSequence = 0f;
		//		subCont.previousStatePenaltyMult = 0.9f;
		//		subCont.enableTrajectorySnapping = false;
		//		subCont.trajectorySnappingThreshold = 5f;
		//		Controller_AskServer serverCont = new Controller_AskServer(subCont);
		//
		//		mc.controller = serverCont;
		//		mc.setup();
		//		mc.doControlled();
		//		
		//		serverCont.closeAll();

		//		Client grabData = new Client();
		//		Controller_NearestNeighborApprox cont = new Controller_NearestNeighborApprox(new ArrayList<File>());
		//		
		//		try {
		//			grabData.initialize();
		//			System.out.println("Asking server for run data...");
		//			grabData.sendObject("runs");
		//			cont.runs = (Set<RunHolder>) grabData.receiveObject();
		//			System.out.println("Complete...");
		//			
		//			System.out.println("Asking server for state data...");
		//			grabData.sendObject("states");
		//			cont.allStates = (NavigableMap<Float, StateHolder>) grabData.receiveObject();
		//			System.out.println("Complete...");
		//			
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		} catch (ClassNotFoundException e) {
		//			e.printStackTrace();
		//		}finally {
		//			grabData.closeAll();
		//		}
		//		mc.controller = cont;
		//		mc.setup();
		//		mc.doControlled();

	}

	public void setup() {
		if (doScreenCapture)  screenCap = new ScreenCapture(new File(Utility.getExcutionPath() + "./" + Utility.generateFileName("vid", "mp4")));
		game.mainRunnerStroke = new BasicStroke(5);
		mainViewPanel = new Panel();
		this.setLayout(new BorderLayout());
		add(mainViewPanel, BorderLayout.CENTER);

		Thread graphicsThread = new Thread(this);
		graphicsThread.start(); // Makes it smoother by updating the graphics faster than the timestep updates.

		JButton saveButton = new JButton();
		saveButton.setText("Save actions");
		saveButton.addActionListener(this);
		saveButton.setVisible(true);
		saveButton.setPreferredSize(new Dimension(1000,50));
		add(saveButton, BorderLayout.PAGE_END);

		game.mainRunnerColor = Color.ORANGE;
		mainViewPanel.setBackground(backgroundColor);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(windowWidth, windowHeight));
		setContentPane(this.getContentPane());
		pack();

		setVisible(true); 
		repaint();

		if (doScreenCapture) {
			// Save a progress log before shutting down.
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						screenCap.finalize();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}}); 
		}
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
			Random rand = new Random();
			while (!actionQueue.isEmpty()) {

				
//									if (rand.nextFloat() > 0.5f) {
//										try {
//											game.applyBodyImpulse(rand.nextFloat() - 0.5f, rand.nextFloat() - 0.5f);
//										} catch (IllegalAccessException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										} catch (IllegalArgumentException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										} catch (InvocationTargetException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										} catch (NoSuchMethodException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										} catch (SecurityException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										} catch (InstantiationException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										}
//									}
									//game.applyBodyTorque(-2f);
								

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
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	int count = 0;
	/** Pop the next action off the queue and execute one timestep. **/
	private void executeNextOnQueue() {
		if (!actionQueue.isEmpty()) {

			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			boolean Q = nextCommand[0];
			boolean W = nextCommand[1]; 
			boolean O = nextCommand[2];
			boolean P = nextCommand[3];
			//if (doScreenCapture) screenCap.takeFrame(screenCapRectangle);
			game.stepGame(Q,W,O,P);
			try {
				if (doScreenCapture && count++ > 20) screenCap.takeFrameFromJPanel(mainViewPanel);

			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}

	private class Panel extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			if (!game.initialized) return;
			super.paintComponent(g);
			if (game != null) {
				controller.draw(g, game, runnerScaling, xOffsetPixels-(int)(runnerScaling*2.5f), yOffsetPixels); // Optionally, the controller may want to draw some stuff for debugging.
				game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);

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
