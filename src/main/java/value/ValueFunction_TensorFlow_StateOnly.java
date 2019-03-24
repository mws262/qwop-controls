package value;

import actions.Action;
import game.GameUnified;
import game.IGame;
import game.State;
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
    private int numThreads = 7;

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
        throw new RuntimeException("This value function currently relies on a game full state for save/loading.");
        // TODO make this just run through the whole sequence every time.
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

    private Action getBestActionFromEvaluationResults(List<EvaluationResult> results) {
        EvaluationResult evalResult = results.stream().max(EvaluationResult::compareTo).get();
        return new Action(evalResult.timestep, Action.keysToBooleans(evalResult.keys));
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Action getMaximizingAction(Node currentNode, IGame realGame) {

        byte[] fullState = realGame.getFullState();

        Objects.requireNonNull(fullState);

        List<Callable<EvaluationResult>> evaluations = new ArrayList<>();
        List<EvaluationResult> evalResults = new ArrayList<>();
        evaluations.add( // No Keys
                getCallable(fullState, currentNode, Action.Keys.none, 1, 30));
        evaluations.add( // QP
                getCallable(fullState, currentNode, Action.Keys.qp, 1, 50));
        evaluations.add( // WO
                getCallable(fullState, currentNode, Action.Keys.wo, 1, 50));
        evaluations.add( // Q
                getCallable(fullState, currentNode, Action.Keys.q, 1, 10));
        evaluations.add( // W
                getCallable(fullState, currentNode, Action.Keys.w, 1, 10));
        evaluations.add( // O
                getCallable(fullState, currentNode, Action.Keys.o, 1, 10));
        evaluations.add( // P
                getCallable(fullState, currentNode, Action.Keys.p, 1, 10));

        // Off keys -- dunno if these are ever helpful.
        evaluations.add( // QO
                getCallable(fullState, currentNode, Action.Keys.qo, 1, 10));
        evaluations.add( // WP
                getCallable(fullState, currentNode, Action.Keys.wp, 1, 10));

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
