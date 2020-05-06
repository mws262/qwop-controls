package goals.perturbation_analysis;

import game.IGameInternal;
import game.action.ActionQueue;
import game.action.perturbers.ActionPerturber_SwapCommandAtTimestep;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import ui.runner.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// See how states diverge -- maybe trunk state
@SuppressWarnings("Duplicates")
public class MAIN_PerturbationSwap extends JFrame {

    public static void main(String[] args) {
        new MAIN_PerturbationSwap().run();
    }

    public void run() {
        // Graphics panel setup.
        PanelRunner_MultiState runnerPanel = new PanelRunner_MultiState("Runners");
        runnerPanel.activateTab();
        getContentPane().add(runnerPanel);
        setPreferredSize(new Dimension(1000, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        // Use a known sample sequence.
        ActionQueue<CommandQWOP> actionQueue = ActionQueue.getSampleActions();

        // Make perturbers for each command type.
        int timestep = 6;
        ActionPerturber_SwapCommandAtTimestep<CommandQWOP>
                qPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.Q),
                wPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.W),
                oPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.O),
                pPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.P),
                qoPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.QO),
                qpPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.QP),
                woPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.WO),
                wpPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.WP);

        List<ActionPerturber_SwapCommandAtTimestep<CommandQWOP>> perturbers = new ArrayList<>();
        perturbers.add(qPerturb);
        perturbers.add(wPerturb);
        perturbers.add(oPerturb);
        perturbers.add(pPerturb);
        perturbers.add(qoPerturb);
        perturbers.add(qpPerturb);
        perturbers.add(woPerturb);
        perturbers.add(wpPerturb);

        IGameInternal<CommandQWOP, StateQWOP> gameUnperturbed = new GameQWOP();

        List<IGameInternal<CommandQWOP, StateQWOP>> perturbedGames = new ArrayList<>();
        List<ActionQueue<CommandQWOP>> perturbedQueues = new ArrayList<>();
        for (int i = 0; i < perturbers.size(); i++) {
            perturbedGames.add(new GameQWOP());
            perturbedQueues.add(perturbers.get(i).perturb(actionQueue));
        }

        while (!actionQueue.isEmpty()) {

            // Step game without perturbations and update graphics.
            CommandQWOP commandUnperturbed = actionQueue.pollCommand();
            gameUnperturbed.step(commandUnperturbed);
            runnerPanel.setMainState(gameUnperturbed.getCurrentState());

            // Step all the perturbed games and update their graphics.
            runnerPanel.clearSecondaryStates();
            for (int i = 0; i < perturbers.size(); i++) {
                perturbedGames.get(i).step(perturbedQueues.get(i).pollCommand());
                runnerPanel.addSecondaryState(perturbedGames.get(i).getCurrentState(), Color.RED);
            }

            repaint();

            try {
                if (gameUnperturbed.getTimestepsThisGame() > timestep) {
                    Thread.sleep(60);
                } else {
                    Thread.sleep(30);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
