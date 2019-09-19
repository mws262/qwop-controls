package value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import game.qwop.*;
import game.IGameSerializable;
import game.action.Action;
import game.state.IState;
import game.state.transform.ITransform;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import tree.node.NodeGame;
import tree.node.NodeGameBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static game.qwop.CommandQWOP.Keys;

public class ValueFunction_TensorFlow_StateOnly<S extends IStateQWOP> extends ValueFunction_TensorFlow<CommandQWOP, S> {

    /**
     * Dimension of the value output.
     */
    private static final int VALUE_SIZE = 1;


    private final boolean multithread = true;

    /**
     * Handles distributing different predictive simulations to different threads to run simultaneously.
     */
    private ExecutorService executor;
    List<EvaluationResult> evalResults;
    List<FuturePredictor> evaluations;

    public final IGameSerializable<CommandQWOP, S> gameTemplate;
    public final String fileName;

    /**
     * Number of threads to distribute the predictive simulations to. There are 9 predicted futures, so this is a
     * natural number to choose, assuming the computer can handle it.
     */
    private int numThreads = 9;

    private static final Logger logger = LogManager.getLogger(ValueFunction_TensorFlow_StateOnly.class);

    public final ITransform<S> stateNormalizer;

    /**
     * Constructor which loads an existing value function net.
     * @param file .pb file of the existing net.
     * @throws FileNotFoundException Unable to find an existing net.
     */
    public ValueFunction_TensorFlow_StateOnly(File file,
                                              IGameSerializable<CommandQWOP, S> gameTemplate,
                                              ITransform<S> stateNormalizer,
                                              float keepProbability,
                                              boolean tensorboardLogging) throws FileNotFoundException {
        super(file, keepProbability, tensorboardLogging);
        Preconditions.checkArgument(gameTemplate.getStateDimension() == inputSize, "Graph file should have input matching the provide game template's " +
                "state size.", gameTemplate.getStateDimension());
        Preconditions.checkArgument(outputSize == 1, "Value function output for this controller should have precisely" +
                " one output.");

        this.gameTemplate = gameTemplate.getCopy();
        this.stateNormalizer = stateNormalizer;
        fileName = file.getName();
        assignFuturePredictors(this.gameTemplate);
        if (multithread)
            executor = Executors.newFixedThreadPool(numThreads);
    }

    /**
     * Constructor which makes a new value function net based on provided parameters. If this net is similar enough
     * to a previously-used one, you can probably load a checkpoint file with weights with it too.
     * @param fileName File name of the new .pb net. Don't include file extension.
     * @param hiddenLayerSizes Sizes of the hidden layers of the net. Don't include the input or output layer sizes.
     * @param additionalNetworkArgs Additional arguments to pass to the network creation script.
     * @throws FileNotFoundException Model file was not successfully created.
     */
    public ValueFunction_TensorFlow_StateOnly(@JsonProperty("fileName") String fileName,
                                              @JsonProperty("gameTemplate") IGameSerializable<CommandQWOP, S> gameTemplate,
                                              @JsonProperty("stateNormalizer") ITransform<S> stateNormalizer,
                                              @JsonProperty("hiddenLayerSizes") List<Integer> hiddenLayerSizes,
                                              @JsonProperty("additionalNetworkArgs") List<String> additionalNetworkArgs,
                                              @JsonProperty("activeCheckpoint") String checkpointFile,
                                              @JsonProperty("keepProbability") float keepProbability,
                                              @JsonProperty("tensorboardLogging") boolean tensorboardLogging) throws IOException {
        super(fileName, gameTemplate.getStateDimension(), VALUE_SIZE, hiddenLayerSizes, additionalNetworkArgs,
                checkpointFile, keepProbability, tensorboardLogging);
        this.gameTemplate = gameTemplate;
        this.stateNormalizer = stateNormalizer;
        this.fileName = fileName;
        assignFuturePredictors(gameTemplate);
        if (multithread)
            executor = Executors.newFixedThreadPool(numThreads);
    }

