package goals.perturbation_analysis;

import game.action.ActionQueue;
import game.action.perturbers.ActionPerturber_SwitchTooSoon;
import game.GameUnified;
import game.IGameInternal;
import ui.runner.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
// See how states diverge -- maybe trunk state
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

        ActionQueue actionQueue = ActionQueue.getSampleActions();
        Map<Integer, Integer> perturbationLocations = new HashMap<>();
        perturbationLocations.put(22, 1);
        ActionPerturber_SwitchTooSoon perturber = new ActionPerturber_SwitchTooSoon(perturbationLocations);
        ActionQueue actionQueuePerturbed = perturber.perturb(actionQueue);

        IGameInternal gameUnperturbed = new GameUnified();
        IGameInternal gamePerturbed = new GameUnified();

        boolean reachedFirstPerturbation = false;
        while (!actionQueue.isEmpty()) {

            boolean[] commandUnperturbed = actionQueue.pollCommand();
            boolean[] commandPerturbed = actionQueuePerturbed.pollCommand();

            gameUnperturbed.step(commandUnperturbed);
            gamePerturbed.step(commandPerturbed);

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
}
