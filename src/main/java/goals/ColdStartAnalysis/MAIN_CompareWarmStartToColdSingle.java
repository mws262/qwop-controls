package goals.ColdStartAnalysis;

import actions.Action;
import actions.ActionQueue;
import game.GameLoader;
import game.State;
import ui.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;

/**
 * Take a known sequence of reasonable actions, simulate for some number of actions, and introduce a second runner,
 * with a cloned state, but a cold start of the Box2D internal solvers. Simulate both together for the rest of the
 * run with identical input commands.
 *
 * @author matt
 */
public class MAIN_CompareWarmStartToColdSingle extends JFrame {

    /**
     * Decide at which action to introduce a cold-started runner.
     */
    private int coldStartAction = 11;

    public static void main(String[] args) {
        new MAIN_CompareWarmStartToColdSingle().run();
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

        GameLoader gameFullRun = new GameLoader(); // This game will run all the commands, start to finish.
        GameLoader gameColdStart = new GameLoader(); // This will start at some point in the middle of the sequence,
        // with a cloned state from gameFullRun, but a cold start on all the internal solvers.

        // Get to a certain part of the run where we want to introduce another cold start runner.
        while (actionQueue.getCurrentActionIdx() < coldStartAction) {
            gameFullRun.stepGame(actionQueue.pollCommand());
        }
        State coldStartState = gameFullRun.getCurrentState();
        gameColdStart.setState(coldStartState);

        // Vis setup.
        PanelRunner_MultiState panel = new PanelRunner_MultiState();
        panel.activateTab();
        getContentPane().add(panel);
        setPreferredSize(new Dimension(1000, 400));
        pack();
        setVisible(true);

        panel.setMainState(gameFullRun.getCurrentState());
        panel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);
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

            panel.clearSecondaryStates();
            panel.setMainState(gameFullRun.getCurrentState());
            panel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);

            repaint();
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}