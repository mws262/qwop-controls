package flashqwop;

import actions.Action;
import actions.ActionGenerator_FixedActions;
import actions.ActionSet;
import game.GameUnified;
import game.State;
import goals.cold_start_analysis.CompareWarmStartToColdBase;
import tree.Node;
import tree.Utility;
import ui.PanelRunner;
import ui.PanelRunner_MultiState;
import ui.PanelRunner_SimpleState;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class CompareFlashToJava extends FlashGame {
    GameUnified gameJava = new GameUnified();
    PanelRunner_MultiState panelRunner;
    private boolean initialized = false;
    private ActionSet bunchOfActions = ActionSet.makeExhaustiveActionSet(14, 35);

    private ValueFunction_TensorFlow valueFunction = null;
    private Node placeholderNode = new Node(); // TODO only really needs the state. This is just acting as a container.

    public CompareFlashToJava() {

        loadController();

        getControlAction(GameUnified.getInitialState()); // TODO make this better. The first controller evaluation ever
        // takes 8 times longer than the rest. I don't know why. In the meantime, just do the first evaluation in a
        // non-time-critical section of the code. In the long term, the controller should be an anytime approach
        // anyway.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        printGameInfo();
        restart();


        gameUnifiedList.add(gameJava);

        panelRunner = new PanelRunner_MultiState();
        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(panelRunner);
        frame.setPreferredSize(new Dimension(1280, 800));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setUndecorated(true);
        frame.setBackground(new Color(1,1,1,.0f));
        frame.setOpacity(0.5f);
        frame.setLocation(2279,299);
        panelRunner.runnerScaling = 19.15f * 2f;
        panelRunner.mainRunnerColor = Color.BLACK;
        panelRunner.xOffsetPixels = panelRunner.xOffsetPixels + 45;
        panelRunner.yOffsetPixels = panelRunner.yOffsetPixels + 222;

        panelRunner.customStroke = new BasicStroke(3f);
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
        Thread panelThread = new Thread(panelRunner);
        //panelThread.start();
        panelRunner.activateTab();
        initialized = true;
        restart();

    }

    @Override
    public Action[] getActionSequenceFromBeginning() {
        Action[] prefix = new Action[]{
                new Action(7, Action.Keys.none),
//                new Action(49, Action.Keys.wo),
//                new Action(20, Action.Keys.qp),
//                new Action(1, Action.Keys.p),
//                new Action(17, Action.Keys.qp),
//                new Action(3, Action.Keys.w),
//                new Action(3, Action.Keys.p),
//                new Action(1, Action.Keys.none),
//                new Action(15, Action.Keys.wo),



//                new Action(4, Action.Keys.w),
//                new Action(17, Action.Keys.qp),
//                new Action(4, Action.Keys.p),
//                new Action(7, Action.Keys.none),
//                new Action(1, Action.Keys.p)
        };
        return prefix;
    }

    @Override
    public Action getControlAction(State state) {

        placeholderNode.setState(state);
//        return valueFunction.getMaximizingAction(placeholderNode);
        return bunchOfActions.getRandom();
    }

    int tp = 0;
    List<GameUnified> gameUnifiedList = new ArrayList();
    @Override
    public void reportGameStatus(State state, boolean[] command, int timestep) {
        if (!initialized) {
            return; // This
        }

        if (timestep == 0) {
            gameJava.makeNewWorld();
            gameJava.iterations = 15;
            gameUnifiedList.clear();
            gameUnifiedList.add(gameJava);

        } else {
            if (timestep < tp + 5 && timestep > tp)
                    gameJava.iterations = 5;

//            if (timestep % 160 == 0) {
//                GameUnified newGame = new GameUnified();
//                newGame.setState(state);
//                gameUnifiedList.add(newGame);
//            }
//            //            gameJava.fullStatePDController(state);
//            panelRunner.clearSecondaryStates();
//            int idx = 0;
//            for (GameUnified game : gameUnifiedList) {
//                game.step(command);
//                panelRunner.addSecondaryState(game.getCurrentState(), Node.getColorFromTreeDepth(idx++));
//
//            }

            panelRunner.setMainState(state); // gameJava.getCurrentState());
            panelRunner.repaint();
        }
    }

    public void loadController() {
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
        CompareFlashToJava comparison = new CompareFlashToJava();
    }
}
