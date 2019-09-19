package goals.value_function;

import game.qwop.GameQWOPCaching;
import game.qwop.StateQWOPDelayEmbedded_HigherDifferences;
import game.state.transform.ITransform;
import ui.runner.PanelRunner_ControlledTFlow;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;

public class MAIN_ValueFunctionControlPlayback {

    //GameQWOPCaching.StateType.HIGHER_DIFFERENCES);
    private MAIN_ValueFunctionControlPlayback() {

        JFrame jFrame = new JFrame();

//        ITransform<StateQWOP> stateNormalizer = null;
//        try {
//            stateNormalizer = new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.STDEV);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        // TODO have a sensible way of substituting out game types.
        //new GameQWOPCaching(1,2,
//                GameQWOP game = new GameQWOP();
        GameQWOPCaching<StateQWOPDelayEmbedded_HigherDifferences> game
                = new GameQWOPCaching<>(1,2, GameQWOPCaching.StateType.HIGHER_DIFFERENCES);
        ITransform<StateQWOPDelayEmbedded_HigherDifferences> stateNormalizer = null;
        try {
            stateNormalizer =
                    new StateQWOPDelayEmbedded_HigherDifferences.Normalizer(StateQWOPDelayEmbedded_HigherDifferences.Normalizer.NormalizationMethod.STDEV);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PanelRunner_ControlledTFlow<StateQWOPDelayEmbedded_HigherDifferences> controlPanel = new PanelRunner_ControlledTFlow<>(
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
