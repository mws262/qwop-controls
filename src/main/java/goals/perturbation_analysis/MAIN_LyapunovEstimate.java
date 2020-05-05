package goals.perturbation_analysis;

import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import ui.runner.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.BiFunction;

public class MAIN_LyapunovEstimate extends JFrame {

    private final ActionQueue<CommandQWOP> queue = new ActionQueue<>();
    private PanelRunner_SimpleState<StateQWOP> runnerPanel;
    private int SEQUENCE_LENGTH = 20;
    private final GameQWOP game = new GameQWOP();

    private int START_TS = 200;
    private int START_ACTION = 1;

    private double diffSize = 2 * Math.sqrt(Math.pow(2,-23)); // Saw recommendation to do sqrt of precision.

    public MAIN_LyapunovEstimate() {

        queue.addSequence(
                // 0
                new Action<>(27, CommandQWOP.NONE),
                new Action<>(8, CommandQWOP.WO),
                new Action<>(29, CommandQWOP.NONE),
                new Action<>(16, CommandQWOP.QP),

                // 1
                new Action<>(10, CommandQWOP.NONE),
                new Action<>(21, CommandQWOP.WO),
                new Action<>(7, CommandQWOP.NONE),
                new Action<>(22, CommandQWOP.QP),

                // 2
                new Action<>(17, CommandQWOP.NONE),
                new Action<>(15, CommandQWOP.WO),
                new Action<>(24, CommandQWOP.NONE),
                new Action<>(15, CommandQWOP.QP),

                // 3
                new Action<>(14, CommandQWOP.NONE),
                new Action<>(22, CommandQWOP.WO),
                new Action<>(16, CommandQWOP.NONE),
                new Action<>(16, CommandQWOP.QP),

                // 4
                new Action<>(19, CommandQWOP.NONE),
                new Action<>(20, CommandQWOP.WO),
                new Action<>(21, CommandQWOP.NONE),
                new Action<>(20, CommandQWOP.QP),

                // 5
                new Action<>(21, CommandQWOP.NONE),
                new Action<>(16, CommandQWOP.WO),
                new Action<>(23, CommandQWOP.NONE),
                new Action<>(14, CommandQWOP.QP),

                // 6
                new Action<>(15, CommandQWOP.NONE),
                new Action<>(13, CommandQWOP.WO),
                new Action<>(17, CommandQWOP.NONE),
                new Action<>(13, CommandQWOP.QP),

                // 7
                new Action<>(22, CommandQWOP.NONE),
                new Action<>(13, CommandQWOP.WO),
                new Action<>(14, CommandQWOP.NONE),
                new Action<>(11, CommandQWOP.QP),

                // 8
                new Action<>(19, CommandQWOP.NONE),
                new Action<>(17, CommandQWOP.WO),
                new Action<>(16, CommandQWOP.NONE),
                new Action<>(15, CommandQWOP.QP),

                // 9
                new Action<>(22, CommandQWOP.NONE),
                new Action<>(13, CommandQWOP.WO),
                new Action<>(8, CommandQWOP.NONE),
                new Action<>(15, CommandQWOP.QP),

                // 10
                new Action<>(13, CommandQWOP.NONE),
                new Action<>(18, CommandQWOP.WO),
                new Action<>(15, CommandQWOP.NONE),
                new Action<>(22, CommandQWOP.QP),

                // 11
                new Action<>(2, CommandQWOP.NONE),
                new Action<>(25, CommandQWOP.WO),
                new Action<>(14, CommandQWOP.NONE),
                new Action<>(15, CommandQWOP.QP)
        );
    }

