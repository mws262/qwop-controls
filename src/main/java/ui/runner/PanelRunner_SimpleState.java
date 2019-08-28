package ui.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.qwop.GameQWOP;
import game.qwop.CommandQWOP;
import game.state.IState;
import tree.node.NodeGameGraphicsBase;

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

    private Thread thread;

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
    public void update(NodeGameGraphicsBase<?, CommandQWOP> node) {
        currentState = node.getState();
    }

    /**
     * Draws the selected node state and potentially previous and future states.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (!active || currentState == null) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        GameQWOP.drawExtraRunner(g2, currentState, "", runnerScaling,
				500 - (int) (currentState.getCenterX() * runnerScaling),
                yOffsetPixels + 100,
                Color.BLACK,
                normalStroke);
    }

    @Override
    public void run() {
        while (active) {
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
}
