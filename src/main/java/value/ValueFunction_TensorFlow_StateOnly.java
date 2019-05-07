package value;

import actions.Action;
import game.GameConstants;
import game.GameUnified;
import game.IGameInternal;
import game.State;
import tree.INode;
import tree.Node;
import tree.NodePlaceholder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.*;

import static actions.Action.*;

public class ValueFunction_TensorFlow_StateOnly extends ValueFunction_TensorFlow {

    /**
     * Dimension of the state variable input.
     */
    public static final int STATE_SIZE = 72;

    /**
     * Dimension of the value output.
     */
    public static final int VALUE_SIZE = 1;


    boolean multithread = true;

    /**
     * Handles distributing different predictive simulations to different threads to run simultaneously.
     */
    private ExecutorService executor;
    List<EvaluationResult> evalResults;
    List<FuturePredictor> evaluations;

    /**
     * Number of threads to distribute the predictive simulations to. There are 9 predicted futures, so this is a
     * natural number to choose, assuming the computer can handle it.
     */
    private int numThreads = 9;

    public EvaluationResult currentResult;

    /**
     * Constructor which loads an existing value function net.
     * @param file .pb file of the existing net.
     * @throws FileNotFoundException Unable to find an existing net.
     */
    public ValueFunction_TensorFlow_StateOnly(File file) throws FileNotFoundException {
        super(file);
        assignFuturePredictors();
        if (multithread)
            executor = Executors.newFixedThreadPool(numThreads);
    }

    /**
     * Constructor which makes a new value function net based on provided parameters. If this net is similar enough
     * to a previously-used one, you can probably load a checkpoint file with weights with it too.
     * @param fileName File name of the new .pb net. Don't include file extension.
     * @param hiddenLayerSizes Sizes of the hidden layers of the net. Don't include the input or output layer sizes.
     *                         They are defined in {@link ValueFunction_TensorFlow_StateOnly#STATE_SIZE} and
     *                         {@link ValueFunction_TensorFlow_StateOnly#VALUE_SIZE}.
     * @param additionalArgs Additional arguments to pass to the network creation script.
     * @throws FileNotFoundException
     */
    public ValueFunction_TensorFlow_StateOnly(String fileName, List<Integer> hiddenLayerSizes,
                                              List<String> additionalArgs) throws FileNotFoundException {
        super(fileName, STATE_SIZE, VALUE_SIZE, hiddenLayerSizes, additionalArgs);
        assignFuturePredictors();
        if (multithread)
            executor = Executors.newFixedThreadPool(numThreads);
    }

    /**
     * Assign the futures that will be explored on each controller evaluation.
     */
    private void assignFuturePredictors() {
        evaluations = new ArrayList<>();
        evalResults = new ArrayList<>();
        evaluations.add(new FuturePredictor(Keys.none, 1, 10));
        evaluations.add(new FuturePredictor(Keys.qp, 2, 45));
        evaluations.add(new FuturePredictor(Keys.wo, 2, 45));
        evaluations.add(new FuturePredictor(Keys.q, 2, 5));
        evaluations.add(new FuturePredictor(Keys.w, 2, 5));
        evaluations.add(new FuturePredictor(Keys.o, 2, 5));
        evaluations.add(new FuturePredictor(Keys.p, 2, 5));
        evaluations.add(new FuturePredictor(Keys.qo, 2, 5));
        evaluations.add(new FuturePredictor(Keys.wp, 2, 5));

    }

