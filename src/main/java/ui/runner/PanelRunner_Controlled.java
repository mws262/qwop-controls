package ui.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.GameUnified;
import game.IGameInternal;
import game.action.Action;
import game.action.ActionQueue;
import game.action.IActionGenerator;
import tree.node.NodeQWOPExplorable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class PanelRunner_Controlled extends PanelRunner implements Runnable, ActionListener {

    IController controller;
    GameUnified game;
    private ActionQueue actionQueue = new ActionQueue();

    private Action mostRecentAction;

    private boolean active = false;
    private JButton resetButton;
    private Thread thread;
    public final String name;

    public IActionGenerator actionGenerator;

    GridBagLayout layout = new GridBagLayout();
    final int layoutRows = 25;
    final int layoutColumns = 15;
    GridBagConstraints constraints = new GridBagConstraints();

    public PanelRunner_Controlled(@JsonProperty("name") String name, GameUnified game, IController controller) {
        this.name = name;
        this.controller = controller;
        this.game = game;

        resetButton = new JButton("Restart");
        resetButton.addActionListener(this);
        resetButton.setPreferredSize(new Dimension(50, 25));


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
        JPanel placeholder = new JPanel();

        placeholder.setPreferredSize(new Dimension(75,25));
        this.add(placeholder, constraints);


        constraints.gridx = 0;
        constraints.gridy = layoutRows - 1;
        this.add(resetButton, constraints);

    }

    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        if (game != null) {
            game.draw(g, runnerScaling, xOffsetPixels, yOffsetPixels);
            boolean[] mostRecentKeys = actionQueue.isEmpty() ? new boolean[]{false, false, false, false} :
                    mostRecentAction.peek();
            keyDrawer(g, mostRecentKeys[0], mostRecentKeys[1], mostRecentKeys[2], mostRecentKeys[3]);
        }
    }

    @Override
    public void run() {
        game.makeNewWorld();
        actionQueue.clearAll();
        NodeQWOPExplorable node = null;
        while(active) {
            // Get another action from the controller if the queue is exhausted.
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
                mostRecentAction = controller.policy(node);
                actionQueue.addAction(mostRecentAction);
            }

            game.step(actionQueue.pollCommand());

            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void activateTab() {
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
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(resetButton)) {
            deactivateTab();
            activateTab();
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
