package game;

import actions.Action;
import actions.ActionQueue;
import data.TFRecordDataParsers;
import org.junit.Assert;
import org.junit.Test;
import org.tensorflow.example.SequenceExample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class GameUnifiedTest {

    @Test
    public void makeNewWorld() {
        float[] initState = GameUnified.getInitialState().flattenState();

        GameUnified game = new GameUnified();
        float[] currState = game.getCurrentState().flattenState();
        for (int i = 0; i < initState.length; i++) {
            Assert.assertEquals(initState[i], currState[i], 1e-12);
        }
        game.step(true, false, true, false);
        currState = game.getCurrentState().flattenState();
        Assert.assertEquals(1, game.getTimestepsSimulatedThisGame());

        float stateDiff = 0;
        for (int i = 0; i < initState.length; i++) {
            stateDiff += Math.abs(initState[i] - currState[i]);
        }
        Assert.assertTrue(stateDiff > 1e-10);

        game.holdKeysForTimesteps(100, true, false, false, true);

        Assert.assertTrue(game.getFailureStatus());

        game.makeNewWorld();
        Assert.assertEquals(0, game.getTimestepsSimulatedThisGame());
        Assert.assertFalse(game.getFailureStatus());
        currState = game.getCurrentState().flattenState(); // Should be back to the initial state now.
        for (int i = 0; i < initState.length; i++) {
            Assert.assertEquals(initState[i], currState[i], 1e-12);
        }
    }

    @Test
    public void stepGame() {
        // Hard to test against "ground truth." Mostly going to make sure it's error free and that there aren't any
        // huge logical problems.
        GameUnified game = new GameUnified();
        float bodyTh = game.getCurrentState().body.getTh();
        Assert.assertEquals(0, game.getTimestepsSimulatedThisGame());

        game.step(true, true, false, false);
        float bodyThNext = game.getCurrentState().body.getTh();
        Assert.assertNotEquals(bodyTh, bodyThNext, 0.0); // States should change after step().
        bodyTh = bodyThNext;
        Assert.assertEquals(1, game.getTimestepsSimulatedThisGame()); // Counter should have advanced.

        game.step(true, false, true, false);
        bodyThNext = game.getCurrentState().body.getTh();
        Assert.assertNotEquals(bodyTh, bodyThNext, 0.0);
        Assert.assertEquals(2, game.getTimestepsSimulatedThisGame());
    }

    @Test
    public void stepGame1() {
        GameUnified game1 = new GameUnified();
        GameUnified game2 = new GameUnified();

        game1.step(true, false, true, false);
        game2.step(new boolean[]{true, false, true, false});

        State gameState1 = game1.getCurrentState();
        State gameState2 = game2.getCurrentState();

        Assert.assertEquals(gameState1.body.getX(), gameState2.body.getX(), 1e-12);
        Assert.assertEquals(gameState1.rthigh.getTh(), gameState2.rthigh.getTh(), 1e-12);
        Assert.assertEquals(gameState1.luarm.getY(), gameState2.luarm.getY(), 1e-12);
    }

    @Test
    public void setState() {
        GameUnified game1 = new GameUnified();
        GameUnified game2 = new GameUnified();

        game1.holdKeysForTimesteps(10, true, false, true, false);

        State gameState1 = game1.getCurrentState();
        game2.setState(gameState1);
        float[] gameState2f = game2.getCurrentState().flattenState();
        float[] gameState1f = gameState1.flattenState();

        for (int i = 0; i < gameState1f.length; i++) {
            Assert.assertEquals(gameState1f[i], gameState2f[i], 1e-12);
        }
    }

    @Test
    public void getFailureStatus() {
        GameUnified game = new GameUnified();
        Assert.assertFalse(game.getFailureStatus());
        game.holdKeysForTimesteps(100, true, false, false, true);
        Assert.assertTrue(game.getFailureStatus());
    }

    @Test
    public void isRightFootDown() {
        GameUnified game = new GameUnified();
        Assert.assertFalse(game.isRightFootDown());
        game.step(false,false,false,false);
        Assert.assertTrue(game.isRightFootDown());
    }

    @Test
    public void isLeftFootDown() {
        GameUnified game = new GameUnified();
        Assert.assertFalse(game.isLeftFootDown());
        game.step(false,false,false,false);
        Assert.assertTrue(game.isLeftFootDown());
    }

    @Test
    public void getInitialState() {
        float[] initState = GameUnified.getInitialState().flattenState();

        GameUnified game = new GameUnified();
        game.step(true,true,true,true);
        float[] initStateAgain = GameUnified.getInitialState().flattenState(); // Make sure a second call gets the same
        // thing, even after the game has stepped a bit.

        for (int i = 0; i < initState.length; i++) {
            Assert.assertEquals(initState[i], initStateAgain[i], 1e-12);
        }
        Assert.assertEquals(initState.length, initStateAgain.length);
    }

    @Test
    public void holdKeysForTimesteps() {
        int numTs = 7;
        GameUnified game1 = new GameUnified();
        GameUnified game2 = new GameUnified();

        game1.holdKeysForTimesteps(numTs, false, true, false, false);
        for (int i = 0; i < numTs; i++) {
            game2.step(false, true, false, false);
        }

        Assert.assertArrayEquals(game2.getCurrentState().flattenState(), game1.getCurrentState().flattenState(),
                1e-15f);
    }

    @Test
    public void getTimestepsSimulatedThisGame() {
        GameUnified game1 = new GameUnified();
        GameUnified game2 = new GameUnified();

        Assert.assertEquals(0, game1.getTimestepsSimulatedThisGame());
        Assert.assertEquals(0, game2.getTimestepsSimulatedThisGame());

        game1.step(false, true, false, true);
        Assert.assertEquals(1, game1.getTimestepsSimulatedThisGame());
        Assert.assertEquals(0, game2.getTimestepsSimulatedThisGame());
        game2.step(true, false, false, false);
        Assert.assertEquals(1, game2.getTimestepsSimulatedThisGame());

        game2.holdKeysForTimesteps(5, true, true, true, true);

        Assert.assertEquals(6, game2.getTimestepsSimulatedThisGame());
        Assert.assertEquals(1, game1.getTimestepsSimulatedThisGame());

    }

    @Test
    public void adjustRealQWOPStateToSimState() { //TODO need to fix the actual method first.
    }

    /**
     * This test uses some saved data, with states at every timestep to make sure that the simulation can still
     * reproduce it when fed the same commands. This alerts us if any changes have altered the behavior of GameThreadSafe
     * without our knowledge.
     */
    @Test
    public void testForAccidentalChanges() {
        File exampleRunFile = new File("src/test/resources/saved_data_examples/example_run.TFRecord");
        List<SequenceExample> dataSeries = null;
        try {
            dataSeries = TFRecordDataParsers.loadSequencesFromTFRecord(exampleRunFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(1, Objects.requireNonNull(dataSeries).size()); // This example data should contain one run.
        State[] loadedStates = TFRecordDataParsers.getStatesFromLoadedSequence(dataSeries.get(0));
        List<Action> loadedActions = TFRecordDataParsers.getActionsFromLoadedSequence(dataSeries.get(0));

        GameUnified game = new GameUnified();
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addSequence(loadedActions);

        int count = 0;
        while (!actionQueue.isEmpty()) {
            float[] stSim = game.getCurrentState().flattenState();
            float[] stLoad = loadedStates[count].flattenState();

            for (int i = 0; i < stSim.length; i++) {
                Assert.assertEquals(stLoad[i], stSim[i], 1e-10);
            }

            boolean[] command = actionQueue.pollCommand();
            game.step(command);
            count++;
        }
    }

    @Test
    public void getToSameEndAfterReload() {
        GameUnified gameSingle = new GameUnified();

        // Run through the full queue with no saving/loading
        ActionQueue actions = getSampleActions();
        while (!actions.isEmpty()) {
            gameSingle.step(actions.pollCommand());
        }
        State stateEndNoLoad = gameSingle.getCurrentState();

        // Redo with save/load in the middle.
        gameSingle.makeNewWorld();
        actions = getSampleActions();
        for (int i = 0; i < 30; i++) {
            gameSingle.step(actions.pollCommand());
        }

        State stateBeforeLoad = gameSingle.getCurrentState();

        // Save
        byte[] fullState = gameSingle.getFullState();

        // Step forward arbitrarily.
        gameSingle.holdKeysForTimesteps(10, true, false, false, true);

        // Load
        gameSingle = gameSingle.restoreFullState(fullState);
        State stateAfterLoad = gameSingle.getCurrentState();

        // Make sure states at save and after load are equal.
        Assert.assertArrayEquals(stateBeforeLoad.flattenState(), stateAfterLoad.flattenState(), 1e-15f);

        // Finish the queue on the loaded game.
        while (!actions.isEmpty()) {
            gameSingle.step(actions.pollCommand());
        }

        State stateEndAfterLoad = gameSingle.getCurrentState();

        Assert.assertArrayEquals(stateEndNoLoad.flattenState(), stateEndAfterLoad.flattenState(), 1e-15f);

    }

    @Test
    public void forkingGameAndContinuingToTheSameEnd() {
        // Make sure that a single game can create a restored copy such that both are consistent with each other, but
        // don't affect each other's results.
        GameUnified game = new GameUnified();

        game.holdKeysForTimesteps(10, false, true, true, false);

        State stateAtSave = game.getCurrentState();
        byte[] gameSave = game.getFullState();

        game.holdKeysForTimesteps(10, true, false, false, true);
        State stateAfter10 = game.getCurrentState();

        GameUnified gameRestored = game.restoreFullState(gameSave);
        State stateAtRestore = gameRestored.getCurrentState();
        gameRestored.holdKeysForTimesteps(10, true, false, false, true);

        State stateReloadAfter10 = gameRestored.getCurrentState();

        Assert.assertArrayEquals(stateAtSave.flattenState(), stateAtRestore.flattenState(), 1e-15f);
        Assert.assertArrayEquals(stateAfter10.flattenState(), stateReloadAfter10.flattenState(), 1e-15f);

        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
            gameRestored.step(false, true, true, false);
        }

        Assert.assertArrayEquals(game.getCurrentState().flattenState(), gameRestored.getCurrentState().flattenState()
                , 1e-15f);
    }

    @Test
    public void branchingGameLoad() {
        // Also make sure that a bunch of things loading from the SAME thing are ok.
        GameUnified game = new GameUnified();
        game.holdKeysForTimesteps(10, false, true, true, false);

        byte[] gameSave = game.getFullState();

        Callable<State> sim = () -> {
            GameUnified gameForLoading = game.restoreFullState(gameSave);
            gameForLoading.holdKeysForTimesteps(10, false, true, false, true);
            return gameForLoading.getCurrentState();
        };

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Callable<State>> sims = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sims.add(sim);
        }

        List<Future<State>> results = null;
        try {
            results = executorService.invokeAll(sims);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        float[] stateComparison = null;
        for (Future<State> f : Objects.requireNonNull(results)) {
            try {
                State s = f.get();
                if (stateComparison == null) {
                    stateComparison = s.flattenState();
                } else {
                    Assert.assertArrayEquals(stateComparison, s.flattenState(), 1e-15f);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void simultaneousGameLoad() {

        // Make sure that a bunch of things loading and saving at the same time are ok.
        Callable<State> sim = () -> {
            GameUnified game = new GameUnified();
            game.holdKeysForTimesteps(10, false, true, true, false);

            byte[] gameSave = game.getFullState();

            game.holdKeysForTimesteps(10, true, false, false, false);

            GameUnified gameLoaded = game.restoreFullState(gameSave);

            gameLoaded.holdKeysForTimesteps(10, true, false, false, false);

            State s = game.getCurrentState();
            float[] s1 = s.flattenState();
            float[] s2 = gameLoaded.getCurrentState().flattenState();
            Assert.assertArrayEquals(s1, s2, 1e-15f);

            return s;
        };

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Callable<State>> sims = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sims.add(sim);
        }

        List<Future<State>> results = null;
        try {
            results = executorService.invokeAll(sims);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        float[] stateComparison = null;
        for (Future<State> f : Objects.requireNonNull(results)) {
            try {
                State s = f.get();
                if (stateComparison == null) {
                    stateComparison = s.flattenState();
                } else {
                    Assert.assertArrayEquals(stateComparison, s.flattenState(), 1e-15f);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
    private static ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these. 19 steps.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(1, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(34, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(14, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(35, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(13, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(24, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(22, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(18, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(24, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));

        return actionQueue;
    }
}
