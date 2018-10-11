package ui;

import java.awt.Graphics;

import game.GameLoader;
import game.State;
import main.Action;
import main.ActionQueue;
import main.Node;
import main.PanelRunner;

public class PanelRunner_Animated extends PanelRunner implements Runnable {

    private static final long serialVersionUID = 1L;

    /**
     * Is the current simulation paused?
     **/
    private boolean pauseFlag = false;

    /**
     * This panel's copy of the game it uses to run games for visualization.
     **/
    protected GameLoader game;

    /**
     * Stores the qwop actions we're going to execute.
     **/
    private ActionQueue actionQueue = new ActionQueue();

    /**
     * Current status of each keypress.
     **/
    private boolean Q = false;
    private boolean W = false;
    private boolean O = false;
    private boolean P = false;

    /**
     * Gets set to non-zero by simRunToNode if the start node is not root. Lets us not have to watch the animation of
     * the beginning of a long run.
     **/
    private int fastForwardTimesteps = 0;

    public PanelRunner_Animated() {
        game = new GameLoader();
        //this.setMinimumSize(new Dimension(100,100));
    }

    /**
     * Give this panel a node to simulate and draw to. If a new node is supplied while another
     * is active, then terminate the previous and start the new one.
     **/
    public void simRunToNode(Node node) {
        fastForwardTimesteps = 0;
        actionQueue.clearAll();
        game.makeNewWorld();
        Action[] actionSequence = node.getSequence();
        actionQueue.addSequence(actionSequence);
        fastForwardTimesteps = 0;
        for (Action a : actionSequence) {
            System.out.println(a);
        }

    }

    /**
     * This version only animates the actions between startNode and endNode. Still simulates all of course.
     **/
    public void simRunToNode(Node startNode, Node endNode) {
        simRunToNode(endNode);

        Node currNode = startNode;
        while (currNode.treeDepth > 0) {
            fastForwardTimesteps += currNode.getAction().getTimestepsTotal();
            currNode = currNode.getParent();
        }
    }

    /**
     * Add a single action to the end of what is already going on.
     **/
    public void addAction(Action action) {
        actionQueue.addAction(action);
    }

    /**
     * Pop the next action off the queue and execute one timestep.
     **/
    private void executeNextOnQueue() {
        if (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            Q = nextCommand[0];
            W = nextCommand[1];
            O = nextCommand[2];
            P = nextCommand[3];
            game.stepGame(Q, W, O, P);
            State st = game.getCurrentState();
            System.out.println(st.body.x + "," + st.body.dx + "," + game.getTimestepsSimulated());
        }
    }

    public boolean isFinishedWithRun() {
        return (actionQueue.isEmpty());
    }

    /**
     * Gets autocalled by the main graphics manager.
     **/
    @Override
    public void paintComponent(Graphics g) {
        if (!active && game.initialized) return;
        super.paintComponent(g);
        if (game != null) {
            game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
            keyDrawer(g, Q, W, O, P);
            drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
        }
    }

    @Override
    public void run() {
        /** Is this panel still listening and ready to draw? Only false if thread is being killed. **/
        boolean running = true;
        while (running) {
            if (active && !pauseFlag) {
                if (game != null) {
                    executeNextOnQueue();
                }
            }
            if (fastForwardTimesteps-- <= 0) {
                try {
                    /** How long the panel pauses between drawing in millis. Assuming that simulation basically takes
					 *  no time. **/
                    long displayPause = 35;
                    Thread.sleep(displayPause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Play/pause the current visualized simulation. Flag is reset by calling again or by selecting a new node to
     * visualize.
     **/
    public void pauseToggle() {
        pauseFlag = !pauseFlag;
    }

    @Override
    public void deactivateTab() {
        actionQueue.clearAll();
        active = false;
    }

    @Override
    public void update(Node node) {
        simRunToNode(node);
    }
}
