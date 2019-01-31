package goals.deep_rl;

import game.GameSingleThread;
import game.State;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.network.dqn.DQN;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.util.DataManager;
import org.jblas.util.Random;
import org.json.JSONObject;
import org.nd4j.linalg.learning.config.Adam;
import tree.Node;
import ui.PanelRunner_MultiState;
import ui.PanelTimeSeries;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;

public class TestDeepRL implements MDP<QWOPStateRL, Integer, DiscreteSpace> {

    public static QLearning.QLConfiguration QWOP_QL =
            new QLearning.QLConfiguration(
                    123,    //Random seed
                    200,    //Max step By epoch
                    15000000, //Max step
                    150000, //Max size of experience replay
                    32,     //size of batches
                    500,    //target update (hard)
                    0,     //num step noop warmup
                    0.1,   //reward scaling
                    0.95,   //gamma
                    1.0,    //td-error clipping
                    0.1f,   //min epsilon
                    1000,   //num step for eps greedy anneal
                    true    //double DQN
            );

    public static DQNFactoryStdDense.Configuration QWOP_NET =
            DQNFactoryStdDense.Configuration.builder()
                    .l2(0.0000).updater(new Adam(0.0001)).numHiddenNodes(16).numLayer(3).build();


    private ObservationSpace<QWOPStateRL> observationSpace = new ArrayObservationSpace(new int[] {72});
    private DiscreteSpace actionSpace = new DiscreteSpace(3);

    private GameSingleThread game = new GameSingleThread();
    private static PanelRunner_MultiState runnerPanel = new PanelRunner_MultiState();
    private static PanelTimeSeries progressPlot = new PanelTimeSeries(1) {
        @Override
        public void update(Node plotNode) {
        }
    };

    @Override
    public ObservationSpace<QWOPStateRL> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }

    @Override
    public QWOPStateRL reset() {
        State currentState = game.getCurrentState();
        progressPlot.addToSeries( currentState.body.getX() - 0.1f*currentState.body.getY() + 0.1f*currentState.body.getDx(),0,
                0);
        progressPlot.applyUpdates();
        runnerPanel.clearSecondaryStates();
        game.makeNewWorld();
        return new QWOPStateRL(game.getCurrentState());
    }

    @Override
    public void close() {}

    @Override
    public StepReply<QWOPStateRL> step(Integer actionIndex) {

        switch(actionIndex) {
            case 0:
                game.stepGame(false, false, false, false);
                break;
            case 1:
                game.stepGame(false, true, true, false);
                break;
            case 2:
                game.stepGame(true, false, false, true);
                break;
//            case 0:
//                game.stepGame(false, false, false, false);
//                break;
//            case 1:
//                game.stepGame(true, false, false, false);
//                break;
//            case 2:
//                game.stepGame(false, true, false, false);
//                break;
//            case 3:
//                game.stepGame(false, false, true, false);
//                break;
//            case 4:
//                game.stepGame(false, false, false, true);
//                break;
//            case 5:
//                game.stepGame(true, false, true, false);
//                break;
//            case 6:
//                game.stepGame(true, false, false, true);
//                break;
//            case 7:
//                game.stepGame(false, true, true, false);
//                break;
//            case 8:
//                game.stepGame(false, true, false, true);
//                break;
            default:
                throw new IllegalArgumentException("Bad action index: " + actionIndex);
        }
        State currentState = game.getCurrentState();
        runnerPanel.addSecondaryState(currentState, Color.BLUE);
        runnerPanel.repaint();
//        game.applyBodyTorque(8500f*(GameSingleThread.getInitialState().body.getTh() - currentState.body.getTh() - 0.2f)
//                - 850f * currentState.body.getDth());
        game.applyBodyImpulse(0.01f*(Random.nextFloat() - 0.5f), 0.01f*(Random.nextFloat() - 0.5f));

        return new StepReply<>(new QWOPStateRL(currentState),
                currentState.body.getX() - 0.1f*currentState.body.getY() + 0.1f*currentState.body.getDx(),
                isDone(),
                new JSONObject(
                "{}"));
    }

    @Override
    public boolean isDone() {
        return game.getFailureStatus();
    }

    @Override
    public MDP<QWOPStateRL, Integer, DiscreteSpace> newInstance() {
        return new TestDeepRL();
    }

    public static void main(String[] args) throws IOException {
        //record the training data in rl4j-data in a new folder (save)
        DataManager manager = new DataManager(true);
        //BasicConfigurator.configure();

        JFrame frame = new JFrame();
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        runnerPanel.activateTab();
        runnerPanel.setMainState(GameSingleThread.getInitialState());
        progressPlot.activateTab();
        progressPlot.updateRange(-20, 100);
        frame.getContentPane().add(runnerPanel);
        frame.getContentPane().add(progressPlot);
        frame.setPreferredSize(new Dimension(1000, 1500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        //define the training
//        QLearningDiscreteDense<Box> dql = new QLearningDiscreteDense(new TestDeepRL(), DQN.load("/tmp/testb"), QWOP_QL
//                , manager);
        QLearningDiscreteDense<Box> dql =new
         QLearningDiscreteDense
         (new
         TestDeepRL(),
         QWOP_NET,
         QWOP_QL, manager);

        Logger.getAnonymousLogger().info("Starting training");

        java.lang.Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                //get the final policy
                DQNPolicy<Box> pol = dql.getPolicy();

                //serialize and save (serialization showcase, but not required)
                try {
                    pol.save("/tmp/pol3");
                    dql.getCurrentDQN().save("/tmp/testb");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Shutdown hook ran!");
            }
        });
        //train
        dql.train();

        //get the final policy
        DQNPolicy<Box> pol = dql.getPolicy();

        //serialize and save (serialization showcase, but not required)
        pol.save("/tmp/pol2");

//        //close the mdp (close http)*
//        mdp.close();
    }
}


