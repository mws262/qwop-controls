package flashqwop;

import actions.Action;
import data.LoadStateStatistics;
import game.GameUnified;
import game.State;
import org.jblas.util.Random;
import tflowtools.TrainableRNN;
import tree.NodeQWOP;
import tree.Utility;
import ui.PanelRunner_MultiState;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class CompareFlashToJava extends FlashGame {
    static {
        Utility.loadLoggerConfiguration();
    }

    private GameUnified gameJava = new GameUnified();
    private PanelRunner_MultiState panelRunner;
    private boolean initialized;

    private ValueFunction_TensorFlow valueFunction = null;

    private List<GameUnified> gameUnifiedList = new ArrayList<>();

    TrainableRNN net;

    private int iteration = 0;


    LoadStateStatistics.StateStatistics stateStats;
    {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private CompareFlashToJava() {

        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(72);
        layerSizes.add(36);
        layerSizes.add(2);
        List<String> netOpts = new ArrayList<>();
        netOpts.add("--learnrate");
        netOpts.add("1e-3");

        try {
            net = TrainableRNN.makeNewNetwork("corrector", layerSizes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        net.loadCheckpoint("correctorchk" + iteration);

        loadController();

        getControlAction(GameUnified.getInitialState()); // TODO make this better. The first controller evaluation ever
        // takes 8 times longer than the rest.

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        restart();

        gameUnifiedList.add(gameJava);

        panelRunner = new PanelRunner_MultiState();
        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(panelRunner);
        frame.setPreferredSize(new Dimension(1280, 800));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//        frame.setUndecorated(true);
//        frame.setBackground(new Color(1,1,1,.0f));
//        frame.setOpacity(0.5f);
        frame.setLocation(2279,299);
        panelRunner.runnerScaling = 19.15f * 2f;
        panelRunner.mainRunnerColor = Color.BLACK;
        panelRunner.xOffsetPixels = panelRunner.xOffsetPixels + 45;
        panelRunner.yOffsetPixels = panelRunner.yOffsetPixels + 222;

        panelRunner.customStroke = new BasicStroke(.2f);
        panelRunner.customStrokeExtra = new BasicStroke(3f);
//        frame.getContentPane().setBackground(new Color(1,1,1,.2f));
       //panelRunner.setBackground(new Color(1,1,1,.0f));
        ComponentResizer cr = new ComponentResizer();
        cr.registerComponent(frame);
        cr.setSnapSize(new Dimension(10, 10));
        cr.setMaximumSize(new Dimension(2000,2000));
        cr.setMinimumSize(new Dimension(100,100));
        frame.pack();
        frame.setVisible(true);
        new Thread(panelRunner);
        panelRunner.activateTab();
        initialized = true;
        restart();

    }

    @Override
    public Action[] getActionSequenceFromBeginning() {
        return new Action[]{
                new Action(5, Action.Keys.none),
        };
    }

    @Override
    public Action getControlAction(State state) {

//        if (((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult != null) {
//            float bodyErrorX =
//                    state.body.getX() - ((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult.state.body.getX();
//            float bodyErrorY =
//                    state.body.getY() - ((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult.state.body.getY();
//
//            System.out.println(bodyErrorX + ", " + bodyErrorY);
//        }
        gameJava.makeNewWorld();
        gameJava.setState(state);
        gameJava.iterations = 20;


        Action action =  valueFunction.getMaximizingAction(new NodeQWOP(state));

        // perturb for variety.
        float rnum = Random.nextFloat();
        if (rnum > 0.9f) {
            action = new Action(Integer.max(1, action.getTimestepsTotal() - 1), action.peek());
        } else if (rnum > 0.8f) {
            action = new Action(action.getTimestepsTotal() + 1, action.peek());
        }
        return action;
    }

    State prevState;
    List<State> stateList = new ArrayList<>();
    List<float[]> errorList = new ArrayList<>();
    @Override
    public void reportGameStatus(State state, boolean[] command, int timestep) {
        if (!initialized) {
            return;
        }

        if (timestep == 0) {
            if (stateList.size() > 0) {
                train();
            }
            prevState = state;
            gameJava.makeNewWorld();
//            gameJava.makeNewWorld();
//            gameJava.iterations = 15;
//            gameUnifiedList.clear();
//            gameUnifiedList.add(gameJava);
            stateList.clear();
            errorList.clear();

        } else {

            gameJava.makeNewWorld();
            gameJava.iterations = 15;
            gameJava.setState(prevState);
            gameJava.step(command);

            float bodyErrorX =
                    state.body.getX() - gameJava.getCurrentState().body.getX();
            float bodyErrorY =
                    state.body.getY() - gameJava.getCurrentState().body.getY();

//            float[][] eval = new float[][]{stateStats.rescaleState(prevState)};
//            float[][] result = net.evaluateInput(eval);
//            System.out.println(bodyErrorX + ", " + bodyErrorY + ", " + result[0][0] + ", " + result[0][1]);

            stateList.add(prevState);
            errorList.add(new float[]{bodyErrorX, bodyErrorY});

            prevState = state;

        }
    }

    private void train() {
        assert stateList.size() == errorList.size();

        float[][] states = new float[stateList.size()][72];
        float[][] error = new float[errorList.size()][2];
        for (int i = 0; i < stateList.size(); i++) {
            states[i] = stateStats.rescaleState(stateList.get(i));
            error[i] = errorList.get(i);
        }

        float loss = net.trainingStep(states, error, 1);
        System.out.println("Loss: " + loss);
        net.saveCheckpoint("correctorchk" + ++iteration);
    }

    private void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/small_net.pb")); // state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("small289"); // _after439");//273");//chk_after1");
    }

    public static void main(String[] args) {
        new CompareFlashToJava();
    }
}
