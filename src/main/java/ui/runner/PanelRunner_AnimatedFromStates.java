package ui.runner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.qwop.GameQWOP;
import game.qwop.IStateQWOP;
import game.qwop.StateQWOP;

import java.awt.*;
import java.util.Queue;

public class PanelRunner_AnimatedFromStates<S extends IStateQWOP> extends PanelRunner<S> implements Runnable {

    /**
     * Is the current simulation paused?
     */
    private boolean pauseFlag = false;

    /**
     * This panel's copy of the game it uses to run games for visualization.
     */
    protected GameQWOP game;

    private Thread thread;

    /**
     * States to animate through.
     */
    private Queue<StateQWOP> states;

    /**
     * Current state being displayed.
     */
    private StateQWOP currState;

    private final String name;

    public PanelRunner_AnimatedFromStates(@JsonProperty("name") String name) {
        this.name = name;
        game = new GameQWOP();
        game.resetGame();
    }

    public void simRun(Queue<StateQWOP> states) {
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

            GameQWOP.drawExtraRunner((Graphics2D) g, currState, "", runnerScaling,
                    (int) (xOffsetPixels - currState.body.getX() * runnerScaling), yOffsetPixels, Color.BLACK,
                    normalStroke);

            // No game.command being displayed, so just draw the keys.
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
        while (active) {
            if (!pauseFlag) {
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
     * Play/pause the current visualized simulation. Flag is resetGame by calling again or by selecting a new node to
     * visualize.
     */
    public void pauseToggle() {
        pauseFlag = !pauseFlag;
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
    /**
     * Check if the current run is finished.
     */
    @JsonIgnore
    public boolean isFinishedWithRun() {
        return states.isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }
}
