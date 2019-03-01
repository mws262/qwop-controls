package goals.cold_start_analysis;

import actions.Action;
import actions.ActionQueue;
import game.GameThreadSafe;
import ui.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;

/**
 * Some of the common setup used in the cold start tests.
 *
 * @author matt
 */
public abstract class CompareWarmStartToColdBase extends JFrame {

    /**
     * Panel for displaying potentially multiple runners.
     */
    PanelRunner_MultiState runnerPanel;

    CompareWarmStartToColdBase() {
        // Vis makeNewWorld.
        runnerPanel = new PanelRunner_MultiState();
        runnerPanel.activateTab();
        getContentPane().add(runnerPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    /**
     * Get some sample actions for use in tests. This is a successful short run found by tree search.
     * @return A successful queue of actions.
     */
    public static ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these. 19 steps.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(1, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(34, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(14, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(35, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(13, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(24, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(22, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(18, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(24, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));

        return actionQueue;
    }

    /**
     * Just simulate a runner from rest with no input commands for the given number of timesteps. This gives the
     * solvers SOME kind of warm start, even if it doesn't directly apply to other states.
     * @param timesteps Number of timesteps to 'warm-start' the solvers.
     * @return A QWOP game which has been run for a specified number of timesteps with no control inputs.
     */
    @SuppressWarnings("SameParameterValue")
    GameThreadSafe getFakedWarmStart(int timesteps) {
        GameThreadSafe game = new GameThreadSafe();
        for (int i = 0; i < timesteps; i++) {
            game.step(false, false, false, false);
        }
        return game;
    }
}