    /**
     * Assign the futures that will be explored on each controller evaluation.
     */
    private void assignFuturePredictors(IGameSerializable<CommandQWOP, S> gameTemplate) {
        evaluations = new ArrayList<>();
        evalResults = new ArrayList<>();
        int min = 2;
        evaluations.add(new FuturePredictor(gameTemplate, Keys.none, min, 10));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.qp, min, 35));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.wo, min, 35));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.q, min, 5));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.w, min, 5));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.o, min, 5));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.p, min, 5));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.qo, min, 5));
        evaluations.add(new FuturePredictor(gameTemplate, Keys.wp, min, 5));

    }

    @Override
    public Action<CommandQWOP> getMaximizingAction(NodeGameBase<?, CommandQWOP, S> currentNode) {
        // Update each of the future predictors to use the new starting states.
        evaluations.forEach(e -> e.setStartingState(currentNode.getState()));
        return runEvaluations();

    }

    @Override
    public Action<CommandQWOP> getMaximizingAction(NodeGameBase<?, CommandQWOP, S> currentNode,
                                                   IGameSerializable<CommandQWOP, S> realGame) {
        evaluations.forEach(e -> e.setStartingState(realGame.getSerializedState()));
        return runEvaluations();
    }

    private Action<CommandQWOP> runEvaluations() {
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
        } catch (ExecutionException e) {
            return new Action<>(1, CommandQWOP.NONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EvaluationResult evalResult = evalResults.stream().max(EvaluationResult::compareTo).orElse(null);
        Objects.requireNonNull(evalResult);

        if (logger.getLevel().isLessSpecificThan(Level.DEBUG)) {
            logger.debug(String.format("Policy evaluated. \tTime: %3d ms \tAction: [%3d, %4s]\t\tValue: %3.2f \tBodyX: %3" +
                            ".2f",
                    System.currentTimeMillis() - initialTime, evalResult.timestep, evalResult.keys.toString(),
                    Math.round(evalResult.value * 100) / 100f, evalResult.state.getCenterX()));
        }

        return new Action<>(evalResult.timestep, CommandQWOP.getCommand(evalResult.keys));
    }

    @Override
    float[] assembleInputFromNode(NodeGameBase<?, CommandQWOP, S> node) {
        return stateNormalizer.transform(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(NodeGameBase<?, CommandQWOP, S> node) {
        return new float[]{node.getValue()};
    }

    /**
     * Result from evaluating the value over some futures.
     */
    public static class EvaluationResult implements Comparable<EvaluationResult> {

        /**
         * Value after the specified command.
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
         * StateQWOP observed from this evaluation.
         */
        public IState state;

        @Override
        public int compareTo(@NotNull EvaluationResult o) {
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
    class FuturePredictor implements Callable<EvaluationResult> {

        /**
         * Game copy used to predict this future.
         */
        IGameSerializable<CommandQWOP, S> gameLocal;

        /**
         * Initial state of this future prediction.``
         */
        S startingState;

        private byte[] startStateFull;
        private boolean useSerializedState = false;

        /**
         * QWOP keys pressed during this future prediction.
         */
        final CommandQWOP command;

        /**
         * Equivalent key representation.
         */
        final Keys keys;

        /**
         * Minimum number of timesteps in the future which will be scored.
         */
        final int minHorizon;

        /**
         * Maximum number of timesteps in the future which will be predicted.
         */
        final int maxHorizon;

        /**
         * Number of physics iterations to start this prediction with. It is useful to have this greater than default
         * to let the cold-started game "catch up" to the normal game.
         */
        private final int initialPhysicsIterations = 4 * QWOPConstants.physIterations;

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
        EvaluationResult bestResult = new EvaluationResult();

        FuturePredictor(IGameSerializable<CommandQWOP, S> gameTemplate, Keys keys, int minHorizon,
                        int maxHorizon) {
            this.gameLocal = gameTemplate.getCopy();
            this.keys = keys;
            command = CommandQWOP.getCommand(keys);
            this.minHorizon = minHorizon;
            this.maxHorizon = maxHorizon;
        }

        void setStartingState(@NotNull S startingState) {
            this.startingState = startingState;
            useSerializedState = false;
        }

        void setStartingState(@NotNull byte[] startingState) {
            this.startStateFull = startingState;
            useSerializedState = true;
        }

        @Override
        public EvaluationResult call() {

            if (useSerializedState) {
                gameLocal = gameLocal.restoreSerializedState(startStateFull);
                gameLocal.setPhysicsIterations(QWOPConstants.physIterations); // Don't need to 'catch up', since
                // full game is
            } else {
                if (newGameBetweenPredictions)
                    gameLocal.resetGame();

                gameLocal.setState(startingState);
                gameLocal.setPhysicsIterations(initialPhysicsIterations); // Catch-up iterations for cold-start game to
                // "catch-up" to warm-started game.
            }

            float startX = gameLocal.getCurrentState().getCenterX();

            // Reset the game and set it to the specified starting state.
            bestResult.value = -Float.MAX_VALUE;

            // Keep track of a window of three adjacent game.command. Some of the selection approaches do a
            // best-worst-case.
            float val1;
            float val2 = 0;
            float val3 = 0;

            float x2;
            float x3 = startX;

            for (int i = 1; i <= maxHorizon; i++) {
                // Return to the normal number of physics iterations after the first step.
                if (i > warmstartIterationCount) {
                    gameLocal.setPhysicsIterations(QWOPConstants.physIterations);
                }

                x2 = x3;

                gameLocal.step(command);
                S st = gameLocal.getCurrentState();
                NodeGameBase<?, CommandQWOP, S> nextNode = new NodeGame<>(st);
                val1 = val2;
                val2 = val3;
                val3 = evaluate(nextNode);

                x3 = st.getCenterX();

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

                    aggregateVal += (x2 - startX) * 0f;

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

    @JsonIgnore
    public ValueFunction_TensorFlow_StateOnly<S> getCopy() {
        ValueFunction_TensorFlow_StateOnly<S> valFunCopy = null;
        try {
            valFunCopy = new ValueFunction_TensorFlow_StateOnly<>(
                    getGraphDefinitionFile(),
                    gameTemplate,
                    stateNormalizer,
                    keepProbability,
                    tensorboardLogging);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return valFunCopy;
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.close();
    }
}
