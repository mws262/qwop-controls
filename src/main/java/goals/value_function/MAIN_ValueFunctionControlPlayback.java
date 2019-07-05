package goals.value_function;

import game.GameUnified;
import ui.runner.PanelRunner_ControlledTFlow;

import javax.swing.*;
import java.awt.*;

public class MAIN_ValueFunctionControlPlayback {


    public MAIN_ValueFunctionControlPlayback() {

        JFrame jFrame = new JFrame();

        PanelRunner_ControlledTFlow<GameUnified> controlPanel = new PanelRunner_ControlledTFlow<>("Controlled runner"
                , new GameUnified());

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

        MAIN_ValueFunctionControlPlayback playback = new MAIN_ValueFunctionControlPlayback();


    }
}