    private void lyapunovExp() {

        runnerPanel = new PanelRunner_SimpleState<>("Runner");
        runnerPanel.activateTab();
        getContentPane().add(runnerPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);


        // Pick the first direction.
        float[] deviation = new float[StateQWOP.STATE_SIZE];
        deviation[2] = (float) diffSize; // Applied to an arbitrary index for now.

        for (int startTimestep = 1; startTimestep < queue.getTotalQueueLengthTimesteps() - 1; startTimestep++) {

            // Do nominal
            game.resetGame();
            queue.resetQueue();
            while (game.getTimestepsThisGame() < startTimestep) {
                game.step(queue.pollCommand());
            }

            float[] startStateScalars = game.getCurrentState().flattenState(0);
            game.step(queue.pollCommand());
            float[] endStateScalars = game.getCurrentState().flattenState(0);


            // Do deviated
            // Apply deviation to the start state.
            for (int i = 0; i < startStateScalars.length; i++) {
                startStateScalars[i] += deviation[i];
            }
            StateQWOP deviatedStartState = new StateQWOP(startStateScalars, false);

            // Get to the point of the deviation
            game.resetGame();
            queue.resetQueue();
            while (game.getTimestepsThisGame() < startTimestep) {
                game.step(queue.pollCommand());
            }
            try {
                runnerPanel.updateState(game.getCurrentState());
                Thread.sleep(200);
                game.setState(deviatedStartState);
                game.step(queue.pollCommand());
                runnerPanel.updateState(game.getCurrentState());
                Thread.sleep(200);
            } catch (Exception e){}

            float[] endStateScalarsDeviated = game.getCurrentState().flattenState(0);


            // Find the deviation magnitude
            double devMag = 0.;
            for (int i = 0; i < endStateScalars.length; i++) {
                double diff = endStateScalarsDeviated[i] - endStateScalars[i];
                devMag += diff * diff;
            }
            devMag = Math.sqrt(devMag);

            // Lyapunov exponent.
            System.out.println(Math.log(Math.abs(devMag / diffSize)));

            // Normalize deviation after timestep and make its magnitude equal to the original deviation size. Assign to
            // deviation to be used.
            for (int i = 0; i < endStateScalars.length; i++) {
                deviation[i] = (float) (diffSize * (endStateScalarsDeviated[i] - endStateScalars[i]) / devMag);
//                System.out.print(deviation[i] + " ");
            }
//            System.out.println();

        }
//        for (int i = 0; i < deviation.length; i++) {
//            System.out.println(deviation[i]);
//        }
    }

    private void incrementalJacobian() {
        for (int i = 0; i < 50; i++) {
            DoubleMatrix jac = calculateJacobian(queue, i, (float) diffSize, 1);
            ComplexDoubleMatrix eigenvalues = Eigen.eigenvalues(jac);
            DoubleMatrix realComponents = eigenvalues.real().sort();
            System.out.println(realComponents.get(realComponents.length - 1));
        }

    }


