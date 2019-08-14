package goals.playback;

import data.TFRecordDataParsers;
import game.GameUnified;
import game.IGameInternal;
import game.action.Action;
import game.action.ActionQueue;
import game.state.State;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.example.SequenceExample;
import ui.runner.PanelRunner_MultiState;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Playback runs or sections of runs saved densely in TFRecord files. This draws three things: state data drawn over
 * time, Actions simulated (from timestep duration, keys data), and commands simulated (keys every timestep). This is
 * partially to make sure that all the data sources from a run match up.
 *
 * @author matt
 */
public class MAIN_PlaybackSaved_TFRecord extends JFrame {
    /**
     * Visual panel for animating the runner.
     */
    private PanelRunner_MultiState runnerPane;

    /**
     * Directory containing the TFRecord files of runs to replay.
     * Can also be a single TFRecord file.
     */
    private File saveLoc = new File("src/main/resources/saved_data/training_data/denseTF_2018-04-26_15-19-44.TFRecord");

    private static final Logger logger = LogManager.getLogger(MAIN_PlaybackSaved_TFRecord.class);

    public static void main(String[] args) {
        MAIN_PlaybackSaved_TFRecord mc = new MAIN_PlaybackSaved_TFRecord();
        mc.setup();
        mc.run();
    }

    public void setup() {
        runnerPane = new PanelRunner_MultiState("Runners");
        runnerPane.activateTab();
        add(runnerPane);
        Thread runnerThread = new Thread(runnerPane);
        runnerThread.start();

        setTitle("TFRecord run playback");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 400));
        pack();
        setVisible(true);
        repaint();
    }

    public void run() {
        File[] allFiles;
        if (saveLoc.isDirectory()) {
            allFiles = saveLoc.listFiles();
        } else {
            allFiles = new File[]{saveLoc};
        }
        Objects.requireNonNull(allFiles); // Null means that the directory was bad.

        // Look for TFRecord files.
        List<File> playbackFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().toLowerCase().contains("tfrecord")) {
                playbackFiles.add(f);
            }
        }

        if (playbackFiles.isEmpty()) {
            throw new IndexOutOfBoundsException("No TFRecord files found in this directory.");
        }
        logger.info("Number of TFRecord files found: " + playbackFiles.size() + ".");

        Collections.shuffle(playbackFiles);

        IGameInternal gameForActionSim = new GameUnified();
        IGameInternal gameForCommandSim = new GameUnified();

        // Load files one at a time.
        for (File tfrecordFile : playbackFiles) {
            // Read all the sequences from a file.
            List<SequenceExample> dataSeries = null;
            try {
                dataSeries = TFRecordDataParsers.loadSequencesFromTFRecord(tfrecordFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Objects.requireNonNull(dataSeries, "TFRecord files were not successfully loaded.");

            logger.info("Read " + dataSeries.size() + " runs from file " + tfrecordFile + ".");
            Collections.shuffle(dataSeries);

            // Playback each sequence one at a time.
            for (SequenceExample seq : dataSeries) {
                // Pull the states out of the Protobuf-like structure.
                State[] stateVars = TFRecordDataParsers.getStatesFromLoadedSequence(seq);
                List<Action> actions = TFRecordDataParsers.getActionsFromLoadedSequence(seq);
                boolean[][] commands = TFRecordDataParsers.getCommandSequenceFromLoadedSequence(seq);

                actions.remove(0);
                actions.remove(0);

                ActionQueue actionQueue = new ActionQueue();
                actionQueue.addSequence(actions);

                gameForActionSim.makeNewWorld();
                gameForCommandSim.makeNewWorld();

                for (int i = 0; i < stateVars.length - 1; i++) {
                    runnerPane.clearSecondaryStates();
                    runnerPane.setMainState(stateVars[i]);
                    runnerPane.addSecondaryState(gameForActionSim.getCurrentState(), Color.RED);
                    runnerPane.addSecondaryState(gameForCommandSim.getCurrentState(), Color.BLUE);
                    if (actionQueue.isEmpty()) {
                        logger.warn("Game.action ended before states did.");
                    } else {
                        boolean[] actionQueueCommand = actionQueue.pollCommand();
                        gameForActionSim.step(actionQueueCommand);
                        gameForCommandSim.step(commands[i]);
                    }
//                    if (!Arrays.equals(actionQueueCommand, commands[i])) {
//                        throw new RuntimeException("Commands taken from Action and boolean sources of the TFRecord do" +
//                                " not match. Issue happened at action index: " + actionQueue.getCurrentActionIdx() +
//                                ", and timestep: " + i + ". Queue says: " + actionQueueCommand[0] + "," +
//                                actionQueueCommand[1] + "," + actionQueueCommand[2] + "," + actionQueueCommand[3] +
//                                "; commands say " + commands[i][0] + ", " + commands[i][1] + ", " + commands[i][2] +
//                                ", " + commands[i][3]);
//                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
