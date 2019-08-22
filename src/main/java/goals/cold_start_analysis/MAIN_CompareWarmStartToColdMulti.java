package goals.cold_start_analysis;

import game.action.ActionQueue;
import game.GameUnified;
import game.IGameInternal;
import game.action.CommandQWOP;
import game.state.IState;
import tree.node.NodeQWOPGraphicsBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Take a known sequence of reasonable game.action, and introduce extra runners all along the way with the same state,
 * but lacking the warm-start of the physics engine solver. Simulate these all with the same commands at each timestep.
 *
 * @author matt
 */
public class MAIN_CompareWarmStartToColdMulti extends CompareWarmStartToColdBase {

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdMulti().run();
    }

    public void run() {
        ActionQueue<CommandQWOP> actionQueue = ActionQueue.getSampleActions();

        IGameInternal<CommandQWOP> gameFullRun = new GameUnified(); // This game will run all the commands, start to
        // finish.
        List<GameUnified> coldStartGames = new ArrayList<>();

        int coldStartAction = 2; // Starts the first cold start runner at this command index.

        while (!actionQueue.isEmpty()) { // Go until all commands used.
            // Every action interval, introduce a new cold-start runner and resume simulating all of them.
            while (!actionQueue.isEmpty() && actionQueue.getCurrentActionIdx() < coldStartAction) {
                runnerPanel.clearSecondaryStates(); // Clear all the visualized runners.

                CommandQWOP nextCommand = actionQueue.pollCommand(); // Next command.
                gameFullRun.step(nextCommand); // Sim the main runner and put on screen.
                runnerPanel.setMainState(gameFullRun.getCurrentState());

                // Simulate the additional cold-started runners.
                int count = 0;
                Iterator<GameUnified> coldStartGameIter = coldStartGames.iterator();
                while (coldStartGameIter.hasNext()) {
                    GameUnified gm = coldStartGameIter.next();
                    gm.step(nextCommand);
                    IState st = gm.getCurrentState();
                    if (st.isFailed()) {
                        coldStartGameIter.remove();
                    } else {
                        runnerPanel.addSecondaryState(st, NodeQWOPGraphicsBase.getColorFromScaledValue(count,
                                actionQueue.getActionsInCurrentRun().length / (float) 5, 0.8f));
                        count++;
                    }
                }
                repaint();
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Add a new cold started runner at the full game's current state.
            GameUnified coldStartGame = getFakedWarmStart(0);
            coldStartGame.setState(gameFullRun.getCurrentState());
            coldStartGames.add(coldStartGame);

            // Every how many game.action we introduce a new cold-started runner to simulate and visualize.
            int coldStartRunnerIntroductionInterval = 1;
            coldStartAction += coldStartRunnerIntroductionInterval;
        }
    }
}
