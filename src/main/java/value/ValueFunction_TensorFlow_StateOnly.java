package value;

import actions.Action;
import com.sun.istack.NotNull;
import game.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tflowtools.TrainableNetwork;
import tree.NodeQWOP;
import tree.NodeQWOPBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static actions.Action.Keys;
import static actions.Action.keysToBooleans;

public class ValueFunction_TensorFlow_StateOnly extends ValueFunction_TensorFlow {

    /**
     * Dimension of the state variable input.
     */
    private static final int STATE_SIZE = 72;

    /**
     * Dimension of the value output.
     */
    private static final int VALUE_SIZE = 1;


    private final boolean multithread = true;

    /**
     * Handles distributing different predictive simulations to different threads to run simultaneously.
     */
    private ExecutorService executor;
    private List<EvaluationResult> evalResults;
    private List<FuturePredictor> evaluations;

    /**
     * Number of threads to distribute the predictive simulations to. There are 9 predicted futures, so this is a
     * natural number to choose, assuming the computer can handle it.
     */
    private int numThreads = 9;

    public EvaluationResult currentResult;

    private static Logger logger = LogManager.getLogger(ValueFunction_TensorFlow_StateOnly.class);

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
     * @throws FileNotFoundException Model file was not successfully created.
     */
    public ValueFunction_TensorFlow_StateOnly(String fileName, List<Integer> hiddenLayerSizes,
                                              List<String> additionalArgs) throws FileNotFoundException {
        super(fileName, STATE_SIZE, VALUE_SIZE, hiddenLayerSizes, additionalArgs);
        assignFuturePredictors();
        if (multithread)
            executor = Executors.newFixedThreadPool(numThreads);
    }

    private ValueFunction_TensorFlow_StateOnly(TrainableNetwork network) {
        super(network);
    }
    /**
     * Assign the futures that will be explored on each controller evaluation.
     */
    private void assignFuturePredictors() {
        evaluations = new ArrayList<>();
        evalResults = new ArrayList<>();
        evaluations.add(new FuturePredictor(Keys.none, 1, 10));
        evaluations.add(new FuturePredictor(Keys.qp, 2, 40));
        evaluations.add(new FuturePredictor(Keys.wo, 2, 40));
        evaluations.add(new FuturePredictor(Keys.q, 2, 5));
        evaluations.add(new FuturePredictor(Keys.w, 2, 5));
        evaluations.add(new FuturePredictor(Keys.o, 2, 5));
        evaluations.add(new FuturePredictor(Keys.p, 2, 5));
        evaluations.add(new FuturePredictor(Keys.qo, 2, 5));
        evaluations.add(new FuturePredictor(Keys.wp, 2, 5));

    }

    @Override
    public Action getMaximizingAction(NodeQWOPBase<?> currentNode) {
        // Update each of the future predictors to use the new starting states.
        evaluations.forEach(e -> e.setStartingState(currentNode.getState()));
        return runEvaluations();

    }

    @Override
    public Action getMaximizingAction(NodeQWOPBase<?> currentNode, IGameSerializable realGame) {
        evaluations.forEach(e -> e.setStartingState(realGame.getSerializedState()));
        return runEvaluations();
    }

    private Action runEvaluations() {
        long initialTime = System.currentTimeMillis();

        evalResults.clear(); // Remove existing results from any previous evaluations.
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
        EvaluationResult evalResult = evalResults.stream().max(EvaluationResult::compareTo).orElse(null);
        Objects.requireNonNull(evalResult);

        currentResult = evalResult;
        logger.info(String.format("Policy evaluated. \tTime: %3d ms \tAction: [%3d, %4s]\t\tValue: %3.2f \tBodyX: %3.2f",
                System.currentTimeMillis() - initialTime, evalResult.timestep, evalResult.keys.toString(),
                Math.round(evalResult.value * 100)/100f, evalResult.state.getCenterX()));

        return new Action(evalResult.timestep, evalResult.keys);
    }

