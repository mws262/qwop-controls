package goals.cold_start_analysis;

import game.actions.ActionQueue;
import game.GameUnified;
import game.IGameInternal;
import game.state.IState;

/**
 * This version runs a normal sequence of game.actions, but adds another cold-started runner at every timestep, and seeing
 * the distance travelled before failure for each.
 * @author matt
 */
public class MAIN_CompareWarmStartToColdEveryStep extends CompareWarmStartToColdBase {
    // 14 steps in the sample run. -> about 17 meters per step.
    // About 2.2 steps-worth of distance on average before failure after a cold start.

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdEveryStep().run();
    }

    private void run() {
        ActionQueue actionQueue = ActionQueue.getSampleActions();
        IGameInternal gameFullRun = new GameUnified();
        IGameInternal coldStartGame = new GameUnified();

        // Start simulating the entire "good" run on the normal game.
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Next command.
            gameFullRun.step(nextCommand); // Sim the main runner and put on screen.

            coldStartGame.makeNewWorld();
            IState st = gameFullRun.getCurrentState();
            coldStartGame.setState(st);
            ActionQueue coldStartActionQueue = actionQueue.getCopyOfQueueAtExecutionPoint();

            // Simulate ahead on the cold started version until failure for each timestep of the original version.
            float initX = st.getCenterX();
            while (!coldStartActionQueue.isEmpty()) {
                nextCommand = coldStartActionQueue.pollCommand();
                coldStartGame.step(nextCommand);
                if (coldStartGame.getFailureStatus()) {
                    System.out.println((coldStartGame.getCurrentState().getCenterX() - initX)/17.);
                    break;
                }
            }
        }
    }
}
