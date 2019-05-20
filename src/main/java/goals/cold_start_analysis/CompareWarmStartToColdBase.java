package goals.cold_start_analysis;

import game.GameUnified;
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
     * Just simulate a runner from rest with no input commands for the given number of timesteps. This gives the
     * solvers SOME kind of warm start, even if it doesn't directly apply to other states.
     * @param timesteps Number of timesteps to 'warm-start' the solvers.
     * @return A QWOP game which has been run for a specified number of timesteps with no control inputs.
     */
    @SuppressWarnings("SameParameterValue")
    GameUnified getFakedWarmStart(int timesteps) {
        GameUnified game = new GameUnified();
        for (int i = 0; i < timesteps; i++) {
            game.step(false, false, false, false);
        }
        return game;
    }
}