    @Override
    float[] assembleInputFromNode(NodeQWOPBase<?> node) {
        return stateStats.standardizeState(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(NodeQWOPBase<?> node) {
        return new float[]{node.getValue()};
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
        public IState state;

        @Override
        public int compareTo(EvaluationResult o) {
            return Float.compare(this.value, o.value);
        }
    }

    /**
     * How future predictions are chosen from within a single button combination held for up to a specified duration.
     */
    private enum SelectionCriteria {
        BEST_OVERALL, BEST_AVERAGE_WINDOW, BEST_WORST_CASE_WINDOW
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
        private IState startingState;

        private byte[] startStateFull;
        private boolean useSerializedState = false;

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
         * Number of physics iterations to start this prediction with. It is useful to have this greater than default
         * to let the cold-started game "catch up" to the normal game.
         */
        private final int initialPhysicsIterations = 4 * GameConstants.physIterations;

        /**
         * Number of timesteps to run the game at the modified number of physics iterations before going back to
         * default.
         */
        private final int warmstartIterationCount = 1;

        /**
         * Do we construct a new game between prediction calls? This speeds it up and reduces garbage collection, but
         * makes each call depend on the previous to some extent. Perhaps good for performance, terrible for
         * predicability.
         */
        private final boolean newGameBetweenPredictions = true;


        private SelectionCriteria selectionCriteria = SelectionCriteria.BEST_AVERAGE_WINDOW;

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

        @NotNull
        void setStartingState(IState startingState) {
            this.startingState = startingState;
            useSerializedState = false;
        }

        @NotNull
        void setStartingState(byte[] startingState) {
            this.startStateFull = startingState;
            useSerializedState = true;
        }

        @Override
        public EvaluationResult call() {

            if (useSerializedState) {
                gameLocal = gameLocal.restoreSerializedState(startStateFull);
                gameLocal.iterations = GameConstants.physIterations; // Don't need to 'catch up', since full game is
            } else {
                if (newGameBetweenPredictions)
                    gameLocal.makeNewWorld();

                gameLocal.setState(startingState);
                gameLocal.iterations = initialPhysicsIterations; // Catch-up iterations for cold-start game to
                // "catch-up" to warm-started game.
            }

            float startX = gameLocal.getCurrentState().getStateVariableFromName(IState.ObjectName.BODY).getDx();

            // Reset the game and set it to the specified starting state.
            bestResult.value = -Float.MAX_VALUE;

            // Keep track of a window of three adjacent actions. Some of the selection approaches do a
            // best-worst-case.
            float val1;
            float val2 = 0;
            float val3 = 0;

            float x2 = startX;
            float x3 = startX;

            for (int i = 1; i <= maxHorizon; i++) {
                // Return to the normal number of physics iterations after the first step.
                if (i > warmstartIterationCount + 1) {
                    gameLocal.iterations = GameConstants.physIterations;
                }

                x2 = x3;

                gameLocal.step(buttons);
                IState st = gameLocal.getCurrentState();
                NodeQWOPBase<?> nextNode = new NodeQWOP(st);
                val1 = val2;
                val2 = val3;
                val3 = evaluate(nextNode);

                x3 = st.getStateVariableFromName(IState.ObjectName.BODY).getDx();

                if (i == 1) {
                    // val1 = val3;
                    val2 = val3 * 4f/4f;
                }

                if (i > 1 && i > minHorizon) {

                    // Decide what value should be recorded as best.
                    float aggregateVal;
                    switch (selectionCriteria) {
                        case BEST_OVERALL:
                            // Best overall value.
                            aggregateVal = val2;
                            break;
                        case BEST_AVERAGE_WINDOW:
                            // Best average window of 3 neighboring durations.
                            aggregateVal = (val1 + val2 + val3)/3f;
                            break;
                        case BEST_WORST_CASE_WINDOW:
                            // Best window of 3 as determined by the WORST of that 3.
                            aggregateVal = val1;
                            if (val2 < aggregateVal) {
                                aggregateVal = val2;
                            }
                            if (val3 < aggregateVal) {
                                aggregateVal = val3;
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid selection criteria invoked.");
                    }

                    aggregateVal += 0.0f * x2; //(x2 - startX) * .2f;

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

    @NotNull
    public ValueFunction_TensorFlow_StateOnly getCopy() {
        ValueFunction_TensorFlow_StateOnly valFunCopy = new ValueFunction_TensorFlow_StateOnly(network);
        valFunCopy.assignFuturePredictors();
        if (multithread)
            valFunCopy.executor = Executors.newFixedThreadPool(numThreads);
        return valFunCopy;
    }
}
