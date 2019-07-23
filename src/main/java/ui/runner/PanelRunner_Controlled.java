package ui.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.IGameSerializable;
import game.action.Action;
import game.action.ActionQueue;
import game.action.IActionGenerator;
import tree.node.NodeQWOPExplorable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Basic panel for displaying the runner under controls. Inheriting from this class is the best way to get anything
 * besides basic behavior.
 *
 * @param <C> Controller type being used. Must implement the IController interface.
 * @param <G> Game implementation used. Must implement the IGameInternal interface.
 */
public class PanelRunner_Controlled<C extends IController, G extends IGameSerializable> extends PanelRunner implements ActionListener {

    /**
     * Controller being visualized.
     */
    C controller;

    /**
     * Game instance being used. Its state size should match what the controller needs.
     */
    G game;

    /**
     * Actions are queued as the controller provides them.
     */
    private ActionQueue actionQueue = new ActionQueue();

    /**
     * Action most recently returned by the controller.
     */
    private Action mostRecentAction;

    /**
     * Timer that handles updating the game and querying the controller when necessary.
     */
    private Timer controllerTimer;

    /**
     * Is this an active window? If its in a hidden tab or something, then we want to deactivate all the internal
     * behavior.
     */
    private boolean active = false;

    /**
     * Button for resetting the runner to the initial configuration.
     */
    private JButton resetButton;

    /**
     * Name of this panel. Used when inserted as a tab.
     */
    public final String name;

    /**
     * If the controller needs an assigned action generator for nodes, then this can be externally set. Not the best
     * solution, but not many controllers need this anyway.
     */
    public IActionGenerator actionGenerator;

    /**
     * Number of layout row slots for the layout manager.
     */
    final int layoutRows = 25;

    /**
     * Number of layout column slots for the layout manager.
     */
    final int layoutColumns = 15;

    /**
     * Constraints for the layout manager.
     */
    GridBagConstraints constraints = new GridBagConstraints();

    /**
     * Handles advancing the game and querying the controller.
     */
    private ControllerExecutor controllerExecutor;

    private JCheckBox pauseToggle;

    public PanelRunner_Controlled(@JsonProperty("name") String name, G game, C controller) {
        this.name = name;
        this.controller = controller;
        this.game = game;
        this.setName(name);

        // Reset button setup.
        resetButton = new JButton("Restart");
        resetButton.addActionListener(this);
        resetButton.setPreferredSize(new Dimension(50, 25));

        // Setup the panel layout.
        GridBagLayout layout = new GridBagLayout();
        layout.columnWeights = new double[layoutColumns];
        layout.rowWeights = new double[layoutRows];
        Arrays.fill(layout.columnWeights, 1);
        Arrays.fill(layout.rowWeights, 1);
        setLayout(layout);

        constraints.gridx = layoutColumns - 1;
        constraints.gridy = layoutRows - 1;
        constraints.ipady = 0;
        constraints.ipadx = 100;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0,0,0,0);
        constraints.anchor = GridBagConstraints.PAGE_END;
        JLabel gameDescription = new JLabel("Provided game state dimension: " + game.getStateDimension());
        add(gameDescription, constraints);

        constraints.gridx = 0;
        constraints.gridy = layoutRows - 1;
        add(resetButton, constraints);
        actionQueue.addAction(new Action(7, Action.Keys.none));

        constraints.gridx = 1;
        pauseToggle = new JCheckBox("Pause");
        pauseToggle.setToolTipText("Pause the controlled game simulation.");
        pauseToggle.setOpaque(false);
        add(pauseToggle, constraints);
    }

    /**
     * Can be useful to keep drawing the runner in a frozen position when an invalid controller is selected.
     */
    void activateDrawingButNotController() {
        active = true;
    }

    /**
     * Will be called before each game timestep. Override for anything specific.
     * @param game Game to apply a disturbance to.
     */
    void applyDisturbance(G game) {}

    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        if (game != null) {
            game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
            boolean[] mostRecentKeys = actionQueue.isEmpty() ? new boolean[]{false, false, false, false} :
                    actionQueue.peekThisAction().peek();
            keyDrawer(g, mostRecentKeys[0], mostRecentKeys[1], mostRecentKeys[2], mostRecentKeys[3]);
        }
    }

    @Override
    public void activateTab() {
        active = true;
        if (controller != null) {
            controllerTimer = new Timer();
            controllerExecutor = new ControllerExecutor();
            controllerExecutor.reset();
            controllerTimer.scheduleAtFixedRate(controllerExecutor, 0, 35);
        }
    }

    @Override
    public void deactivateTab() {
        actionQueue.clearAll();
        active = false;
        if (controllerTimer != null) {
            controllerTimer.cancel();
            controllerTimer.purge();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(resetButton)) {
            controllerExecutor.reset();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public G getGame() {
        return game;
    }

    /**
     * Handles advancing the game and querying the controller. Should be run on a timer.
     */
    private class ControllerExecutor extends TimerTask {

        private NodeQWOPExplorable node;

        /**
         * Reset the game.
         */
        public void reset() {
            game.makeNewWorld();
            actionQueue.clearAll();
            actionQueue.addAction(new Action(7, Action.Keys.none));
            node = null;
        }

        @Override
        public void run() {
            if (!pauseToggle.isSelected()) {
                // If the queue is out of actions, then ask the controller for a new one.
                if (actionQueue.isEmpty()) {
                    // Either make the first node since the game began, or add a child to the previous node.
                    if (node == null) {
                        if (actionGenerator != null) {
                            node = new NodeQWOPExplorable(game.getCurrentState(), actionGenerator);
                        } else {
                            node = new NodeQWOPExplorable(game.getCurrentState());
                        }
                    } else {
                        node = node.addBackwardsLinkedChild(mostRecentAction, game.getCurrentState());
                    }
                    mostRecentAction = controller.policy(node, game);
                    actionQueue.addAction(mostRecentAction);
                }
                applyDisturbance(game);
                game.step(actionQueue.pollCommand());
            }
        }
    }
}
