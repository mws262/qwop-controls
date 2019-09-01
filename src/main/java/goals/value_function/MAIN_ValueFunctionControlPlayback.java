package goals.value_function;

import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import game.state.transform.ITransform;
import ui.runner.PanelRunner_ControlledTFlow;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;

public class MAIN_ValueFunctionControlPlayback {

    //GameQWOPCaching.StateType.HIGHER_DIFFERENCES);
            private MAIN_ValueFunctionControlPlayback() {

        JFrame jFrame = new JFrame();

        ITransform<StateQWOP> stateNormalizer = null;
        try {
            stateNormalizer = new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.STDEV);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

                //new GameQWOPCaching(1,2,
                GameQWOP game = new GameQWOP();
                PanelRunner_ControlledTFlow<StateQWOP> controlPanel = new PanelRunner_ControlledTFlow<>(
                "Controlled runner",
                        game,
                stateNormalizer,
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
