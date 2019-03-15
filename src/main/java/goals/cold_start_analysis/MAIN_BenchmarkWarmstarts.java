package goals.cold_start_analysis;

import actions.ActionQueue;
import game.GameSingleThread;
import game.GameThreadSafe;
import game.GameThreadSafeSavable;
import tree.Utility;


/**
 * Trying to see the fastest/best ways to get back to a certain state. Not a great way of doing profiling, but just a
 * quick general idea.
 */
@SuppressWarnings("Duplicates")
public class MAIN_BenchmarkWarmstarts {



    public static void main(String[] args) {

        GameSingleThread gameSingle = GameSingleThread.getInstance();
        GameThreadSafe gameMulti = new GameThreadSafe();


        int singleTestSamples = 100;
        gameSingle.step(false, true, true, false);
        gameMulti.step(false, true, true, false);

        byte[] singleSave = null;
        Utility.tic();
        for (int i = 0; i < singleTestSamples; i++) {
            singleSave = gameSingle.getFullState();
        }
        long timeElapsed = Utility.toc();
        System.out.println("Single save only: " + timeElapsed / (float) singleTestSamples / 1e6f + " ms");


        GameThreadSafeSavable multiSave = null;
        Utility.tic();
        for (int i = 0; i < singleTestSamples; i++) {
            multiSave = GameThreadSafeSavable.getFullState(gameMulti);
        }
        timeElapsed = Utility.toc();
        System.out.println("Multi save only: " + timeElapsed / (float) singleTestSamples / 1e6f + " ms");

        Utility.tic();
        for (int i = 0; i < singleTestSamples; i++) {
            gameSingle = gameSingle.restoreFullState(singleSave);
        }
        timeElapsed = Utility.toc();
        System.out.println("Single load only: " + timeElapsed / (float) singleTestSamples / 1e6f + " ms");


        Utility.tic();
        for (int i = 0; i < singleTestSamples; i++) {
            gameMulti = GameThreadSafeSavable.getRestoredCopy(multiSave);
        }
        timeElapsed = Utility.toc();
        System.out.println("Multi load only: " + timeElapsed / (float) singleTestSamples / 1e6f + " ms");

        // More integrated benchmarking.
        ActionQueue actionsBase = CompareWarmStartToColdBase.getSampleActions();
        //actionsBase.addSequence(actionsBase.getCopyOfUnexecutedQueue().getActionsInCurrentRun());
       // actionsBase.addSequence(actionsBase.getCopyOfUnexecutedQueue().getActionsInCurrentRun());

        System.out.println(actionsBase.getTotalQueueLengthTimesteps()/25f + " second sequence used here on.");

        int samples = 100;
        // Baseline: simulate to the end each time from the beginning.
        Utility.tic();
        for (int i = 0; i < samples; i++) {
            gameSingle.makeNewWorld();
            ActionQueue actions = actionsBase.getCopyOfUnexecutedQueue();
            while (!actions.isEmpty()) {
                gameSingle.step(actions.pollCommand());
            }
            gameSingle.step(true, false, false, true);
        }
        timeElapsed = Utility.toc();

        System.out.println("Baseline single: " + timeElapsed / (float) samples / 1e6f + " ms");


        // Baseline: simulate to the end each time from the beginning.
        Utility.tic();
        for (int i = 0; i < samples; i++) {
            gameMulti.makeNewWorld();
            ActionQueue actions = actionsBase.getCopyOfUnexecutedQueue();
            while (!actions.isEmpty()) {
                gameMulti.step(actions.pollCommand());
            }
            gameMulti.step(true, false, false, true);
        }
        timeElapsed = Utility.toc();

        System.out.println("Baseline multi: " + timeElapsed / (float) samples / 1e6f + " ms");


        // Single thread game, load at end. Save only once.
        gameSingle.makeNewWorld();
        ActionQueue actions = actionsBase.getCopyOfUnexecutedQueue();
        while (!actions.isEmpty()) {
            gameSingle.step(actions.pollCommand());
        }
        byte[] fullStateSingle = gameSingle.getFullState();

        Utility.tic();
        for (int i = 0; i < samples; i++) {
            gameSingle = gameSingle.restoreFullState(fullStateSingle);
            gameMulti.step(true, false, false, true);
        }
        timeElapsed = Utility.toc();

        System.out.println("Single load, save once: " + timeElapsed / (float) samples / 1e6f + " ms");


        // Single thread game, load at end. Save every time.
        gameSingle.makeNewWorld();
        actions = actionsBase.getCopyOfUnexecutedQueue();
        while (!actions.isEmpty()) {
            gameSingle.step(actions.pollCommand());
        }
        Utility.tic();
        for (int i = 0; i < samples; i++) {
            fullStateSingle = gameSingle.getFullState();
            gameSingle = gameSingle.restoreFullState(fullStateSingle);
            gameMulti.step(true, false, false, true);
        }
        timeElapsed = Utility.toc();

        System.out.println("Single load, save every time: " + timeElapsed / (float) samples / 1e6f + " ms");


        // Multi thread game, load at end. Save only once.
        gameMulti.makeNewWorld();
        actions = actionsBase.getCopyOfUnexecutedQueue();
        while (!actions.isEmpty()) {
            gameMulti.step(actions.pollCommand());
        }
        GameThreadSafeSavable fullStateMulti = GameThreadSafeSavable.getFullState(gameMulti);

        Utility.tic();
        for (int i = 0; i < samples; i++) {
            gameMulti = GameThreadSafeSavable.getRestoredCopy(fullStateMulti);
            gameMulti.step(true, false, false, true);
        }
        timeElapsed = Utility.toc();

        System.out.println("Multi load, save once: " + timeElapsed / (float) samples / 1e6f + " ms");


        // Multi thread game, load at end. Save every time.
        gameMulti.makeNewWorld();
        actions = actionsBase.getCopyOfUnexecutedQueue();
        while (!actions.isEmpty()) {
            gameMulti.step(actions.pollCommand());
        }
        Utility.tic();
        for (int i = 0; i < samples; i++) {
            fullStateMulti = GameThreadSafeSavable.getFullState(gameMulti);
            gameMulti = GameThreadSafeSavable.getRestoredCopy(fullStateMulti);
            gameMulti.step(true, false, false, true);
        }
        timeElapsed = Utility.toc();

        System.out.println("Multi load, save every time: " + timeElapsed / (float) samples / 1e6f + " ms");


        gameSingle.releaseGame();
    }



}
