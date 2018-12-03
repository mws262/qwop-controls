package game;

import actions.Action;
import actions.ActionQueue;
import data.TFRecordDataParsers;
import org.junit.Assert;
import org.junit.Test;
import org.tensorflow.example.SequenceExample;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GameLoaderTest {

    @Test
    public void makeNewWorld() {
        float[] initState = GameLoader.getInitialState().flattenState();

        GameLoader game = new GameLoader();
        float[] currState = game.getCurrentState().flattenState();
        for (int i = 0; i < initState.length; i++) {
            Assert.assertEquals(initState[i], currState[i], 1e-12);
        }
        game.stepGame(true, false, true, false);
        currState = game.getCurrentState().flattenState();
        Assert.assertEquals(1, game.getTimestepsSimulatedThisGame());

        float stateDiff = 0;
        for (int i = 0; i < initState.length; i++) {
            stateDiff += Math.abs(initState[i] - currState[i]);
        }
        Assert.assertTrue(stateDiff > 1e-10);

        for (int i = 0; i < 100; i++) {
            game.stepGame(true, false, false, true);
        }
        Assert.assertTrue(game.getFailureStatus());

        game.makeNewWorld();
        Assert.assertEquals(0, game.getTimestepsSimulatedThisGame());
        Assert.assertFalse(game.isRightFootDown());
        Assert.assertFalse(game.isLeftFootDown());
        Assert.assertFalse(game.getFailureStatus());
        currState = game.getCurrentState().flattenState(); // Should be back to the initial state now.
        for (int i = 0; i < initState.length; i++) {
            Assert.assertEquals(initState[i], currState[i], 1e-12);
        }
    }

    @Test
    public void applyBodyImpulse() { //TODO
    }

    @Test
    public void applyBodyTorque() { // TODO
    }

    @Test
    public void stepGame() {
        // Hard to test against "ground truth." Mostly going to make sure it's error free and that there aren't any
        // huge logical problems.
        GameLoader game = new GameLoader();
        float bodyTh = game.getCurrentState().body.getTh();
        Assert.assertEquals(0, game.getTimestepsSimulatedThisGame());

        game.stepGame(true, true, false, false);
        float bodyThNext = game.getCurrentState().body.getTh();
        Assert.assertFalse(bodyTh == bodyThNext); // States should change after stepGame().
        bodyTh = bodyThNext;
        Assert.assertEquals(1, game.getTimestepsSimulatedThisGame()); // Counter should have advanced.

        game.stepGame(true, false, true, false);
        bodyThNext = game.getCurrentState().body.getTh();
        Assert.assertFalse(bodyTh == bodyThNext);
        Assert.assertEquals(2, game.getTimestepsSimulatedThisGame());
    }

    @Test
    public void stepGame1() {
        GameLoader game1 = new GameLoader();
        GameLoader game2 = new GameLoader();

        game1.stepGame(true, false, true, false);
        game2.stepGame(new boolean[]{true, false, true, false});

        State gameState1 = game1.getCurrentState();
        State gameState2 = game2.getCurrentState();

        Assert.assertEquals(gameState1.body.getX(), gameState2.body.getX(), 1e-12);
        Assert.assertEquals(gameState1.rthigh.getTh(), gameState2.rthigh.getTh(), 1e-12);
        Assert.assertEquals(gameState1.luarm.getY(), gameState2.luarm.getY(), 1e-12);
    }

    @Test
    public void setState() {
        GameLoader game1 = new GameLoader();
        GameLoader game2 = new GameLoader();

        for (int i = 0; i < 10; i++) {
            game1.stepGame(true, false, true, false);
        }
        State gameState1 = game1.getCurrentState();
        game2.setState(gameState1);
        float[] gameState2f = game2.getCurrentState().flattenState();
        float[] gameState1f = gameState1.flattenState();

        for (int i = 0; i < gameState1f.length; i++) {
            Assert.assertEquals(gameState1f[i], gameState2f[i], 1e-12);
        }
    }

    @Test
    public void getXForms() { // TODO
    }

    @Test
    public void getXForm() { //TODO
    }

    @Test
    public void getFailureStatus() {
        GameLoader game = new GameLoader();
        Assert.assertFalse(game.getFailureStatus());
        for (int i = 0; i < 100; i++) {
            game.stepGame(true, false, false, true);
        }
        Assert.assertTrue(game.getFailureStatus());
    }

    @Test
    public void isGameInitialized() {
        GameLoader game = new GameLoader();
        Assert.assertTrue(game.isGameInitialized());
    }

    @Test
    public void isRightFootDown() {
        GameLoader game = new GameLoader();
        Assert.assertFalse(game.isRightFootDown());
        game.stepGame(false,false,false,false);
        Assert.assertTrue(game.isRightFootDown());
    }

    @Test
    public void isLeftFootDown() {
        GameLoader game = new GameLoader();
        Assert.assertFalse(game.isLeftFootDown());
        game.stepGame(false,false,false,false);
        Assert.assertTrue(game.isLeftFootDown());
    }

    @Test
    public void getInitialState() {
        float[] initState = GameLoader.getInitialState().flattenState();

        GameLoader game = new GameLoader();
        game.stepGame(true,true,true,true);
        float[] initStateAgain = game.getInitialState().flattenState(); // Make sure a second call gets the same
        // thing, even after the game has stepped a bit.

        for (int i = 0; i < initState.length; i++) {
            Assert.assertEquals(initState[i], initStateAgain[i], 1e-12);
        }
        Assert.assertEquals(initState.length, initStateAgain.length);
    }

    @Test
    public void getTimestepsSimulatedThisGame() {
        GameLoader game1 = new GameLoader();
        GameLoader game2 = new GameLoader();

        Assert.assertEquals(0, game1.getTimestepsSimulatedThisGame());
        Assert.assertEquals(0, game2.getTimestepsSimulatedThisGame());

        game1.stepGame(false, true, false, true);
        Assert.assertEquals(1, game1.getTimestepsSimulatedThisGame());
        Assert.assertEquals(0, game2.getTimestepsSimulatedThisGame());
        game2.stepGame(true, false, false, false);
        Assert.assertEquals(1, game2.getTimestepsSimulatedThisGame());

        for (int i = 0; i < 5; i++) {
            game2.stepGame(true, true, true, true);
        }
        Assert.assertEquals(6, game2.getTimestepsSimulatedThisGame());
        Assert.assertEquals(1, game1.getTimestepsSimulatedThisGame());

    }

    @Test
    public void adjustRealQWOPStateToSimState() { //TODO need to fix the actual method first.
    }

    /**
     * This test uses some saved data, with states at every timestep to make sure that the simulation can still
     * reproduce it when fed the same commands. This alerts us if any changes have altered the behavior of GameLoader
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

        Assert.assertEquals(1, dataSeries.size()); // This example data should contain one run.
        State[] loadedStates = TFRecordDataParsers.getStatesFromLoadedSequence(dataSeries.get(0));
        List<Action> loadedActions = TFRecordDataParsers.getActionsFromLoadedSequence(dataSeries.get(0));

        GameLoader game = new GameLoader();
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
            game.stepGame(command);
            count++;
        }
    }
}