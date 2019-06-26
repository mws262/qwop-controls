package ui.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.GameUnified;
import game.state.IState;
import tree.node.NodeQWOPGraphicsBase;

import java.awt.*;

/**
 * Simple runner visualizer that either takes a node and gets its state or takes a state
 * directly and displays it.
 *
 * @author matt
 */
public class PanelRunner_SimpleState extends PanelRunner implements Runnable {

    /**
     * Current state being displayed.
     */
    private IState currentState;

    private final String name;

    public PanelRunner_SimpleState(@JsonProperty("name") String name) {
        this.name = name;
    }

    /**
     * Update the state to be displayed.
     */
    public void updateState(IState state) {
        currentState = state;
    }

    @Override
    public void update(NodeQWOPGraphicsBase<?> node) {
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
				500 - (int) (currentState.getCenterX() * runnerScaling),
                yOffsetPixels + 100,
                Color.BLACK,
                normalStroke);
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            repaint();
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
