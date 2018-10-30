package goals.cold_start_analysis;

import actions.ActionQueue;
import game.GameLoader;
import game.State;

import java.awt.*;

/**
 * Take a known sequence of reasonable actions, simulate for some number of actions, and introduce a second runner,
 * with a cloned state, but a cold start of the Box2D internal solvers. Simulate both together for the rest of the
 * run with identical input commands.
 *
 * @author matt
 */
public class MAIN_CompareWarmStartToColdSingle extends CompareWarmStartToColdBase {

    /**
     * Decide at which action to introduce a cold-started runner.
     */
    private int coldStartAction = 11;

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdSingle().run();
    }
    public void run() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue actionQueue = getSampleActions();

        GameLoader gameFullRun = new GameLoader(); // This game will run all the commands, start to finish.
        GameLoader gameColdStart = new GameLoader(); // This will start at some point in the middle of the sequence,
        // with a cloned state from gameFullRun, but a cold start on all the internal solvers.

        // Get to a certain part of the run where we want to introduce another cold start runner.
        while (actionQueue.getCurrentActionIdx() < coldStartAction) {
            gameFullRun.stepGame(actionQueue.pollCommand());
        }
        State coldStartState = gameFullRun.getCurrentState();
        gameColdStart.setState(coldStartState);

        runnerPanel.setMainState(gameFullRun.getCurrentState());
        runnerPanel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);
        repaint();
        try { // Pause for a moment so the user can see that the initial positions of the runners match.
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Simulate the rest of the run with both runners.
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand();

            gameFullRun.stepGame(nextCommand);
            gameColdStart.stepGame(nextCommand);

            runnerPanel.clearSecondaryStates();
            runnerPanel.setMainState(gameFullRun.getCurrentState());
            runnerPanel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);

            repaint();
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
