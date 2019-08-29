package ui.runner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import tree.Utility;
import tree.node.NodeGameGraphicsBase;

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
    private StateQWOP mainState;

    private Thread thread;

    /**
     * Additional states to draw, and their colors. x-coordinate will be relative to the mainState.
     */
    private Map<StateQWOP, Color> secondaryStates = new ConcurrentHashMap<>();

    @JsonSerialize(using = Utility.ColorSerializer.class)
    @JsonDeserialize(using = Utility.ColorDeserializer.class)
    public Color mainRunnerColor = Color.BLACK;

    transient public Stroke customStrokeExtra;

    private int[] offset = new int[2];

    private final String name;

    public PanelRunner_MultiState(@JsonProperty("name") String name) {
        this.name = name;
    }

    /**
     * Add a state to be displayed.
     * @param state StateQWOP to draw the runner at.
     * @param color Color to draw the runner outlined with.
     */
    public void addSecondaryState(StateQWOP state, Color color) {
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
    public void setMainState(StateQWOP state) {
        mainState = state;
    }

    @Override
    public void update(NodeGameGraphicsBase<?, CommandQWOP, StateQWOP> node) {
        mainState = node.getState();
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
        int xOffset = (int) (mainState.getCenterX() * runnerScaling);

        offset[0] = xOffsetPixels - xOffset;
        offset[1] = 100 + yOffsetPixels;

        // Draw the main state.
        GameQWOP.drawExtraRunner(g2, mainState, "", runnerScaling,
                offset[0], offset[1], mainRunnerColor, (customStroke != null) ? customStroke : boldStroke);

        // Draw secondary states, if any.
        for (Map.Entry<StateQWOP, Color> entry : secondaryStates.entrySet()) {
            StateQWOP st = entry.getKey();
            Color col = entry.getValue();
            GameQWOP.drawExtraRunner(g2, st, "", runnerScaling,
                    offset[0], offset[1], col, (customStrokeExtra != null) ? customStrokeExtra : normalStroke);
        }

        //This draws the "road" markings to show that the ground is moving relative to the dude.
        for (int i = 0; i < 2000 / 69; i++) {
            g.drawString("_", ((-(int) (runnerScaling * mainState.getCenterX()) - i * 70) % 2000) + 2000,
                    yOffsetPixels + 195);
        }
    }

    @Override
    public void run() {
        while (active) {
            repaint();
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void activateTab() {
        active = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void deactivateTab() {
        active = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @JsonIgnore
    public int[] getOffset() {
        return offset;
    }
}
