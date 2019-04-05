package value;

import actions.Action;
import game.GameUnified;
import game.IGame;
import game.State;
import game.StateVariable;
import tree.INode;
import tree.Node;
import tree.NodePlaceholder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;

public class ValueFunction_TensorFlow_StateOnly extends ValueFunction_TensorFlow {

    private static final int STATE_SIZE = 72;
    private static final int VALUE_SIZE = 1;

    boolean multithread = true;
    private ExecutorService ex;
    private int numThreads = 9;
    List<Callable<EvaluationResult>> evaluations = new ArrayList<>();

    public ValueFunction_TensorFlow_StateOnly(File file) throws FileNotFoundException {
        super(file);
        if (multithread) {
            ex = Executors.newFixedThreadPool(numThreads);
        }
    }

    public ValueFunction_TensorFlow_StateOnly(String fileName, List<Integer> hiddenLayerSizes,
                                              List<String> additionalArgs) throws FileNotFoundException {
        super(fileName, STATE_SIZE, VALUE_SIZE, hiddenLayerSizes, additionalArgs);
        if (multithread) {
            ex = Executors.newFixedThreadPool(numThreads);
        }
    }

    @Override
    public Action getMaximizingAction(Node currentNode) {
        State fullState = currentNode.getState();
        Objects.requireNonNull(fullState);

        List<Callable<EvaluationResult>> evaluations = new ArrayList<>();
        List<EvaluationResult> evalResults = new ArrayList<>();
        evaluations.add( // No Keys
                getCallable(fullState, currentNode, Action.Keys.none, 1, 10));
        evaluations.add( // QP
                getCallable(fullState, currentNode, Action.Keys.qp, 1, 35));
        evaluations.add( // WO
                getCallable(fullState, currentNode, Action.Keys.wo, 1, 35));
        evaluations.add( // Q
                getCallable(fullState, currentNode, Action.Keys.q, 1, 5));
        evaluations.add( // W
                getCallable(fullState, currentNode, Action.Keys.w, 1, 5));
        evaluations.add( // O
                getCallable(fullState, currentNode, Action.Keys.o, 1, 5));
        evaluations.add( // P
                getCallable(fullState, currentNode, Action.Keys.p, 1, 5));

        // Off keys -- dunno if these are ever helpful.
        evaluations.add( // QO
                getCallable(fullState, currentNode, Action.Keys.qo, 1, 5));
        evaluations.add( // WP
                getCallable(fullState, currentNode, Action.Keys.wp, 1, 5));

        if (multithread) { // Multi-thread
            List<Future<EvaluationResult>> allResults;
            try {
                allResults = ex.invokeAll(evaluations);
                for (Future<EvaluationResult> future : allResults) {
                    evalResults.add(future.get()); // Each blocks until the thread is done.
                }
            }catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else { // Single thread
            try {
                for (Callable<EvaluationResult> eval : evaluations) {
                    evalResults.add(eval.call());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getBestActionFromEvaluationResults(evalResults);
    }


    private Callable<EvaluationResult> getCallable(byte[] gameStartingState, INode startingNode,
                                                   Action.Keys keys, int minDuration, int maxDuration) {
        boolean[] buttons = Action.keysToBooleans(keys);

        return () -> {
            GameUnified gameLocal = GameUnified.restoreFullState(gameStartingState);
            EvaluationResult bestResult = new EvaluationResult();
            for (int i = minDuration; i < maxDuration; i++) {
                gameLocal.step(buttons);
                State st = gameLocal.getCurrentState();
                INode nextNode = new NodePlaceholder(startingNode, new Action(i, buttons), st);
                float val = evaluate(nextNode);
                if (val > bestResult.value) {
                    bestResult.value = val;
                    bestResult.timestep = i;
                    bestResult.keys = keys;
                }
            }
            return bestResult;
        };
    }

    @SuppressWarnings("Duplicates")
    private Callable<EvaluationResult> getCallable(State gameStartingState, INode startingNode,
                                                   Action.Keys keys, int minDuration, int maxDuration) {
        boolean[] buttons = Action.keysToBooleans(keys);

        return () -> {
            GameUnified gameLocal = new GameUnified();
            gameLocal.iterations = 25;
//            gameLocal.useWarmStarting = false;
            gameLocal.makeNewWorld();

            gameLocal.setState(gameStartingState);
            EvaluationResult bestResult = new EvaluationResult();
            //System.out.println("end " + keys.toString());
            float val1 = 0;
            float val2 = 0;
            float val3 = 0;
            for (int i = minDuration; i < maxDuration; i++) {
//                gameLocal.applyBodyImpulse(-0.0005f, 0.0012f);
                if (i > 2) {
                    gameLocal.iterations = 5;
                }
                gameLocal.step(buttons);
                State st = gameLocal.getCurrentState();
                INode nextNode = new NodePlaceholder(startingNode, new Action(i, buttons), st);
                val1 = val2;
                val2 = val3;
                val3 = evaluate(nextNode);
                if (i == minDuration) {
                   // val1 = val3;
                    val2 = val3 * 4f/4f;
                }
                if (i > minDuration) {
//                System.out.println(val);
//                    float sum = val1;
//                    if (val2 < sum) {
//                        sum = val2;
//                    }
//                    if (val3 < sum) {
//                        sum = val3;
//                    }
                    float sum = val1 + val2 + val3;

                    if (sum > bestResult.value) {
                        bestResult.value = sum;
                        bestResult.timestep = i - 1;
                        bestResult.keys = keys;
                    }
                }
            }
            return bestResult;
        };
    }

    private Action getBestActionFromEvaluationResults(List<EvaluationResult> results) {
        EvaluationResult evalResult = results.stream().max(EvaluationResult::compareTo).get();
        return new Action(evalResult.timestep, Action.keysToBooleans(evalResult.keys));
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Action getMaximizingAction(Node currentNode, IGame realGame) {
//        byte[] fullState = realGame.getFullState(); // This one has perfect state recall.
        State fullState = currentNode.getState();
        Objects.requireNonNull(fullState);

        List<Callable<EvaluationResult>> evaluations = new ArrayList<>();
        List<EvaluationResult> evalResults = new ArrayList<>();
        evaluations.add( // No Keys
                getCallable(fullState, currentNode, Action.Keys.none, 1, 15));
        evaluations.add( // QP
                getCallable(fullState, currentNode, Action.Keys.qp, 1, 30));
        evaluations.add( // WO
                getCallable(fullState, currentNode, Action.Keys.wo, 1, 30));
        evaluations.add( // Q
                getCallable(fullState, currentNode, Action.Keys.q, 1, 5));
        evaluations.add( // W
                getCallable(fullState, currentNode, Action.Keys.w, 1, 5));
        evaluations.add( // O
                getCallable(fullState, currentNode, Action.Keys.o, 1, 5));
        evaluations.add( // P
                getCallable(fullState, currentNode, Action.Keys.p, 1, 5));

        // Off keys -- dunno if these are ever helpful.
        evaluations.add( // QO
                getCallable(fullState, currentNode, Action.Keys.qo, 1, 5));
        evaluations.add( // WP
                getCallable(fullState, currentNode, Action.Keys.wp, 1, 5));

        if (multithread) { // Multi-thread
            List<Future<EvaluationResult>> allResults;
            try {
                allResults = ex.invokeAll(evaluations);
                for (Future<EvaluationResult> future : allResults) {
                    evalResults.add(future.get()); // Each blocks until the thread is done.
                }
            }catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else { // Single thread
            try {
                for (Callable<EvaluationResult> eval : evaluations) {
                    evalResults.add(eval.call());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getBestActionFromEvaluationResults(evalResults);
    }

    @Override
    float[] assembleInputFromNode(INode node) {
        return stateStats.standardizeState(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(Node node) {
        return new float[]{node.getValue() / node.visitCount.floatValue()};
    }

    /**
     * Result from evaluating the value over some futures.
     */
    private static class EvaluationResult implements Comparable<EvaluationResult> {

        /**
         * Value after the specified action.
         */
        float value = -Float.MAX_VALUE;

        /**
         * Duration in timesteps of the Action producing this result.
         */
        int timestep = -1;

        /**
         * Keys pressed for the Action producing this result.
         */
        Action.Keys keys;

        @Override
        public int compareTo(EvaluationResult o) {
            return Float.compare(this.value, o.value);
        }
    }
}
