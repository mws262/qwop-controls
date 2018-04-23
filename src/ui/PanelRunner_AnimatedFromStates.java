package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Queue;

import game.GameLoader;
import game.State;
import main.Action;
import main.ActionQueue;
import main.Node;
import main.PanelRunner;

public class PanelRunner_AnimatedFromStates extends PanelRunner implements Runnable{

	private static final long serialVersionUID = 1L;

	/** Is this panel still listening and ready to draw? Only false if thread is being killed. **/
	private boolean running = true;

	/** Is the current simulation paused? **/
	private boolean pauseFlag = false;

	/** This panel's copy of the game it uses to run games for visualization. **/
	protected GameLoader game;

	/** How long the panel pauses between drawing in millis. Assuming that simulation basically takes no time. **/
	private long displayPause = 35;

	/** States to animate through. **/
	private Queue<State> states;

	/** Current state being displayed. **/
	private State currState;

	/** Current status of each keypress. **/
	private boolean Q = false;
	private boolean W = false;
	private boolean O = false;
	private boolean P = false;

	public PanelRunner_AnimatedFromStates() {
		game = new GameLoader();
		game.makeNewWorld();
		//this.setMinimumSize(new Dimension(100,100));
	}

	public void simRun(Queue<State> states) {
		this.states = states;
		active = true;
	}

	/** Gets autocalled by the main graphics manager. **/
	@Override
	public void paintComponent(Graphics g) {
		if (!active && game.initialized) return;
		super.paintComponent(g);
		if (game != null && currState != null) {

			game.drawExtraRunner((Graphics2D)g, currState, "", runnerScaling, (int)(xOffsetPixels - currState.body.x*runnerScaling), yOffsetPixels, Color.BLACK, normalStroke);
			keyDrawer(g, Q, W, O, P);
			
			//This draws the "road" markings to show that the ground is moving relative to the dude.
			for (int i = 0; i < 2000/69; i++) {
				g.drawString("_", ((-(int)(runnerScaling * currState.body.x) - i * 70) % 2000) + 2000, yOffsetPixels + 92);
			}
			//drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
		}
	}

	@Override
	public void run() {
		while (running) {
			if (active && !pauseFlag) {
				if (game != null) {
					if (states != null && !states.isEmpty()) {
						currState = states.poll();
					}
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

	@Override
	public void deactivateTab() {
		active = false;
	}

	@Override
	public void update(Node node) {}


	/** Check if the current run is finished. **/
	public boolean isFinishedWithRun() {
		return states.isEmpty();
	}
}