    private static DoubleMatrix calculateJacobian(ActionQueue<CommandQWOP> trajectoryCommands, int jacobianTimestep,
                                   float finiteDiffMagnitude, int timestepsIntoFuture) {
        // Fresh queue and game copies here to prevent contamination.
        ActionQueue<CommandQWOP> queue = trajectoryCommands.getCopyOfUnexecutedQueue();
        GameQWOP game = new GameQWOP();

        // Get to the place in the trajectory we're investigating.
        stepToSpecificTimestep(game, queue, jacobianTimestep);
        float[] startStateNominal = game.getCurrentState().flattenState(0);

        // Function that simulates to jacobianTimestep, applies a deviation of deviationAmmt to the state variable at
        // index deviatedStateIdx and then simulates timestepsIntoFuture. Returns the flattened state at this point.
        BiFunction<Integer, Float, float[]> simDeviation = (Integer deviatedStateIdx, Float deviationAmmt) -> {

            // Start fresh
            queue.resetQueue();
            game.resetGame();

            // Get to the place in the trajectory we're going to apply the deviation to.
            stepToSpecificTimestep(game, queue, jacobianTimestep);

            // Assign the small deviation to one of the 72 QWOP states.
            float[] startStateDeviated = Arrays.copyOf(startStateNominal, StateQWOP.STATE_SIZE);
            startStateDeviated[deviatedStateIdx] += deviationAmmt;

            // Change the game's state to the deviated one.
            game.setState(new StateQWOP(startStateDeviated, false));

            // Simulate forward to see how the deviation has propagated.
            stepToSpecificTimestep(game, queue, jacobianTimestep + timestepsIntoFuture);
            if (game.isFailed()) {
                System.out.println("failed");
            }

            return game.getCurrentState().flattenState(0);
        };

        // Jacobian matrix -- rows are each for a deviation to a different state with columns having the results when
        // simulated forward. e.g. [Dst1/Ddev1, Dst2/Ddev1, ... ; Dst1/Ddev2, Dst2/Ddev2 ....].

        DoubleMatrix jacobian = new DoubleMatrix(StateQWOP.STATE_SIZE, StateQWOP.STATE_SIZE);

        // Tweak the state by a small amount in each dimension one at a time.
        for (int stateDeviationIdx = 0; stateDeviationIdx < StateQWOP.STATE_SIZE; stateDeviationIdx++) {

            // Evaluate the results of tweaking the state by a small amount at several magnitudes for a better
            // derivative estimate.
            float[] dev_neg4 = simDeviation.apply(stateDeviationIdx, -4f * finiteDiffMagnitude);
            float[] dev_neg3 = simDeviation.apply(stateDeviationIdx, -3f * finiteDiffMagnitude);
            float[] dev_neg2 = simDeviation.apply(stateDeviationIdx, -2f * finiteDiffMagnitude);
            float[] dev_neg1 = simDeviation.apply(stateDeviationIdx, -finiteDiffMagnitude);

            float[] dev_pos1 = simDeviation.apply(stateDeviationIdx, finiteDiffMagnitude);
            float[] dev_pos2 = simDeviation.apply(stateDeviationIdx, 2f * finiteDiffMagnitude);
            float[] dev_pos3 = simDeviation.apply(stateDeviationIdx, 3f * finiteDiffMagnitude);
            float[] dev_pos4 = simDeviation.apply(stateDeviationIdx, 4f * finiteDiffMagnitude);

            // Estimate the partial derivative. Change in the rate of change of the state as a function of changing
            // one state variable. Do for each state variable to assemble the matrix of partial derivatives.
            for (int i = 0; i < StateQWOP.STATE_SIZE; i++) {
                double DStateDTweak = // Central difference, 4th order.
                        (1 / 280. * dev_neg4[i] +
                                -4 / 105. * dev_neg3[i] +
                                1 / 5. * dev_neg2[i] +
                                -4 / 5. * dev_neg1[i] +
                                4 / 5. * dev_pos1[i] +
                                -1 / 5. * dev_pos2[i] +
                                4 / 105. * dev_pos3[i] +
                                -1 / 280. * dev_pos4[i]) / finiteDiffMagnitude;
                jacobian.put(stateDeviationIdx, i, DStateDTweak);
            }
        }

        return jacobian;
    }

    private static void stepToSpecificTimestep(GameQWOP game, ActionQueue<CommandQWOP> queue, int timestep) {
        if (game.getTimestepsThisGame() > timestep) {
            throw new IllegalStateException("Can't step to timestep in the past. Probably indicates a logic error " +
                    "upstream.");
        }
        while (game.getTimestepsThisGame() < timestep) {
            game.step(queue.pollCommand());
        }
    }

