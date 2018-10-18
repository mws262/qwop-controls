package goals;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;

import org.tensorflow.example.FeatureList;
import org.tensorflow.example.SequenceExample;

import data.TFRecordReader;
import game.GameLoader;
import game.State;
import game.StateVariable;
import tree.Node;
import tree.Utility;
import ui.PanelRunner_AnimatedFromStates;

/**
 * Playback runs or sections of runs saved in SavableSingleRun files.
 *
 * @author matt
 */

@SuppressWarnings("serial")
public class MAIN_PlaybackSaved_TFRecord extends JFrame {

    public GameLoader game;
    private PanelRunner_AnimatedFromStates runnerPane;

    /**
     * Window width
     **/
    public static int windowWidth = 1920;

    /**
     * Window height
     **/
    public static int windowHeight = 1000;


    private File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");

    List<Node> leafNodes = new ArrayList<>();

    public static void main(String[] args) {
        MAIN_PlaybackSaved_TFRecord mc = new MAIN_PlaybackSaved_TFRecord();
        mc.setup();
        mc.run();
    }

    public void setup() {
        /* Runner pane */
        runnerPane = new PanelRunner_AnimatedFromStates();
        runnerPane.activateTab();
        runnerPane.yOffsetPixels = 600;
        this.add(runnerPane);
        Thread runnerThread = new Thread(runnerPane);
        runnerThread.start();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setPreferredSize(new Dimension(windowWidth, windowHeight));
        this.setContentPane(this.getContentPane());
        this.pack();
        this.setVisible(true);
        repaint();
    }

    public void run() {
        File[] allFiles = saveLoc.listFiles();
        if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

        List<File> playbackFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().contains("TFRecord") && f.getName().contains("denseTF_2018-05-11_13-35-18.TFRecord")) {
                playbackFiles.add(f);
            }
        }

        Collections.shuffle(playbackFiles);

        // Validate -- not needed during batch run.
        List<SequenceExample> dataSeries = new ArrayList<>();
        String fileName = playbackFiles.get(0).getAbsolutePath();
        System.out.println(fileName);

        try (FileInputStream fIn = new FileInputStream(fileName); DataInputStream dIn = new DataInputStream(fIn)){
            TFRecordReader tfReader = new TFRecordReader(dIn, true);
            while (fIn.available() > 0) {
                dataSeries.add(SequenceExample.parser().parseFrom(tfReader.read()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Read " + dataSeries.size() + " runs from file " + fileName);
        for (SequenceExample seq : dataSeries) {
            int totalTimestepsInRun = seq.getFeatureLists().getFeatureListMap().get("BODY").getFeatureCount();
            State[] stateVars = new State[totalTimestepsInRun];

            for (int i = 0; i < totalTimestepsInRun; i++) {
                // Unpack each x y th... value in a given timestep. Turn them into StateVariables.
                Map<String, FeatureList> featureListMap = seq.getFeatureLists().getFeatureListMap();
                StateVariable[] sVarBuffer = new StateVariable[State.ObjectName.values().length];
                int idx = 0;
                for (State.ObjectName bodyPart : State.ObjectName.values()) {
                    List<Float> sValList =
							featureListMap.get(bodyPart.toString()).getFeature(i).getFloatList().getValueList();

                    sVarBuffer[idx] = new StateVariable(sValList);
                    idx++;
                }

                // Turn the StateVariables into a single State for this timestep.
                stateVars[i] = new State(sVarBuffer[0], sVarBuffer[1], sVarBuffer[2], sVarBuffer[3], sVarBuffer[4],
                        sVarBuffer[5], sVarBuffer[6], sVarBuffer[7], sVarBuffer[8], sVarBuffer[9], sVarBuffer[10], sVarBuffer[11], false);
            }

            runnerPane.simRun(new LinkedList<>(Arrays.asList(stateVars)));

            while (!runnerPane.isFinishedWithRun()) {
                runnerPane.repaint();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
