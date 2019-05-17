package ui;

import game.GameUnified;
import game.State;
import tree.NodeQWOPGraphicsBase;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An slightly more fully-featured version of {@link PanelRunner_SimpleState}. Allows drawing of multiple states.
 * @author matt
 */
public class PanelRunner_MultiState extends PanelRunner implements Runnable {
    /**
     * Main state to draw. It will provide the x-reference position.
     */
    private State mainState;

    /**
     * Additional states to draw, and their colors. x-coordinate will be relative to the mainState.
     */
    private Map<State, Color> secondaryStates = new ConcurrentHashMap<>();
    public Color mainRunnerColor = Color.BLACK;
    public Stroke customStrokeExtra;

    public int[] offset = new int[2];
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
    public void update(NodeQWOPGraphicsBase<?> node) {
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

        offset[0] = xOffsetPixels - xOffset;
        offset[1] = 100 + yOffsetPixels;

        // Draw the main state.
        GameUnified.drawExtraRunner(g2, mainState, "", runnerScaling,
                offset[0], offset[1], mainRunnerColor, (customStroke != null) ? customStroke : boldStroke);

        // Draw secondary states, if any.
        for (Map.Entry<State, Color> entry : secondaryStates.entrySet()) {
            State st = entry.getKey();
            Color col = entry.getValue();
            GameUnified.drawExtraRunner(g2, st, "", runnerScaling,
                    offset[0], offset[1], col, (customStrokeExtra != null) ? customStrokeExtra : normalStroke);
        }

        //This draws the "road" markings to show that the ground is moving relative to the dude.
        for (int i = 0; i < 2000 / 69; i++) {
            g.drawString("_", ((-(int) (runnerScaling * mainState.body.getX()) - i * 70) % 2000) + 2000,
                    yOffsetPixels + 195);
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
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
