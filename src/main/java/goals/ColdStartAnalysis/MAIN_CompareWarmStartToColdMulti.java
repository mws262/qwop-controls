package goals.ColdStartAnalysis;

import actions.Action;
import actions.ActionQueue;
import game.GameLoader;
import tree.Node;
import ui.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Take a known sequence of reasonable actions, and introduce extra runners all along the way with the same state,
 * but lacking the warm-start of the physics engine solver. Simulate these all with the same commands at each timestep.
 * @author matt
 */
public class MAIN_CompareWarmStartToColdMulti extends JFrame {

    /**
     * Every how many actions we introduce a new cold-started runner to simulate and visualize.
     */
    private int coldStartRunnerIntroductionInterval = 4;

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdMulti().run();
    }
    public void run() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(35, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(6, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(6, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(9, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(30, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(18, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(37, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(25, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(12, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(12, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(22, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(15, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(24, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(3, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));

        // Vis setup.
        PanelRunner_MultiState panel = new PanelRunner_MultiState();
        panel.activateTab();
        getContentPane().add(panel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        GameLoader gameFullRun = new GameLoader(); // This game will run all the commands, start to finish.
        List<GameLoader> coldStartGames = new ArrayList<>();
        int coldStartAction = 2; // Starts the first cold start runner at this command index.

        while(!actionQueue.isEmpty()){ // Go until all commands used.
            // Every action interval, introduce a new cold-start runner and resume simulating all of them.
            while (actionQueue.getCurrentActionIdx() < coldStartAction && !actionQueue.isEmpty()) {
                panel.clearSecondaryStates(); // Clear all the visualized runners.

                boolean[] nextCommand = actionQueue.pollCommand(); // Next command.
                gameFullRun.stepGame(nextCommand); // Sim the main runner and put on screen.
                panel.setMainState(gameFullRun.getCurrentState());

                // Simulate the additional cold-started runners.
                for (int i = 0; i < coldStartGames.size(); i++) {
                    coldStartGames.get(i).stepGame(nextCommand);
                        panel.addSecondaryState(coldStartGames.get(i).getCurrentState(),
                                Node.getColorFromScaledValue(i, actionQueue.getActionsInCurrentRun().length/5, 0.8f));
                }
                repaint();
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Add a new cold started runner at the full game's current state.
            GameLoader coldStartGame = new GameLoader();
            coldStartGame.setState(gameFullRun.getCurrentState());
            coldStartGames.add(coldStartGame);

            coldStartAction += coldStartRunnerIntroductionInterval;
        }
    }
}
