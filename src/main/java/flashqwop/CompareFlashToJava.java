package flashqwop;

import actions.Action;
import game.GameUnified;
import game.State;
import goals.cold_start_analysis.CompareWarmStartToColdBase;
import ui.PanelRunner;
import ui.PanelRunner_MultiState;
import ui.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;

public class CompareFlashToJava extends FlashGame {
    GameUnified gameJava = new GameUnified();
    PanelRunner_MultiState panelRunner;
    private boolean initialized = false;

    public CompareFlashToJava() {
        panelRunner = new PanelRunner_MultiState();
        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(panelRunner);
        frame.setPreferredSize(new Dimension(800, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        Thread panelThread = new Thread(panelRunner);
        panelThread.start();
        panelRunner.activateTab();
        initialized = true;

    }

    @Override
    public Action[] getActionSequenceFromBeginning() {
        Action[] prefix = new Action[]{
                new Action(7, Action.Keys.none),
                new Action(49, Action.Keys.wo),
                new Action(20, Action.Keys.qp),
                new Action(1, Action.Keys.p),
                new Action(17, Action.Keys.qp),
                new Action(3, Action.Keys.w),
                new Action(3, Action.Keys.p),
                new Action(1, Action.Keys.none),
                new Action(15, Action.Keys.wo),
                new Action(4, Action.Keys.w),
                new Action(17, Action.Keys.qp),
                new Action(4, Action.Keys.p),
                new Action(7, Action.Keys.none),
                new Action(1, Action.Keys.p)
        };
        return prefix;
    }

    @Override
    public Action getControlAction(State state) {
        return null;
    }
    int tp = 30;
    @Override
    public void reportGameStatus(State state, boolean[] command, int timestep) {
        if (!initialized) {
            return; // This
        }

        if (timestep == 0) {
            gameJava.makeNewWorld();
            gameJava.iterations = 50;
            tp += 10;
        }else if (timestep == tp) {
            gameJava.setState(state);


//            gameJava.applyBodyImpulse(-150, 0f);
        } else {
            if (timestep < tp + 5 && timestep > tp)
                    gameJava.iterations = 5;
            gameJava.step(command);
//            gameJava.fullStatePDController(state);
            panelRunner.clearSecondaryStates();
            panelRunner.addSecondaryState(state, Color.BLUE);
            panelRunner.setMainState(gameJava.getCurrentState());
        }
    }

    public static void main(String[] args) {
        CompareFlashToJava comparison = new CompareFlashToJava();
    }
}
