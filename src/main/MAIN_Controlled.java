package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import controllers.Controller_Null;
import controllers.Controller_Tensorflow_ClassifyActionsPerTimestep;
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
	private int doPrefixToDepth = 12;

	private List<Node> leafNodes = new ArrayList<Node>();

	private SaveableFileIO<SaveableActionSequence> actionSaver = new SaveableFileIO<SaveableActionSequence>();

	private String savePath = Utility.getExcutionPath() + "saved_data/individual_expansions_todo/";
	
	private ActionQueue actionQueue = new ActionQueue();

	public static void main(String[] args) {
		MAIN_Controlled mc = new MAIN_Controlled();
		
		// Pick the exact controller and its settings.
		Controller_Tensorflow_ClassifyActionsPerTimestep cont = new Controller_Tensorflow_ClassifyActionsPerTimestep("frozen_model.pb", "./python/logs/");
		cont.inputName = "tfrecord_input/split";
		cont.outputName = "softmax/Softmax";
		mc.controller = cont;

		mc.setup();
		mc.doControlled();
	}

	public void setup() {
		Panel panel = new Panel();
		this.setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);

		Thread graphicsThread = new Thread(this);
		graphicsThread.start(); // Makes it smoother by updating the graphics faster than the timestep updates.

		JButton saveButton = new JButton();
		saveButton.setText("Save actions");
		saveButton.addActionListener(this);
		saveButton.setVisible(true);
		saveButton.setPreferredSize(new Dimension(1000,50));
		add(saveButton, BorderLayout.PAGE_END);

		
		
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
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// Enter controller mode.
		while (true) {
			State state = game.getCurrentState();
			Action nextAction = controller.policy(state);
			actionQueue.addAction(nextAction);
			while (!actionQueue.isEmpty()) {
				executeNextOnQueue();
				try {
					Thread.sleep(40);
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