//public class SimpleToy implements MDP<SimpleToyState, Integer, DiscreteSpace> {
//
//    final private int maxStep;
//    //TODO 10 steps toy (always +1 reward2 actions), toylong (1000 steps), toyhard (7 actions, +1 only if actiion = (step/100+step)%7, and toyStoch (like last but reward has 0.10 odd to be somewhere else).
//    @Getter
//    private DiscreteSpace actionSpace = new DiscreteSpace(2);
//    @Getter
//    private ObservationSpace<SimpleToyState> observationSpace = new ArrayObservationSpace(new int[] {1});
//    private SimpleToyState simpleToyState;
//    @Setter
//    private NeuralNetFetchable<IDQN> fetchable;
//
//    public SimpleToy(int maxStep) {
//        this.maxStep = maxStep;
//    }
//
//    public void printTest(int maxStep) {
//        INDArray input = Nd4j.create(maxStep, 1);
//        for (int i = 0; i < maxStep; i++) {
//            input.putRow(i, Nd4j.create(new SimpleToyState(i, i).toArray()));
//        }
//        INDArray output = fetchable.getNeuralNet().output(input);
//        log.info(output.toString());
//    }
//
//    public void close() {}
//
//    @Override
//    public boolean isDone() {
//        return simpleToyState.getStep() == maxStep;
//    }
//
//    public SimpleToyState reset() {
//        if (fetchable != null)
//            printTest(maxStep);
//
//        return simpleToyState = new SimpleToyState(0, 0);
//    }
//
//    public StepReply<SimpleToyState> step(Integer a) {
//        double reward = (simpleToyState.getStep() % 2 == 0) ? 1 - a : a;
//        simpleToyState = new SimpleToyState(simpleToyState.getI() + 1, simpleToyState.getStep() + 1);
//        return new StepReply<>(simpleToyState, reward, isDone(), new JSONObject("{}"));
//    }
//
//    public SimpleToy newInstance() {
//        SimpleToy simpleToy = new SimpleToy(maxStep);
//        simpleToy.setFetchable(fetchable);
//        return simpleToy;
//    }
//
//}
