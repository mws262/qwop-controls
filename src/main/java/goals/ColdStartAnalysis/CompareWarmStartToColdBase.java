package goals.ColdStartAnalysis;

import actions.Action;
import actions.ActionQueue;
import game.GameLoader;
import ui.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;

abstract class CompareWarmStartToColdBase extends JFrame {

    PanelRunner_MultiState runnerPanel;

    CompareWarmStartToColdBase() {
        // Vis setup.
        runnerPanel = new PanelRunner_MultiState();
        runnerPanel.activateTab();
        getContentPane().add(runnerPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(35, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(6, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(6, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(9, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(30, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(18, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(37, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(25, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(12, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(12, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(22, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(15, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(24, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(3, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        return actionQueue;
    }

    /**
     * Just simulate a runner from rest with no input commands for the given number of timesteps. This gives the
     * solvers SOME kind of warm start, even if it doesn't directly apply to other states.
     * @param timesteps Number of timesteps to 'warm-start' the solvers.
     * @return A QWOP game which has been run for a specified number of timesteps with no control inputs.
     */
    GameLoader getFakedWarmStart(int timesteps) {
        GameLoader game = new GameLoader();
        for (int i = 0; i < timesteps; i++) {
            game.stepGame(false, false, false, false);
        }
        return game;
    }
}
