package goals;

import actions.Action;
import actions.ActionQueue;
import game.GameLoader;
import game.State;

public class MAIN_CompareWarmStartToCold {

    public static void main(String[] args) {
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

        int coldStartAction = 12;

        while (actionQueue.getCurrentActionIdx() < 12) {
            gameFullRun.stepGame(actionQueue.pollCommand());
        }
        State coldStartState = gameFullRun.getCurrentState();

        gameColdStart.setState(coldStartState);

        float[] assignedState = gameColdStart.getCurrentState().flattenState();
        float[] originalState = gameFullRun.getCurrentState().flattenState();

        // Check.
        for (int i = 0; i < assignedState.length; i++) {
            System.out.println( assignedState[i] == originalState[i]);
        }

    }
}
