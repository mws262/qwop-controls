package game;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GameThreadSafeSavableTest {

    @Test
    public void singleGameLoad() {

        // Make sure that a single game can create a restored copy such that both are consistent with each other, but
        // don't affect each other's results.
        GameThreadSafe game = new GameThreadSafe();

        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
        }
        State stateAtSave = game.getCurrentState();
        GameThreadSafeSavable gameSave = GameThreadSafeSavable.getFullState(game);

        for (int i = 0; i < 10; i++) {
            game.step(true, false, false, true);
        }
        State stateAfter10 = game.getCurrentState();

        GameThreadSafe gameRestored = GameThreadSafeSavable.getRestoredCopy(gameSave);
        State stateAtRestore = gameRestored.getCurrentState();
        for (int i = 0; i < 10; i++) {
            gameRestored.step(true, false, false, true);
        }
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
    public void simultaneousGameLoad() {

        // Make sure that a bunch of things loading and saving at the same time are ok.
        Callable<State> sim = () -> {
            GameThreadSafe game = new GameThreadSafe();

            for (int i = 0; i < 10; i++) {
                game.step(false, true, true, false);
            }
            GameThreadSafeSavable gameSave = GameThreadSafeSavable.getFullState(game);

            for (int i = 0; i < 10; i++) {
                game.step(true, false, false, false);
            }

            GameThreadSafe gameLoaded = GameThreadSafeSavable.getRestoredCopy(gameSave);
            for (int i = 0; i < 10; i++) {
                gameLoaded.step(true, false, false, false);
            }
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
        for (Future<State> f : results) {
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
    public void branchingGameLoad() {
        // Also make sure that a bunch of things loading from the SAME thing are ok.
        GameThreadSafe game = new GameThreadSafe();

        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
        }
        GameThreadSafeSavable gameSave = GameThreadSafeSavable.getFullState(game);

        Callable<State> sim = () -> {
            GameThreadSafe gameForLoading = GameThreadSafeSavable.getRestoredCopy(gameSave);
            for (int i = 0; i < 10; i++) {
                gameForLoading.step(false, true, false, true);
            }
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
        for (Future<State> f : results) {
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
}