    @Override
    public Action getMaximizingAction(Node currentNode) {
        evalResults.clear(); // Remove existing results from any previous evaluations.

        State state = Objects.requireNonNull(currentNode.getState(), "Received node did not have a state assigned.");

        // Update each of the future predictors to use the new starting states.
        evaluations.forEach(e -> e.setStartingState(state));

        try{
            if (multithread) { // Multi-thread, send to executor.
                List<Future<EvaluationResult>> allResults = executor.invokeAll(evaluations);
                for (Future<EvaluationResult> future : allResults) {
                    evalResults.add(future.get()); // Each blocks until the thread is done.
                }

            } else { // Single thread, just call right here.
                for (Callable<EvaluationResult> eval : evaluations) {
                    evalResults.add(eval.call());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        EvaluationResult evalResult = evalResults.stream().max(EvaluationResult::compareTo).get();
        currentResult = evalResult;
        return new Action(evalResult.timestep, evalResult.keys);
    }


//    private Callable<EvaluationResult> getCallable(byte[] gameStartingState, INode startingNode,
//                                                   Action.Keys keys, int minDuration, int maxDuration) {
//        boolean[] buttons = Action.keysToBooleans(keys);
//
//        return () -> {
//            GameUnified gameLocal = GameUnified.restoreFullState(gameStartingState);
//            EvaluationResult bestResult = new EvaluationResult();
//            for (int i = minDuration; i < maxDuration; i++) {
//                gameLocal.step(buttons);
//                State st = gameLocal.getCurrentState();
//                INode nextNode = new NodePlaceholder(startingNode, new Action(i, buttons), st);
//                float val = evaluate(nextNode);
//                if (val > bestResult.value) {
//                    bestResult.value = val;
//                    bestResult.timestep = i;
//                    bestResult.keys = keys;
//                }
//            }
//            return bestResult;
//        };
//    }

    @Override
    public Action getMaximizingAction(Node currentNode, IGameInternal realGame) {
        return null;
    }
////        byte[] fullState = realGame.getFullState(); // This one has perfect state recall.
//        State fullState = currentNode.getState();
//        Objects.requireNonNull(fullState);
//
//        List<Callable<EvaluationResult>> evaluations = new ArrayList<>();
//        List<EvaluationResult> evalResults = new ArrayList<>();
//        evaluations.add( // No Keys
//                getCallable(fullState, currentNode, Keys.none, 1, 15));
//        evaluations.add( // QP
//                getCallable(fullState, currentNode, Keys.qp, 2, 55));
//        evaluations.add( // WO
//                getCallable(fullState, currentNode, Keys.wo, 2, 55));
//        evaluations.add( // Q
//                getCallable(fullState, currentNode, Keys.q, 2, 5));
//        evaluations.add( // W
//                getCallable(fullState, currentNode, Keys.w, 2, 5));
//        evaluations.add( // O
//                getCallable(fullState, currentNode, Keys.o, 2, 5));
//        evaluations.add( // P
//                getCallable(fullState, currentNode, Keys.p, 2, 5));
//
//        // Off keys -- dunno if these are ever helpful.
//        evaluations.add( // QO
//                getCallable(fullState, currentNode, Keys.qo, 2, 5));
//        evaluations.add( // WP
//                getCallable(fullState, currentNode, Keys.wp, 2, 5));
//
//        if (multithread) { // Multi-thread
//            List<Future<EvaluationResult>> allResults;
//            try {
//                allResults = executor.invokeAll(evaluations);
//                for (Future<EvaluationResult> future : allResults) {
//                    evalResults.add(future.get()); // Each blocks until the thread is done.
//                }
//            }catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//        } else { // Single thread
//            try {
//                for (Callable<EvaluationResult> eval : evaluations) {
//                    evalResults.add(eval.call());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return getBestActionFromEvaluationResults(evalResults);
//    }

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
    public static class EvaluationResult implements Comparable<EvaluationResult> {

        /**
         * Value after the specified action.
         */
        public float value = -Float.MAX_VALUE;

        /**
         * Duration in timesteps of the Action producing this result.
         */
        public int timestep = -1;

        /**
         * Keys pressed for the Action producing this result.
         */
        public Keys keys;

        /**
         * State observed from this evaluation.
         */
        public State state;

        @Override
        public int compareTo(EvaluationResult o) {
            return Float.compare(this.value, o.value);
        }
    }


    /**
     * For executing short-term predictions of the future in parallel or not.
     * Each one has an assigned set of keys and a minimum and maximum number of timesteps in the future to pay
     * attention to. Each time a new prediction is needed, just give a new starting state.
     */
    private class FuturePredictor implements Callable<EvaluationResult> {

        /**
         * Game copy used to predict this future.
         */
        private GameUnified gameLocal = new GameUnified();

        /**
         * Initial state of this future prediction.
         */
        private State startingState;

        /**
         * QWOP keys pressed during this future prediction.
         */
        private final boolean[] buttons;

        /**
         * Equivalent key representation.
         */
        private final Keys keys;

        /**
         * Minimum number of timesteps in the future which will be scored.
         */
        private final int minHorizon;

        /**
         * Maximum number of timesteps in the future which will be predicted.
         */
        private final int maxHorizon;

        /**
         * Best result from within this prediction of the future.
         */
        private EvaluationResult bestResult = new EvaluationResult();

        FuturePredictor(Keys keys, int minHorizon, int maxHorizon) {
            this.keys = keys;
            buttons = keysToBooleans(keys);
            this.minHorizon = minHorizon;
            this.maxHorizon = maxHorizon;
        }

        void setStartingState(State startingState) {
            this.startingState = startingState;
        }

        @Override
        public EvaluationResult call() {
            gameLocal.iterations = GameConstants.physIterations * 4; // Tunable parameter for getting caught up to the
            // warm-start of the normal game.

            // Reset the game and set it to the specified starting state.
            bestResult.value = -Float.MAX_VALUE;
            gameLocal.makeNewWorld(); // TODO eliminating this speeds up the control loop quite a bit, but also
            // makes the controller less predictable.
            gameLocal.setState(startingState);

            // Keep track of a window of three adjacent actions. Some of the selection approaches do a
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
                State st = gameLocal.getCurrentState();
                INode nextNode = new NodePlaceholder(null, null, st);
                val1 = val2;
                val2 = val3;
                val3 = evaluate(nextNode);
                if (i == 1) {
                    // val1 = val3;
                    val2 = val3 * 4f/4f;
                }
                if (i > 1 && i > minHorizon) {
                    float aggregateVal;

                    // Best worst case window
//                    float aggregateVal = val1;
//                    if (val2 < aggregateVal) {
//                        aggregateVal = val2;
//                    }
//                    if (val3 < aggregateVal) {
//                        aggregateVal = val3;
//                    }
                    // Best average window.
                    aggregateVal = (val1 + val2 + val3)/3f;

                    // Best single.
//                    float sum = val2;

                    if (aggregateVal > bestResult.value) {
                        bestResult.value = aggregateVal;
                        bestResult.timestep = i - 1; // TODO TEMP 2 for hardware. 1 is correct otherwise.
                        bestResult.keys = keys;
                        bestResult.state = st;
                    }
                }
            }
            return bestResult;
        }
    }
}
