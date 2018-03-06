package main;

import java.awt.Graphics;
import javax.swing.JPanel;
import main.IUserInterface.TabbedPaneActivator;

public class Panel_Runner extends JPanel implements Runnable, TabbedPaneActivator{

	/** Is this panel still listening and ready to draw? Only false if thread is being killed. **/
	private boolean running = true;

	/** Should this panel be drawing or is it hidden. **/
	private boolean active = true;

	/** This panel's copy of the game it uses to run games for visualization. **/
	private QWOPGame game;

	/** Stores the qwop actions we're going to execute. **/
	private ActionQueue actionQueue = new ActionQueue();

	/** How long the panel pauses between drawing in millis. Assuming that simulation basically takes no time. **/
	private long displayPause = 35;

	/** Runner coordinates to pixels. **/
	public float runnerScaling = 10f;

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	public int xOffsetPixels = 960;
	public int yOffsetPixels = 100;


	/** Current status of each keypress. **/
	private boolean Q = false;
	private boolean W = false;
	private boolean O = false;
	private boolean P = false;

	public Panel_Runner() {}


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

	public void paintComponent(Graphics g) {
		if (!active) return;
		super.paintComponent(g);
		if (game != null) {
			game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
		}
	}

	@Override
	public void run() {
		while (running) {
			if (active) {
				//System.out.print("HI");
				if (game != null) {
					//System.out.println("HI");
					executeNextOnQueue();
				}

				try {
					Thread.sleep(displayPause);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void activateTab() {
		active = true;
	}

	@Override
	public void deactivateTab() {
		active = false;

	}
	
	@Override
	public boolean isActive() {
		return active;
	}
}
