package main;

import java.awt.Graphics;

public class PanelRunner_Animated extends PanelRunner implements Runnable{

	private static final long serialVersionUID = 1L;

	/** Is this panel still listening and ready to draw? Only false if thread is being killed. **/
	private boolean running = true;
	
	/** Is the current simulation paused? **/
	private boolean pauseFlag = false;

	/** This panel's copy of the game it uses to run games for visualization. **/
	protected QWOPGame game;

	/** Stores the qwop actions we're going to execute. **/
	private ActionQueue actionQueue = new ActionQueue();

	/** How long the panel pauses between drawing in millis. Assuming that simulation basically takes no time. **/
	private long displayPause = 35;

	/** Current status of each keypress. **/
	private boolean Q = false;
	private boolean W = false;
	private boolean O = false;
	private boolean P = false;

	public PanelRunner_Animated() {}

	/** Give this panel a node to simulate and draw to. If a new node is supplied while another
	 * is active, then terminate the previous and start the new one.**/
	public void simRunToNode(Node node) {
		actionQueue.clearAll();
		game = new QWOPGame();
		actionQueue.addSequence(node.getSequence());
	}

	/** Pop the next action off the queue and execute one timestep. **/
	private void executeNextOnQueue() {
		if (!actionQueue.isEmpty()) {
			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			Q = nextCommand[0];
			W = nextCommand[1]; 
			O = nextCommand[2];
			P = nextCommand[3];
			game.stepGame(Q,W,O,P);
		}
	}

	/** Gets autocalled by the main graphics manager. **/
	public void paintComponent(Graphics g) {
		if (!active) return;
		super.paintComponent(g);
		if (game != null) {
			game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
			keyDrawer(g, Q, W, O, P);
			drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
		}
	}

	@Override
	public void run() {
		while (running) {
			if (active && !pauseFlag) {
				if (game != null) {
					executeNextOnQueue();
				}
			}
			try {
				Thread.sleep(displayPause);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** Play/pause the current visualized simulation. Flag is reset by calling again or by selecting a new node to visualize. **/
	public void pauseToggle() {
		pauseFlag = !pauseFlag;
	}
}
