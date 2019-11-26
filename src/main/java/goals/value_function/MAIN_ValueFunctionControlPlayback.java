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

        GameQWOP game = new GameQWOP();
        ITransform<StateQWOP> stateNormalizer = null;
        try {
            stateNormalizer = new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.RANGE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // TODO have a sensible way of substituting out game types.
        //new GameQWOPCaching(1,2,
//                GameQWOP game = new GameQWOP();
//        GameQWOPCaching<StateQWOPDelayEmbedded_Differences> game
//                = new GameQWOPCaching<>(1,1, GameQWOPCaching.StateType.DIFFERENCES);
//        ITransform<StateQWOPDelayEmbedded_Differences> stateNormalizer = null;
//        try {
//            stateNormalizer =
//                    new StateQWOPDelayEmbedded_Differences.Normalizer(StateQWOPDelayEmbedded_Differences.Normalizer.NormalizationMethod.RANGE);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

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
