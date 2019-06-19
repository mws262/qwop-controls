package ui.runner;

import game.GameUnified;
import game.IGameInternal;
import game.state.State;

import java.awt.*;
import java.util.Queue;

public class PanelRunner_AnimatedFromStates extends PanelRunner implements Runnable {

    /**
     * Is the current simulation paused?
     */
    private boolean pauseFlag = false;

    /**
     * This panel's copy of the game it uses to run games for visualization.
     */
    protected IGameInternal game;

    /**
     * States to animate through.
     */
    private Queue<State> states;

    /**
     * Current state being displayed.
     */
    private State currState;

    public PanelRunner_AnimatedFromStates() {
        game = new GameUnified();
        game.makeNewWorld();
    }

    public void simRun(Queue<State> states) {
        this.states = states;
        active = true;
    }

    /**
     * Gets auto-called by the goals graphics manager.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        if (game != null && currState != null) {

            GameUnified.drawExtraRunner((Graphics2D) g, currState, "", runnerScaling,
                    (int) (xOffsetPixels - currState.body.getX() * runnerScaling), yOffsetPixels, Color.BLACK,
                    normalStroke);

            // No game.action being displayed, so just draw the keys.
            keyDrawer(g, false, false, false, false);

            //This draws the "road" markings to show that the ground is moving relative to the dude.
            for (int i = 0; i < 2000 / 69; i++) {
                g.drawString("_", ((-(int) (runnerScaling * currState.body.getX()) - i * 70) % 2000) + 2000,
                        yOffsetPixels + 92);
            }
            //drawActionString(g, actionQueue.getActionsInCurrentRun(), actionQueue.getCurrentActionIdx());
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            if (active && !pauseFlag) {
                if (game != null) {
                    if (states != null && !states.isEmpty()) {
                        currState = states.poll();
                    }
                }
            }
            try {
                // How long the panel pauses between drawing in millis. Assuming that simulation basically takes no
                // time.
                long displayPause = 35;
                Thread.sleep(displayPause);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
    public void deactivateTab() {
        active = false;
    }

    /**
     * Check if the current run is finished.
     */
    public boolean isFinishedWithRun() {
        return states.isEmpty();
    }
}
