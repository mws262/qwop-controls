package goals.value_function;

import game.qwop.GameQWOP;
import ui.runner.PanelRunner_ControlledTFlow;

import javax.swing.*;
import java.awt.*;

public class MAIN_ValueFunctionControlPlayback {

    private GameQWOP game = new GameQWOP(); //new GameQWOPCaching(1,2,
            //GameQWOPCaching.StateType.HIGHER_DIFFERENCES);
    public MAIN_ValueFunctionControlPlayback() {

        JFrame jFrame = new JFrame();

        PanelRunner_ControlledTFlow controlPanel = new PanelRunner_ControlledTFlow("Controlled runner",
                game,
                "src/main/resources/tflow_models",
                "src/main/resources/tflow_models/checkpoints");

        jFrame.setPreferredSize(new Dimension(1000, 1000));
        jFrame.add(controlPanel);
        controlPanel.activateTab();
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);

        Timer drawTimer = new Timer(30, e -> jFrame.repaint());
        drawTimer.start();
    }

    public static void main(String[] args) {
        new MAIN_ValueFunctionControlPlayback();
    }
}
