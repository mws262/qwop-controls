package goals.ColdStartAnalysis;

import actions.ActionQueue;
import game.GameLoader;

public class MAIN_CompareWarmStartToColdEveryStep extends CompareWarmStartToColdBase {

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
            coldStartGame.setState(gameFullRun.getCurrentState());
            ActionQueue coldStartActionQueue = actionQueue.getCopyOfQueueAtExecutionPoint();

            // Simulate ahead on the cold started version until failure for each timestep of the original version.
            int counter = 0;
            while (!coldStartActionQueue.isEmpty()) {
                nextCommand = coldStartActionQueue.pollCommand();
                coldStartGame.stepGame(nextCommand);
                counter++;
                if (coldStartGame.getFailureStatus()) {
                    break;
                }
            }
            System.out.println(counter);
        }
    }
}
