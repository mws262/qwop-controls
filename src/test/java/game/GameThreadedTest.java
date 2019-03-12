package game;

import actions.Action;
import com.google.common.collect.Lists;
import org.junit.*;
import tree.Utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GameThreadedTest {

    private final Action sampleAction1 = new Action(5,false, false, false, false),
            sampleAction2 = new Action(10,false, false, false, false),
            sampleAction3 = new Action(6,true, false, false, true),
            sampleAction4 = new Action(11,true, false, false, true),
            sampleAction5 = new Action(7,false, true, true, false),
            sampleAction6 = new Action(12,false, true, true, false);

    @Test
    public void singleThread() {

//        ExecutorService executorService = Executors.newFixedThreadPool(1);
//        GameThreaded game = new GameThreaded();
//
//        // Make sure that results can be replicated over multiple games.
//        float[][] allFlattenedStates = new float[3][72];
//        for (int i = 0; i < 3; i++) {
//            game.newGame();
//
//            game.addAction(sampleAction3);
//            game.addAction(sampleAction1);
//            game.addAction(sampleAction6);
//
//            Future<?> f = executorService.submit(game);
//            try {
//                f.get();
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//
//            allFlattenedStates[i] = game.getState().flattenState();
//        }
//
//        Assert.assertArrayEquals(allFlattenedStates[0], allFlattenedStates[1], 1e-15f);
//        Assert.assertArrayEquals(allFlattenedStates[1], allFlattenedStates[2], 1e-15f);
//
//        executorService.shutdownNow();
    }

    @Test
    public void multiThread() {

        int numGames = 3;
        GameThreaded[] games = new GameThreaded[numGames];
        List<GameThreaded> gamesList = new ArrayList<>();
        for (int i = 0; i < numGames; i++) {
            games[i] = new GameThreaded();
            gamesList.add(new GameThreaded());
        }

        Action[] actions = new Action[]{sampleAction3, sampleAction1, sampleAction6};
        List<Action> alist = new ArrayList<>();
        alist.add(sampleAction3);
        alist.add(sampleAction1);
        alist.add(sampleAction6);
//        ExecutorService executorService = Executors.newFixedThreadPool(numGames);
//
//        float[][] allFlattenedStates = new float[numGames][72];
//
//        for (GameThreaded game : games) {
//            game.addAction(sampleAction3);
//            game.addAction(sampleAction1);
//            game.addAction(sampleAction6);
//            executorService.submit(game);
//        }
//        executorService.shutdown();

        Iterator<Action> iterator = alist.iterator();

        gamesList.parallelStream().forEach(g->{g.addAllActions(actions);
            g.addAction(iterator.next());
            g.run();});


//        gamesList.parallelStream().forEach(g->{g.addAllActions(actions); g.run();});

//        for (int i = 0; i < numGames - 1; i++) {
//            Assert.assertArrayEquals(allFlattenedStates[i], allFlattenedStates[i + 1], 1e-15f);
//        }
//
//        // Do it again without resetting the game.
//        float[] previousState = allFlattenedStates[0];
//
//        executorService = Executors.newFixedThreadPool(numGames);
//
//        for (GameThreaded game : games) {
//            //game.newGame();
//            game.addAction(sampleAction3);
//            game.addAction(sampleAction1);
//            game.addAction(sampleAction6);
//            executorService.submit(game);
//        }
//        executorService.shutdown();
//
////        for (int i = 0; i < numGames - 1; i++) {
////            Assert.assertArrayEquals(allFlattenedStates[i], allFlattenedStates[i + 1], 1e-15f);
////        }
////        for (int i = 0; i < previousState.length; i++)
////            Assert.assertNotEquals(previousState[i], allFlattenedStates[0][i], 1e-15);
    }
}