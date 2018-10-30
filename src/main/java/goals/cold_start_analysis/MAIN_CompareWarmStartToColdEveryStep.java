package goals.cold_start_analysis;

import actions.ActionQueue;
import game.GameLoader;
import game.State;

/**
 * This version runs a normal sequence of actions, but adds another cold-started runner at every timestep, and seeing
 * the distance travelled before failure for each.
 * @author matt
 */
public class MAIN_CompareWarmStartToColdEveryStep extends CompareWarmStartToColdBase {
    // 19 steps in the sample run. -> about 14 meters per step.
    // About 1.75 steps on average before failure after a cold start.

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdEveryStep().run();
    }

    private void run() {
        ActionQueue actionQueue = getSampleActions();
        GameLoader gameFullRun = new GameLoader();
        GameLoader coldStartGame = new GameLoader();

        // Start simulating the entire "good" run on the normal game.
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Next command.
            gameFullRun.stepGame(nextCommand); // Sim the main runner and put on screen.

            coldStartGame.makeNewWorld();
            State st = gameFullRun.getCurrentState();
            coldStartGame.setState(st);
            ActionQueue coldStartActionQueue = actionQueue.getCopyOfQueueAtExecutionPoint();

            // Simulate ahead on the cold started version until failure for each timestep of the original version.
            int counter = 0;
            float initX = st.body.getX();
            while (!coldStartActionQueue.isEmpty()) {
                nextCommand = coldStartActionQueue.pollCommand();
                coldStartGame.stepGame(nextCommand);
                counter++;
                if (coldStartGame.getFailureStatus()) {
                    System.out.println((coldStartGame.getCurrentState().body.getX() - initX)/14.);
                    break;
                }
            }
        }
        System.out.println((gameFullRun.getCurrentState().body.getX() - GameLoader.getInitialState().body.getX())/19.);
    }
}
