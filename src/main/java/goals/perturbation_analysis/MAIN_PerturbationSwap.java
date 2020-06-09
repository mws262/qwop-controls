package goals.perturbation_analysis;

import game.IGameInternal;
import game.action.ActionQueue;
import game.action.perturbers.ActionPerturber_SwapCommandAtTimestep;
import game.qwop.*;
import game.state.StateVariable6D;
import ui.runner.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * See how states diverge on a single timestep command swapped for another. This perturbation is applied for all
 * timesteps along a trajectory and the results saved to text. Not that it only saves one state variable at a time
 * for now.
 *
 * @author matt
 */
public class MAIN_PerturbationSwap extends JFrame {

    public static void main(String[] args) {
        new MAIN_PerturbationSwap().run();
    }

    /**
     * Display an animation of the perturbation experiment.
     */
    private final boolean graphics = false;

    /**
     * Set the body part for which the perturbation data will be saved.
     */
    private final IStateQWOP.ObjectName bodyPartToSave = IStateQWOP.ObjectName.BODY;

    /**
     * Set the coordinate (for the body) for which the perturbation data will be saved.
     */
    private final StateVariable6D.StateName stateVarToSave = StateVariable6D.StateName.TH;


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
        final ActionQueue<CommandQWOP> actionQueue = ActionQueuesQWOP.makeLongQueue();

        // Make a game to follow the nominal trajectory.
        final IGameInternal<CommandQWOP, StateQWOP> gameUnperturbed = new GameQWOP();

        // Make games to follow each of the perturbed queues.
        final List<IGameInternal<CommandQWOP, StateQWOP>> perturbedGames =
                Stream.generate(GameQWOP::new).limit(CommandQWOP.NUM_COMMANDS).collect(Collectors.toList());

        // Do perturbations on each timestep along the nominal trajectory.
        for (int timestep = 100; timestep < actionQueue.getTotalQueueLengthTimesteps() - 100; timestep += 1) {
            System.out.println("working on ts: " + timestep);

            // Reset the games and nominal action queue back to the beginning.
            actionQueue.resetQueue();
            gameUnperturbed.resetGame();
            perturbedGames.forEach(IGameInternal::resetGame);

            // Perturb the baseline queue, i.e. swap one action, for all the perturbation types.
            final int ts = timestep;
            List<ActionQueue<CommandQWOP>> perturbedQueues =
                    CommandQWOP.allCommands.stream()
                            .map(c -> new ActionPerturber_SwapCommandAtTimestep<>(ts, c)
                                    .perturb(actionQueue)).collect(Collectors.toList());

            try (FileWriter stateWriter = new FileWriter("./tmp/tmpStateDump" + timestep + ".txt")) {
                while (!actionQueue.isEmpty()) {

                    // Step game without perturbations and update graphics.
                    CommandQWOP commandUnperturbed = actionQueue.pollCommand();
                    gameUnperturbed.step(commandUnperturbed);

                    // Write the timestep number.
                    stateWriter.write((gameUnperturbed.getTimestepsThisGame()) * QWOPConstants.timestep + " ");
                    // Write the nominal state.
                    stateWriter.write((gameUnperturbed.isFailed() ? "NaN" :
                            gameUnperturbed.getCurrentState().getStateVariableFromName(bodyPartToSave).getStateByName(stateVarToSave)) + " ");

                    // Step all the perturbed games and update their graphics.
                    if (graphics) {
                        runnerPanel.setMainState(gameUnperturbed.getCurrentState());
                        runnerPanel.clearSecondaryStates();
                    }

                    boolean allFailed = true; // Only simulate perturbations as long as at least one is still unfallen.


                    for (int i = 0; i < perturbedQueues.size(); i++) {

                        IGameInternal<CommandQWOP, StateQWOP> g = perturbedGames.get(i);
                        g.step(perturbedQueues.get(i).pollCommand());
                        StateQWOP st = g.getCurrentState();

                        stateWriter.write((g.isFailed() ? "NaN" :
                                st.getStateVariableFromName(bodyPartToSave).getStateByName(stateVarToSave)) + " ");

                        if (!g.isFailed())
                            allFailed = false;

                        if (graphics)
                            runnerPanel.addSecondaryState(st, Color.RED);
                    }

                    stateWriter.write('\n');

                    if (graphics)
                        repaint();

                    if (allFailed)
                        break;


                    // Add a graphics pause, if we're doing graphics.
                    if (graphics) {
                        try {
                            if (gameUnperturbed.getTimestepsThisGame() > timestep) {
                                Thread.sleep(20);
                            } else {
                                Thread.sleep(5); // Speed through timesteps before the perturbation.
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
