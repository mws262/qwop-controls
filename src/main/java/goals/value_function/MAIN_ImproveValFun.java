package goals.value_function;

import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MAIN_ImproveValFun {
    public MAIN_ImproveValFun() throws IOException {

        GameQWOP game = new GameQWOP();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(128);
        layerSizes.add(32);

        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--loss");
        additionalArgs.add("target_only"); // Loss has nothing to do with the network output in this case. Just the
        // performance of the controller.
        ValueFunction_TensorFlow_StateOnly<StateQWOP> valFun = new ValueFunction_TensorFlow_StateOnly<>(
                "src/main/resources/tflow_models/test.pb",
                game,
                new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.STDEV),
                layerSizes,
                additionalArgs,
                "src/main/resources/tflow_models/checkpoints" +
                "/checkpoint_lots360", true);

        WeightScoreFunction problem = new WeightScoreFunction(valFun);

        CMAESOptimizer.PopulationSize popSize = new CMAESOptimizer.PopulationSize(5);

        ConvergenceChecker convergenceChecker = (iteration, previous, current) -> false;

        RandomGenerator randomGenerator = new JDKRandomGenerator();
//        CMAESOptimizer optim =
//                new CMAESOptimizer(
//                        5000,
//                        1,
//                        true,
//                        1,
//                        10,
//                        randomGenerator,
//                        true,
//                        convergenceChecker);

        BOBYQAOptimizer optim = new BOBYQAOptimizer(10000);
        optim.optimize(problem.objectiveFunction, problem.initialGuess, problem.bounds, problem.sigma, popSize,
                problem.maxEval, problem.maxIter, WeightScoreFunction.goal);
        // valFun.network.printGraphOperations();
//
//        Controller_ValueFunction controller = new Controller_ValueFunction<>(valFun);
//
//        float[] layer1biases = valFun.network.getLayerBiases(1);
//        float[][] layer2weights = valFun.network.getLayerWeights(2);
//        System.out.println(layer1biases.length);
//        System.out.println(layer2weights.length);
//
//        for (int i = 0; i < 32; i++) {
//            float initWeight = layer2weights[i][0];
//            long baseline = 0;
//            for (int j = 0; j < 5; j++) {
//                ActionQueue actionQueue = new ActionQueue();
//                actionQueue.addAction(new Action(7, Action.Keys.none));
//                game.resetGame();
//
//                int maxTs = 3000;
//                while (!game.isFailed() && game.getTimestepsThisGame() < maxTs && game.getCurrentState().getCenterX() < QWOPConstants.goalDistance) {
//
//                    if (actionQueue.isEmpty()) {
//                        actionQueue.addAction(controller.policy(new NodeGameExplorable(game.getCurrentState()), game)); // Can
//                        // change to game serialization version here.
//                    }
//                    game.step(actionQueue.pollCommand());
//                }
//                IState finalState = game.getCurrentState();
//                System.out.println("Final X: " + finalState.getCenterX() / QWOPConstants.worldScale + "  Time: "
//                        + game.getTimestepsThisGame() * QWOPConstants.timestep);
//
//                if (j == 0) {
//                    baseline = game.getTimestepsThisGame();
//                } else if (finalState.getCenterX() >= 1000f && game.getTimestepsThisGame() < baseline) {
//                    System.out.println("Keeping weight");
//                    break;
//                } else if (j == 4) {
//                    layer2weights[i][0] = initWeight;
//                    valFun.network.setLayerWeights(2, layer2weights);
//                    System.out.println("rejecting new weights");
//                    break;
//                }
////            float loss =
////                    (QWOPConstants.goalDistance - (finalState.getCenterX() - GameQWOP.getInitialState().getCenterX())) * 1f // Distance term should dominate.
////                            - (finalState.getCenterX() - GameQWOP.getInitialState().getCenterX()) / (game.getTimestepsThisGame() * QWOPConstants.timestep) * 0.1f; //
////            System.out.println(loss);
//
////            layer1biases[12] -= 0.1f;
//                layer2weights[i][0] *= 1.01f;
////            valFun.network.setLayerBiases(1, layer1biases);
//                valFun.network.setLayerWeights(2, layer2weights);
//            }
//        }
    }

    public static void main(String[] args) throws IOException {

        new MAIN_ImproveValFun();

        System.exit(0);

    }
}
