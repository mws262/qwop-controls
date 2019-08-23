package goals.value_function;

import com.google.common.base.Preconditions;
import controllers.Controller_ValueFunction;
import game.GameConstants;
import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.action.CommandQWOP;
import game.state.IState;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import tflowtools.TrainableNetwork;
import tree.node.NodeQWOPExplorable;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.IOException;

public class WeightScoreFunction implements MultivariateFunction {

    public static final GoalType goal = GoalType.MINIMIZE;

    private final ValueFunction_TensorFlow_StateOnly valFun;

    private final TrainableNetwork network;

    private final float[] initialBiases; // TODO generalize
    private final float[][] initialWeights; // TODO generalize


    final CMAESOptimizer.Sigma sigma;

    final InitialGuess initialGuess;

    final ObjectiveFunction objectiveFunction;

    public final SimpleBounds bounds;

    final MaxIter maxIter = new MaxIter(1000);
    final MaxEval maxEval = new MaxEval(10000);

    private GameUnified game = new GameUnified();

    private final Controller_ValueFunction<CommandQWOP, ValueFunction_TensorFlow<CommandQWOP>> controller;

    private final int layer = 1;
    private final boolean weights = true;
    WeightScoreFunction(ValueFunction_TensorFlow_StateOnly valFun) {
        this.valFun = valFun;
        this.network = valFun.network;
        initialBiases = network.getLayerBiases(layer); // TODO generalize to other parameters.
        initialWeights = network.getLayerWeights(layer);
        controller = new Controller_ValueFunction<>(valFun);

        float[] initialVals;
        if (weights) {
            initialVals = flattenWeights(initialWeights);
        } else {
            initialVals = initialBiases;
        }

        double[] sigmaValues = new double[initialVals.length];
        double[] initialGuessValues = new double[initialVals.length];
        double[] lb = new double[initialVals.length];
        double[] ub = new double[initialVals.length];
        for (int i = 0; i < initialVals.length; i++) {
            sigmaValues[i] = 0.5 * Math.abs(initialVals[i]);
            initialGuessValues[i] = initialVals[i];
            lb[i] = initialVals[i] - 2.45 * Math.abs(initialVals[i]);
            ub[i] = initialVals[i] + 2.45 * Math.abs(initialVals[i]);
        }

        sigma = new CMAESOptimizer.Sigma(sigmaValues);
        initialGuess = new InitialGuess(initialGuessValues);

        objectiveFunction = new ObjectiveFunction(this);
        bounds = new SimpleBounds(lb, ub);
    }

    private double bestCostSoFar = Double.MAX_VALUE;
    private int bestIdx = 0;
    @Override
    public double value(double[] point) {
        // Set weights according to the objective function input.
        float[] vals = new float[point.length];

        for (int i = 0; i < point.length; i++) {
            vals[i] = (float) point[i];
        }

        if (weights) {
            Preconditions.checkArgument(initialWeights.length * initialWeights[0].length == point.length);
            network.setLayerWeights(layer, expandWeights(vals, initialWeights.length, initialWeights[0].length));
        } else {
            Preconditions.checkArgument(initialBiases.length == point.length);
            network.setLayerBiases(layer, vals);
        }

        // Evaluate it.
        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();
        actionQueue.addAction(new Action<>(7, CommandQWOP.NONE));
        game.makeNewWorld();

        int maxTs = 3000;
        while (!game.getFailureStatus() && game.getTimestepsThisGame() < maxTs && game.getCurrentState().getCenterX() < GameConstants.goalDistance) {

            if (actionQueue.isEmpty()) {
                actionQueue.addAction(controller.policy(new NodeQWOPExplorable<>(game.getCurrentState()), game)); // Can
                // change to game serialization version here.
            }
            game.step(actionQueue.pollCommand());
        }
        IState finalState = game.getCurrentState();
        System.out.println("Final X: " + finalState.getCenterX() / GameConstants.worldScale + "  Time: "
                + game.getTimestepsThisGame() * GameConstants.timestep);

        double val =  (GameConstants.goalDistance - Math.min(GameConstants.goalDistance,
                finalState.getCenterX() - GameUnified.getInitialState().getCenterX())) * 10f + game.getTimestepsThisGame();
        System.out.println(val);

        if (val < bestCostSoFar) {
            if (bestIdx > 0) { // Skip the first one since it will just be the evaluation of the initial guess anyway.
                try {
                    network.saveCheckpoint("src/main/resources/tflow_models/checkpoints/improvement" + bestIdx);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            bestCostSoFar = val;
            bestIdx++;
        }
        return val;
    }

    /**
     * Convert from a 2D array of weights to a 1D array for optimization.
     * @param weights Network weights. Should NOT be a jagged array.
     * @return A single-dimensional array with the same number of elements as the input.
     */
    private static float[] flattenWeights(float[][] weights) {
        float[] flat = new float[weights.length * weights[0].length];

        int idx = 0;
        for (float[] weight : weights) {
            for (int j = 0; j < weights[0].length; j++) {
                flat[idx++] = weight[j];
            }
        }
        return flat;
    }

    /**
     * Convert a flattened, 1D array of the network weights back into the 2D version for assignment back into the
     * network. Provided dimensions MUST be consistent with the number of elements in the provided weights.
     * @param flatWeights 1D array of the weights.
     * @param dim1 First dimension of the expanded weights.
     * @param dim2 Second dimension of the expanded weights.
     * @return Re-expanded 2D array of the network weights.
     */
    private static float[][] expandWeights(float[] flatWeights, int dim1, int dim2) {
        Preconditions.checkArgument(dim1 * dim2 == flatWeights.length, "Given dimensions don't match the given array" +
                ".", flatWeights.length);

        float[][] weights = new float[dim1][dim2];

        int idx = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                weights[i][j] = flatWeights[idx++];
            }
        }
        return weights;
    }
}
