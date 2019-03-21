package value;

import actions.Action;
import game.GameUnified;
import game.IGame;
import game.State;
import tree.Node;

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


    private Callable<EvaluationResult> getCallable(byte[] gameStartingState, Node startingNode,
                                                   EvaluationResult.Keys keys, int minDuration, int maxDuration) {
        boolean[] buttons = EvaluationResult.labelsToButtons.get(keys);

        return () -> {
            GameUnified gameLocal = GameUnified.restoreFullState(gameStartingState);
            EvaluationResult bestResult = new EvaluationResult();
            for (int i = minDuration; i < maxDuration; i++) {
                gameLocal.step(buttons);
                State st = gameLocal.getCurrentState();
                Node nextNode = new Node(startingNode, new Action(i, buttons), false);
                nextNode.setState(st);
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
        return new Action(evalResult.timestep, EvaluationResult.labelsToButtons.get(evalResult.keys));
    }

    @SuppressWarnings("Duplicates")
    @Override
    public Action getMaximizingAction(Node currentNode, IGame realGame) {

        byte[] fullState = realGame.getFullState();

        Objects.requireNonNull(fullState);

        List<Callable<EvaluationResult>> evaluations = new ArrayList<>();
        List<EvaluationResult> evalResults = new ArrayList<>();
        evaluations.add( // No Keys
                getCallable(fullState, currentNode, EvaluationResult.Keys.none, 1, 30));
        evaluations.add( // QP
                getCallable(fullState, currentNode, EvaluationResult.Keys.qp, 1, 50));
        evaluations.add( // WO
                getCallable(fullState, currentNode, EvaluationResult.Keys.wo, 1, 50));
        evaluations.add( // Q
                getCallable(fullState, currentNode, EvaluationResult.Keys.q, 1, 10));
        evaluations.add( // W
                getCallable(fullState, currentNode, EvaluationResult.Keys.w, 1, 10));
        evaluations.add( // O
                getCallable(fullState, currentNode, EvaluationResult.Keys.o, 1, 10));
        evaluations.add( // P
                getCallable(fullState, currentNode, EvaluationResult.Keys.p, 1, 10));

        if (multithread) { // Multithread seems to weigh in at about 15ms per evaluation of controller. Single is
            // maybe 25ms

            List<Future<EvaluationResult>> allResults = null;

            try {
                allResults = ex.invokeAll(evaluations);
                for (Future<EvaluationResult> future : allResults) {
                    evalResults.add(future.get());
                }
            }catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        } else {
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
    float[] assembleInputFromNode(Node node) {
        return stateStats.standardizeState(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(Node node) {
        return new float[]{node.getValue() / node.visitCount.floatValue()};
    }

    private static class EvaluationResult implements Comparable<EvaluationResult> {
        enum Keys {
            q, w, o, p, qp, wo, none
        }

        final static Map<Keys, boolean[]> labelsToButtons = new HashMap<>();
        static {
            labelsToButtons.put(Keys.q, new boolean[]{true, false, false, false});
            labelsToButtons.put(Keys.w, new boolean[]{false, true, false, false});
            labelsToButtons.put(Keys.o, new boolean[]{false, false, true, false});
            labelsToButtons.put(Keys.p, new boolean[]{false, false, false, true});
            labelsToButtons.put(Keys.qp, new boolean[]{true, false, false, true});
            labelsToButtons.put(Keys.wo, new boolean[]{false, true, true, false});
            labelsToButtons.put(Keys.none, new boolean[]{false, false, false, false});
        }

        float value = -Float.MAX_VALUE;
        int timestep = -1;
        Keys keys;

        @Override
        public int compareTo(EvaluationResult o) {
            return Float.compare(this.value, o.value);
        }
    }
}
