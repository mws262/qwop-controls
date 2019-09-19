package flashqwop;

import game.IGameSerializable;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.IStateQWOP;
import game.qwop.StateQWOP;
import game.state.transform.Transform_Identity;
import tree.node.NodeGame;
import ui.runner.PanelRunner_MultiState;
import value.ValueFunction_TensorFlow_StateOnly;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompareFlashToJava extends FlashGame {
    private GameQWOP gameJava = new GameQWOP();
    private PanelRunner_MultiState panelRunner;
    private boolean initialized;

    private ValueFunction_TensorFlow_StateOnly valueFunction = null;

    private List<GameQWOP> gameQWOPList = new ArrayList<>();

    private CompareFlashToJava() {
        loadController();

        getControlAction(GameQWOP.getInitialState()); // TODO make this better. The first controller evaluation ever
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

        gameQWOPList.add(gameJava);

        panelRunner = new PanelRunner_MultiState("Runners");
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
        panelRunner.activateTab();
        initialized = true;
        restart();

    }

    @Override
    public List<Action<CommandQWOP>> getActionSequenceFromBeginning() {
        List<Action<CommandQWOP>> commandList = new ArrayList<>();
        commandList.add(new Action<>(5, CommandQWOP.NONE));
        return commandList;
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
    }

    @Override
    public Action<CommandQWOP> getControlAction(IStateQWOP state) {
        return valueFunction.getMaximizingAction(new NodeGame<>(state));
    }

    @Override
    public void reportGameStatus(IStateQWOP state, CommandQWOP command, int timestep) {
        if (!initialized) {
            return; // This
        }

        if (timestep == 0) {
            gameJava.resetGame();
            gameJava.setPhysicsIterations(15);
            gameQWOPList.clear();
            gameQWOPList.add(gameJava);

        } else {
            int tp = 0;
            if (timestep < tp + 5 && timestep > tp)
                    gameJava.setPhysicsIterations(5);

//            if (timestep % 160 == 0) {
//                GameQWOP newGame = new GameQWOP();
//                newGame.setState(state);
//                gameQWOPList.add(newGame);
//            }
//            //            gameJava.fullStatePDController(state);
//            panelRunner.clearSecondaryStates();
//            int idx = 0;
//            for (GameQWOP game : gameQWOPList) {
//                game.step(command);
//                panelRunner.addSecondaryState(game.getCurrentState(), Node.getColorFromTreeDepth(idx++));
//
//            }

//            panelRunner.clearSecondaryStates();
//            panelRunner.addSecondaryState(((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult.state,
//                    NodeGameGraphicsBase.getColorFromScaledValue(((ValueFunction_TensorFlow_StateOnly) valueFunction).currentResult.value, 40f, 0.65f));

            panelRunner.setMainState(state.getPositionCoordinates());
            panelRunner.repaint();
        }
    }

    private void loadController() {
        // Load a value function controller.
        try {
            // TODO not type safe
            IGameSerializable<CommandQWOP, StateQWOP> game =  new GameQWOP();
            valueFunction = new ValueFunction_TensorFlow_StateOnly<>(
                    new File("src/main/resources/tflow_models" +
                    "/small_net.pb"),
                    game,
                    new Transform_Identity<>(),
                    1f,
                    false); // state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            valueFunction.loadCheckpoint("small289"); // _after439");//273");//chk_after1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new CompareFlashToJava();
    }
}
