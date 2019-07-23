package value;

import game.GameConstants;
import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.state.IState;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class ValueFunctionControllerConsistencyTest {

    private final int timestepCheckpoint = 500;
    private final float[] flatStateDesired = {0.0f, -2.2574735f, -0.22032446f, 8.491266f, -0.4776165f, 0.41031206f,
            -0.82696533f, -6.168619f, -0.14093362f, 10.665741f, -1.110998f, 1.3264679f, 0.33296204f, 2.0762799f,
            -0.019159568f, 12.896204f, -0.8064831f, -2.227094f, 2.3029327f, 0.055687245f, -1.390076f, 7.7789645f, 5.206331f,
            3.0818133f, -1.5801392f, 5.1324425f, 0.98306257f, 17.834433f, -0.25546604f, 0.1305578f, 4.3138123f, 2.4191806f,
            - 0.109280996f, 6.9328294f, 12.058101f, 0.57560337f, -3.5023193f, 7.3101854f, 1.4782614f, 18.000292f, -0.6449095f,
            0.037456147f, 5.3841095f, 4.5512505f, -0.50844455f, 5.961708f, 12.751632f, 0.59944254f, -0.9659271f, -2.8314664f,
            -0.4903312f, 6.596973f, 0.4133494f, 2.2750506f, -0.20916748f, -3.191747f, 0.17921239f, 10.823969f, -0.4991777f,
            -1.8603454f, 0.9838257f, -2.8648987f, -2.339338f, 7.169518f, 4.8238482f, 2.2750506f, 0.702652f, -1.8903351f,
            1.354073f, 12.99538f, -2.464645f, -1.8603454f};

    @Test
    public void controllerConsistency_test() throws IOException {
        GameUnified game = new GameUnified();
        File modelFile = new File("src/test/resources/test_models/small_net.pb");
        Assert.assertTrue(modelFile.exists());
        ValFunSandbox valFun = new ValFunSandbox(modelFile, game);
        valFun.loadCheckpoint("src/test/resources/test_models/good_save"); // TODO change to full path.

        ActionQueue queue = new ActionQueue();

        //NodeQWOPExplorable currentNode = new NodeQWOPExplorable(game.getCurrentState());

        while (game.getTimestepsThisGame() < timestepCheckpoint) {
            if (queue.isEmpty()) {
                NodeQWOPExplorable currentNode = new NodeQWOPExplorable(game.getCurrentState());
                queue.addAction(valFun.getMaximizingAction(currentNode));
            }
            game.step(queue.pollCommand());
        }
        
        Assert.assertArrayEquals(flatStateDesired, game.getCurrentState().flattenState(), 1e-5f);

    }

    class ValFunSandbox extends ValueFunction_TensorFlow_StateOnly {

        public ValFunSandbox(File file, GameUnified gameTemplate) throws FileNotFoundException {
            super(file, gameTemplate);
            assignFuturePredictors(gameTemplate);
        }

        // Overridden because these parameters are frequently messed with outside the test.
        private void assignFuturePredictors(GameUnified gameTemplate) {
            evaluations = new ArrayList<>();
            evalResults = new ArrayList<>();
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.none, 1, 10));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.qp, 2, 40));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.wo, 2, 40));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.q, 2, 5));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.w, 2, 5));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.o, 2, 5));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.p, 2, 5));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.qo, 2, 5));
            evaluations.add(new FuturePredictorSandbox(gameTemplate, Action.Keys.wp, 2, 5));
        }

        class FuturePredictorSandbox extends FuturePredictor {

            FuturePredictorSandbox(GameUnified gameTemplate, Action.Keys keys, int minHorizon, int maxHorizon) {
                super(gameTemplate, keys, minHorizon, maxHorizon);
            }

            @Override
            public EvaluationResult call() {
                gameLocal.makeNewWorld();
                gameLocal.setState(startingState);
                gameLocal.iterations = 4 * GameConstants.physIterations;

                // Reset the game and set it to the specified starting state.
                bestResult.value = -Float.MAX_VALUE;

                // Keep track of a window of three adjacent game.actions. Some of the selection approaches do a
                // best-worst-case.
                float val1;
                float val2 = 0;
                float val3 = 0;

                for (int i = 1; i <= maxHorizon; i++) {
                    // Return to the normal number of physics iterations after the first step.
                    if (i > 2) {
                        gameLocal.iterations = GameConstants.physIterations;
                    }

                    gameLocal.step(buttons);
                    IState st = gameLocal.getCurrentState();
                    NodeQWOPBase<?> nextNode = new NodeQWOP(st);
                    val1 = val2;
                    val2 = val3;
                    val3 = evaluate(nextNode);

                    if (i == 1) {
                        val2 = val3;
                    }

                    if (i > 1 && i > minHorizon) {
                        // Decide what value should be recorded as best.
                        float aggregateVal = (val1 + val2 + val3)/3f;
                        if (aggregateVal > bestResult.value) {
                            bestResult.value = aggregateVal;
                            bestResult.timestep = i - 1;
                            bestResult.keys = keys;
                            bestResult.state = st;
                        }
                    }
                }
                return bestResult;
            }
        }
    }
}

