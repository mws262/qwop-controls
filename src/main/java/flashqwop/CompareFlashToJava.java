package flashqwop;

import actions.Action;
import game.GameUnified;
import game.IState;
import tree.NodeQWOP;
import tree.NodeQWOPGraphicsBase;
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
    private GameUnified gameJava = new GameUnified();
    private PanelRunner_MultiState panelRunner;
    private boolean initialized;

    private ValueFunction_TensorFlow valueFunction = null;

    private List<GameUnified> gameUnifiedList = new ArrayList<>();

    private CompareFlashToJava() {
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
        panelRunner.mainRunnerColor = Color.YELLOW;
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
    }

    @Override
    public Action getControlAction(IState state) {
        return valueFunction.getMaximizingAction(new NodeQWOP(state));
    }

    @Override
    public void reportGameStatus(IState state, boolean[] command, int timestep) {
        if (!initialized) {
            return; // This
        }

        if (timestep == 0) {
            gameJava.makeNewWorld();
            gameJava.iterations = 15;
            gameUnifiedList.clear();
            gameUnifiedList.add(gameJava);

        } else {
            int tp = 0;
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

            panelRunner.clearSecondaryStates();
            panelRunner.addSecondaryState(((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult.state,
                    NodeQWOPGraphicsBase.getColorFromScaledValue(((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult.value, 40f, 0.65f));

            panelRunner.setMainState(state);
            panelRunner.repaint();
        }
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
