package ui.runner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Action;
import game.action.ActionQueue;
import game.GameUnified;
import game.IGameInternal;
import game.action.CommandQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphicsBase;

import java.awt.Graphics;
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
    protected IGameInternal<CommandQWOP> game;

    /**
     * Stores the QWOP game.action we're going to execute.
     */
    private ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();

    private Thread thread;

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

    private static final Logger logger = LogManager.getLogger(PanelRunner_Animated.class);

    private final String name;

    public PanelRunner_Animated(@JsonProperty("name") String name) {
        this.name = name;
        game = new GameUnified();
    }

    /**
     * Give this panel a node to simulate and draw to. If a new node is supplied while another
     * is active, then terminate the previous and start the new one.
     */
    public void simRunToNode(NodeQWOPExplorableBase<?, CommandQWOP> node) {
        assert node.getTreeDepth() > 0; // Doesn't make sense to simulate to the starting configuration.

        fastForwardTimesteps = 0;
        actionQueue.clearAll();
        game.makeNewWorld();
        List<Action<CommandQWOP>> actionList = new ArrayList<>();
        node.getSequence(actionList);
        actionQueue.addSequence(actionList);
        fastForwardTimesteps = 0;
        logger.info("Animating sequence.");
        for (Action a : actionList) {
            logger.info(a);
        }
    }

    /**
     * This version only animates the game.action between startNode and endNode. Still simulates all of course.
     */
    public void simRunToNode(NodeQWOPExplorableBase<?, CommandQWOP> startNode,
                             NodeQWOPExplorableBase<?, CommandQWOP> endNode) {
        simRunToNode(endNode);

        NodeQWOPExplorableBase<?, CommandQWOP> currNode = startNode;
        while (currNode.getTreeDepth() > 0) {
            fastForwardTimesteps += currNode.getAction().getTimestepsTotal();
            currNode = currNode.getParent();
        }
    }

    /**
     * Add a single action to the end of what is already going on.
     */
    public void addAction(Action<CommandQWOP> action) {
        actionQueue.addAction(action);
    }

    /**
     * Pop the next action off the queue and execute one timestep.
     */
    private void executeNextOnQueue() {
        if (!actionQueue.isEmpty()) {
            CommandQWOP nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            Q = nextCommand.get()[0];
            W = nextCommand.get()[1];
            O = nextCommand.get()[2];
            P = nextCommand.get()[3];
            game.step(nextCommand);
        }
    }

    @JsonIgnore
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
        if (!pauseFlag) {
            if (game != null) {
                executeNextOnQueue();
            }
        }
        if (game != null) {
            game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
            keyDrawer(g, Q, W, O, P);
            if (!actionQueue.isEmpty())
                drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
        }
    }

    @Override
    public void run() {
        while (active) {
            if (!pauseFlag) {
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
    public void activateTab() {
        actionQueue.clearAll();
        active = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void deactivateTab() {
        actionQueue.clearAll();
        active = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(NodeQWOPGraphicsBase<?, CommandQWOP> node) {
        if (node.getTreeDepth() > 0)
            simRunToNode(node);
    }

    @Override
    public String getName() {
        return name;
    }
}
