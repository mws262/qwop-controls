package goals.perturbation_analysis;

import actions.Action;
import actions.ActionQueue;
import actions.perturbers.ActionPerturber_SwitchTooSoon;
import game.GameLoader;
import ui.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MAIN_PerturbationSimple extends JFrame {

    public static void main(String[] args) {
        new MAIN_PerturbationSimple().run();
    }

    public void run() {
        PanelRunner_MultiState runnerPanel = new PanelRunner_MultiState();
        runnerPanel.activateTab();
        getContentPane().add(runnerPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);


        ActionQueue actionQueue = getSampleActions();
        Map<Integer, Integer> perturbationLocations = new HashMap<>();
        perturbationLocations.put(18, 1);
        ActionPerturber_SwitchTooSoon perturber = new ActionPerturber_SwitchTooSoon(perturbationLocations);
        ActionQueue actionQueuePerturbed = perturber.perturb(actionQueue);

        GameLoader gameUnperturbed = new GameLoader();
        GameLoader gamePerturbed = new GameLoader();

        boolean reachedFirstPerturbation = false;
        while (!actionQueue.isEmpty()) {

            boolean[] commandUnperturbed = actionQueue.pollCommand();
            boolean[] commandPerturbed = actionQueuePerturbed.pollCommand();

            gameUnperturbed.stepGame(commandUnperturbed);
            gamePerturbed.stepGame(commandPerturbed);

            runnerPanel.clearSecondaryStates();
            runnerPanel.setMainState(gameUnperturbed.getCurrentState());
            runnerPanel.addSecondaryState(gamePerturbed.getCurrentState(), Color.RED);

            repaint();

            if (!reachedFirstPerturbation && !Arrays.equals(commandPerturbed, commandUnperturbed)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reachedFirstPerturbation = true;
            }

            if (reachedFirstPerturbation) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these. 19 steps.
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
}
