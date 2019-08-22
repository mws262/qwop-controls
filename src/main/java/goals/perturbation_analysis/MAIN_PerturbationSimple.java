package goals.perturbation_analysis;

import game.action.ActionQueue;
import game.action.CommandQWOP;
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
        PanelRunner_MultiState runnerPanel = new PanelRunner_MultiState("Runners");
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

        IGameInternal<CommandQWOP> gameUnperturbed = new GameUnified();
        IGameInternal<CommandQWOP> gamePerturbed = new GameUnified();

        boolean reachedFirstPerturbation = false;
        while (!actionQueue.isEmpty()) {

            CommandQWOP commandUnperturbed = actionQueue.pollCommand();
            CommandQWOP commandPerturbed = actionQueuePerturbed.pollCommand();

            gameUnperturbed.step(commandUnperturbed);
            gamePerturbed.step(commandPerturbed);

            runnerPanel.clearSecondaryStates();
            runnerPanel.setMainState(gameUnperturbed.getCurrentState());
            runnerPanel.addSecondaryState(gamePerturbed.getCurrentState(), Color.RED);

            repaint();

            if (!reachedFirstPerturbation && !commandPerturbed.equals(commandUnperturbed)) {
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
