package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import game.GameUnified;
import game.IGame;
import game.State;
import tree.Node;

/**
 * Simple runner visualizer that either takes a node and gets its state or takes a state
 * directly and displays it.
 *
 * @author matt
 */
public class PanelRunner_SimpleState extends PanelRunner implements Runnable {
    /**
     * Access to the game for the sake of the drawing methods.
     */
    private IGame game = new GameUnified();

    /**
     * Current state being displayed.
     */
    private State currentState;

    /**
     * Update the state to be displayed.
     */
    public void updateState(State state) {
        currentState = state;
    }

    @Override
    public void update(Node node) {
        currentState = node.getState();
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    /**
     * Draws the selected node state and potentially previous and future states.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (!active || currentState == null) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        GameUnified.drawExtraRunner(g2, currentState, "", runnerScaling,
				500 - (int) (currentState.body.getX() * runnerScaling), yOffsetPixels + 100, Color.BLACK, normalStroke);
    }

    @Override
    public void run() {
        while (true) {
            repaint();
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
