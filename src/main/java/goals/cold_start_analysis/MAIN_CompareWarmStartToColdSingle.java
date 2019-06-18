package goals.cold_start_analysis;

import game.actions.ActionQueue;
import game.*;
import game.state.IState;

import java.awt.*;

/**
 * Take a known sequence of reasonable game.actions, simulate for some number of game.actions, and introduce a second runner,
 * with a cloned state, but a cold start of the Box2D internal solvers. Simulate both together for the rest of the
 * run with identical input commands.
 *
 * @author matt
 */
public class MAIN_CompareWarmStartToColdSingle extends CompareWarmStartToColdBase {

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdSingle().run();
    }

    public void run() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue actionQueue = ActionQueue.getSampleActions();

        IGameInternal gameFullRun = new GameUnified(); // This game will run all the commands, start to finish.
        IGameInternal gameColdStart = new GameUnified(); // This will start at some point in the middle of the sequence,
        // with a cloned state from gameFullRun, but a cold start on all the internal solvers.

        // Get to a certain part of the run where we want to introduce another cold start runner.

        // Decide at which action to introduce a cold-started runner.
        int coldStartAction = 14;
        while (actionQueue.getCurrentActionIdx() < coldStartAction) {
            gameFullRun.step(actionQueue.pollCommand());
        }
        IState coldStartState = gameFullRun.getCurrentState();
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

            gameFullRun.step(nextCommand);
            GameConstants.physIterations = 5;
            gameColdStart.step(nextCommand);
            GameConstants.physIterations = 5;

            runnerPanel.clearSecondaryStates();
            runnerPanel.setMainState(gameFullRun.getCurrentState());
            runnerPanel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);

            repaint();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
