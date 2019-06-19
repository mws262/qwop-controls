package ui.runner;

import game.actions.Action;
import game.actions.ActionQueue;
import game.GameUnified;
import game.IGameInternal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphicsBase;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PanelRunner_Animated extends PanelRunner implements Runnable {

    /**
     * Is the current simulation paused?
     */
    private boolean pauseFlag = false;

    /**
     * This panel's copy of the game it uses to run games for visualization.
     */
    protected IGameInternal game;

    /**
     * Stores the QWOP game.actions we're going to execute.
     */
    private ActionQueue actionQueue = new ActionQueue();

    /**
     * Current status of each keypress.
     */
    private boolean Q = false;
    private boolean W = false;
    private boolean O = false;
    private boolean P = false;

    /**
     * Gets set to non-zero by simRunToNode if the start node is not root. Lets us not have to watch the animation of
     * the beginning of a long run.
     */
    private int fastForwardTimesteps = 0;

    private Logger logger = LogManager.getLogger(PanelRunner_Animated.class);

    public PanelRunner_Animated() {
        game = new GameUnified();
    }

    /**
     * Give this panel a node to simulate and draw to. If a new node is supplied while another
     * is active, then terminate the previous and start the new one.
     */
    public void simRunToNode(NodeQWOPExplorableBase<?> node) {
        assert node.getTreeDepth() > 0; // Doesn't make sense to simulate to the starting configuration.

        fastForwardTimesteps = 0;
        actionQueue.clearAll();
        game.makeNewWorld();
        List<Action> actionList = new ArrayList<>();
        node.getSequence(actionList);
        actionQueue.addSequence(actionList);
        fastForwardTimesteps = 0;
        logger.info("Animating sequence.");
        for (Action a : actionList) {
            logger.info(a);
        }
    }

    /**
     * This version only animates the game.actions between startNode and endNode. Still simulates all of course.
     */
    public void simRunToNode(NodeQWOPExplorableBase<?> startNode, NodeQWOPExplorableBase<?> endNode) {
        simRunToNode(endNode);

        NodeQWOPExplorableBase<?> currNode = startNode;
        while (currNode.getTreeDepth() > 0) {
            fastForwardTimesteps += currNode.getAction().getTimestepsTotal();
            currNode = currNode.getParent();
        }
    }

    /**
     * Add a single action to the end of what is already going on.
     */
    public void addAction(Action action) {
        actionQueue.addAction(action);
    }

    /**
     * Pop the next action off the queue and execute one timestep.
     */
    private void executeNextOnQueue() {
        if (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            Q = nextCommand[0];
            W = nextCommand[1];
            O = nextCommand[2];
            P = nextCommand[3];
            game.step(Q, W, O, P);
        }
    }

    public boolean isFinishedWithRun() {
        return (actionQueue.isEmpty());
    }

    /**
     * Gets auto-called by the goals graphics manager.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        if (game != null) {
            game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
            keyDrawer(g, Q, W, O, P);
            if (!actionQueue.isEmpty())
                drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            if (active && !pauseFlag) {
                if (game != null) {
                    executeNextOnQueue();
                }
            }
            if (fastForwardTimesteps-- <= 0) {
                try {
                    // How long the panel pauses between drawing in millis. Assuming that simulation basically takes
                    // no time.
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
     */
    public void pauseToggle() {
        pauseFlag = !pauseFlag;
    }

    @Override
    public void deactivateTab() {
        actionQueue.clearAll();
        active = false;
    }

    @Override
    public void update(NodeQWOPGraphicsBase<?> node) {
        if (node.getTreeDepth() > 0)
            simRunToNode(node);
    }
}
