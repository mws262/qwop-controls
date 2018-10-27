package ui;

import game.GameLoader;
import game.State;
import tree.Node;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * An slightly more fully-featured version of {@link PanelRunner_SimpleState}. Allows drawing of multiple states.
 * @author matt
 */
public class PanelRunner_MultiState extends PanelRunner implements Runnable {
    /**
     * Access to the game for the sake of the drawing methods.
     */
    private GameLoader game = new GameLoader();


    /**
     * Main state to draw. It will provide the x-reference position.
     */
    private State mainState;

    /**
     * Additional states to draw, and their colors. x-coordinate will be relative to the mainState.
     */
    private Map<State, Color> secondaryStates = new HashMap<>();

    /**
     * Add a state to be displayed.
     * @param state State to draw the runner at.
     * @param color Color to draw the runner outlined with.
     */
    public void addSecondaryState(State state, Color color) {
        secondaryStates.put(state, color);
    }

    /**
     * Remove all drawn secondary states. Main state will remain.
     */
    public void clearSecondaryStates() {
        secondaryStates.clear();
    }

    /**
     * Set the main state to be drawn. It's body x-coordinate will be used as an offset for all other secondary
     * states drawn.
     * @param state Main runner state to be drawn.
     */
    public void setMainState(State state) {
        mainState = state;
    }

    @Override
    public void update(Node node) {
        mainState = node.getState();
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
        if (!active || mainState == null) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // x offset comes from the main state and is applied to all states drawn.
        int xOffset = (int) (mainState.body.getX() * runnerScaling);

        // Draw the main state.
        game.drawExtraRunner(g2, game.getXForms(mainState), "", runnerScaling,
				500 - xOffset, yOffsetPixels + 100, Color.BLACK, boldStroke);

        // Draw secondary states, if any.
        for (State st : secondaryStates.keySet()) {
            game.drawExtraRunner(g2, game.getXForms(st), "", runnerScaling,
                    500 - xOffset, yOffsetPixels + 100, secondaryStates.get(st), normalStroke);
        }
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
