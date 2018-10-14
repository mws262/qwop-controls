package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import game.GameLoader;
import game.State;
import main.Node;
import main.PanelRunner;

/**
 * Simple runner displayer that either takes a node and gets its state or takes a state
 * directly and displays it.
 *
 * @author matt
 */
public class PanelRunner_SimpleState extends PanelRunner implements Runnable {
    private static final long serialVersionUID = 1L;

    /**
     * Access to the game for the sake of the drawing methods.
     **/
    private GameLoader game = new GameLoader();

    /**
     * Current state being displayed.
     **/
    private State currentState;

    @Override
    public void update(Node node) {
        currentState = node.getState();
    }

    /**
     * Update the state to be displayed.
     **/
    public void updateState(State state) {

        currentState = state;
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    /**
     * Draws the selected node state and potentially previous and future states.
     **/
    @Override
    public void paintComponent(Graphics g) {
        if (!active || currentState == null) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        game.drawExtraRunner(g2, game.getXForms(currentState), "", runnerScaling,
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
