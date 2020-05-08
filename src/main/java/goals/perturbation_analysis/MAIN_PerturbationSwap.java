package goals.perturbation_analysis;

import game.IGameInternal;
import game.action.ActionQueue;
import game.action.perturbers.ActionPerturber_SwapCommandAtTimestep;
import game.qwop.*;
import ui.runner.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// See how states diverge -- maybe trunk state
@SuppressWarnings("Duplicates")
public class MAIN_PerturbationSwap extends JFrame {

    public static void main(String[] args) {
        new MAIN_PerturbationSwap().run();
    }

    boolean graphics = false;
    public void run() {
        PanelRunner_MultiState runnerPanel = null;
        if (graphics) {
            // Graphics panel setup.
            runnerPanel = new PanelRunner_MultiState("Runners");
            runnerPanel.activateTab();
            getContentPane().add(runnerPanel);
            setPreferredSize(new Dimension(1000, 800));
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pack();
            setVisible(true);
        }

        // Use a known sample sequence.
        ActionQueue<CommandQWOP> actionQueue = ActionQueuesQWOP.makeShortQueue();

        // Make perturbers for each command type.
        for (int timestep = 100; timestep < actionQueue.getTotalQueueLengthTimesteps() - 100; timestep += 1) {
            System.out.println("working on ts: " + timestep);
            actionQueue.resetQueue();
            ActionPerturber_SwapCommandAtTimestep<CommandQWOP>
                    qPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.Q),
                    wPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.W),
                    oPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.O),
                    pPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.P),
                    qoPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.QO),
                    qpPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.QP),
                    woPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.WO),
                    wpPerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.WP),
                    nonePerturb = new ActionPerturber_SwapCommandAtTimestep<>(timestep, CommandQWOP.NONE);

            List<ActionPerturber_SwapCommandAtTimestep<CommandQWOP>> perturbers = new ArrayList<>();
            perturbers.add(qPerturb);
            perturbers.add(wPerturb);
            perturbers.add(oPerturb);
            perturbers.add(pPerturb);
            perturbers.add(qoPerturb);
            perturbers.add(qpPerturb);
            perturbers.add(woPerturb);
            perturbers.add(wpPerturb);
            perturbers.add(nonePerturb);

            IGameInternal<CommandQWOP, StateQWOP> gameUnperturbed = new GameQWOP();

            List<IGameInternal<CommandQWOP, StateQWOP>> perturbedGames = new ArrayList<>();
            List<ActionQueue<CommandQWOP>> perturbedQueues = new ArrayList<>();
            for (int i = 0; i < perturbers.size(); i++) {
                perturbedGames.add(new GameQWOP());
                perturbedQueues.add(perturbers.get(i).perturb(actionQueue));
            }

            try (FileWriter stateWriter = new FileWriter("./tmp/tmpStateDump" + timestep + ".txt")) {
                while (!actionQueue.isEmpty()) {

                    // Step game without perturbations and update graphics.
                    CommandQWOP commandUnperturbed = actionQueue.pollCommand();
                    gameUnperturbed.step(commandUnperturbed);
                    if (graphics)
                        runnerPanel.setMainState(gameUnperturbed.getCurrentState());

                    // Step all the perturbed games and update their graphics.
                    boolean allFailed = true;
                    if (graphics)
                        runnerPanel.clearSecondaryStates();

                    stateWriter.write((gameUnperturbed.getTimestepsThisGame()) * QWOPConstants.timestep + " ");
                    stateWriter.write((gameUnperturbed.isFailed() ? "NaN" :
                            gameUnperturbed.getCurrentState().body.getTh()) + " ");

                    for (int i = 0; i < perturbers.size(); i++) {
                        IGameInternal<CommandQWOP, StateQWOP> g = perturbedGames.get(i);
                        g.step(perturbedQueues.get(i).pollCommand());
                        StateQWOP st = g.getCurrentState();

                        if (!g.isFailed()) {
                            allFailed = false;
                        }

                        if (graphics)
                            runnerPanel.addSecondaryState(st, Color.RED);
                        stateWriter.write((g.isFailed() ? "NaN" : st.body.getTh()) + " ");
                    }
                    stateWriter.write('\n');
                    if (graphics)
                        repaint();

                    if (allFailed) {
                        break;
                    }

                    if (graphics) {
                        try {
                            if (gameUnperturbed.getTimestepsThisGame() > timestep) {
                                Thread.sleep(10);
                            } else {
                                Thread.sleep(5);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