    private void doJacobianStyle() {
//        runnerPanel = new PanelRunner_SimpleState<>("Runner");
//        runnerPanel.activateTab();
//        getContentPane().add(runnerPanel);
//        setPreferredSize(new Dimension(1000, 400));
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        pack();
//        setVisible(true);

        getToStart();
        StateQWOP startState = game.getCurrentState();
        float[] startStateScalars = startState.flattenState(0);
        double[][] differences = new double[StateQWOP.STATE_SIZE][StateQWOP.STATE_SIZE];

        for (int m = 1; m < 100; m++) {
            SEQUENCE_LENGTH = m;
            for (int i = 0; i < StateQWOP.STATE_SIZE; i++) {
                float[] stateMorphed_neg4 = doDeviatedInitialConditions(startStateScalars, i, -4 * diffSize);
                float[] stateMorphed_neg3 = doDeviatedInitialConditions(startStateScalars, i, -3 * diffSize);
                float[] stateMorphed_neg2 = doDeviatedInitialConditions(startStateScalars, i, -2 * diffSize);
                float[] stateMorphed_neg1 = doDeviatedInitialConditions(startStateScalars, i, -diffSize);
                float[] stateMorphed_plus1 = doDeviatedInitialConditions(startStateScalars, i, diffSize);
                float[] stateMorphed_plus2 = doDeviatedInitialConditions(startStateScalars, i, 2 * diffSize);
                float[] stateMorphed_plus3 = doDeviatedInitialConditions(startStateScalars, i, 3 * diffSize);
                float[] stateMorphed_plus4 = doDeviatedInitialConditions(startStateScalars, i, 4 * diffSize);

                for (int j = 0; j < stateMorphed_plus1.length; j++) {

                    double diff8 =
                            (1 / 280. * stateMorphed_neg4[j] +
                                    -4 / 105. * stateMorphed_neg3[j] +
                                    1 / 5. * stateMorphed_neg2[j] +
                                    -4 / 5. * stateMorphed_neg1[j] +
                                    4 / 5. * stateMorphed_plus1[j] +
                                    -1 / 5. * stateMorphed_plus2[j] +
                                    4 / 105. * stateMorphed_plus3[j] +
                                    -1 / 280. * stateMorphed_plus4[j]);

//                    double diff6 =
//                            (-1 / 60. * stateMorphed_neg3[j] +
//                                    3 / 20. * stateMorphed_neg2[j] +
//                                    -3 / 4. * stateMorphed_neg1[j] +
//                                    3 / 4. * stateMorphed_plus1[j] +
//                                    -3 / 20. * stateMorphed_plus2[j] +
//                                    1 / 60. * stateMorphed_plus3[j]);
//                    double diff4 =
//                            (1 / 12. * stateMorphed_neg2[j] +
//                                    -2 / 3. * stateMorphed_neg1[j] +
//                                    2 / 3. * stateMorphed_plus1[j] +
//                                    -1 / 12. * stateMorphed_plus2[j]);
//                    double diff2 = (-stateMorphed_neg1[j] +
//                            stateMorphed_plus1[j]);

                    differences[i][j] = diff8 / diffSize;
                }
            }
//        for (int i = 0; i < StateQWOP.STATE_SIZE; i++) {
//            for (int j = 0; j < StateQWOP.STATE_SIZE; j++) {
//                System.out.print(differences[i][j] + ", ");
//            }
//            System.out.println();
//        }

            DoubleMatrix f = new DoubleMatrix(differences);
            ComplexDoubleMatrix eigenvalues = Eigen.eigenvalues(f);
            DoubleMatrix realComponents = eigenvalues.real().sort();
            System.out.print(realComponents.get(0) + ", ");
            System.out.println(realComponents.get(realComponents.length - 1));

//        for (int i = 0; i < realComponents.rows; i++) {
//            System.out.println(realComponents.get(i));
//        }
        }


    }

    private float[] doDeviatedInitialConditions(float[] startStateScalars, int deviationIdx, double deviationSize) {
        float[] morphedStartScalars = Arrays.copyOf(startStateScalars, startStateScalars.length);
        morphedStartScalars[deviationIdx] += deviationSize;
        getToStart();
        game.setState(new StateQWOP(morphedStartScalars, false));
        getToNext();

        return game.getCurrentState().flattenState(0);
    }

    private void getToStart() {
        game.resetGame();
        queue.resetQueue();

        // Get to the cycle we want to analyze.
//        while (queue.getCurrentActionIdx() < START_ACTION) {
//            game.step(queue.pollCommand());
//        }
        while (game.getTimestepsThisGame() < START_TS) {
            game.step(queue.pollCommand());
        }
    }

    private void getToNext() {
//        while (queue.getCurrentActionIdx() < START_ACTION + SEQUENCE_LENGTH) {
//            game.step(queue.pollCommand());
//        }
        while (game.getTimestepsThisGame() < START_TS + SEQUENCE_LENGTH) {
            game.step(queue.pollCommand());
//            runnerPanel.updateState(game.getCurrentState());
//            repaint();
//            try {
//                Thread.sleep(60);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
        if (game.isFailed()) {
            System.out.println("failure");
        }
    }

    public static void main(String[] args) {
        new MAIN_LyapunovEstimate().incrementalJacobian();
    }

}
