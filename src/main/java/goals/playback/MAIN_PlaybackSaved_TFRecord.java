package goals.playback;

import java.awt.Dimension;
import java.io.*;
import java.util.*;
import javax.swing.JFrame;

import data.TFRecordDataParsers;
import org.tensorflow.example.SequenceExample;

import game.State;
import ui.PanelRunner_AnimatedFromStates;

/**
 * Playback runs or sections of runs saved densely in TFRecord files. These are simply played-back states.
 * Re-simulation is not done.
 *
 * @author matt
 */

@SuppressWarnings("serial")
public class MAIN_PlaybackSaved_TFRecord extends JFrame {
    /**
     * Visual panel for animating the runner.
     */
    private PanelRunner_AnimatedFromStates runnerPane;

    /**
     * Window width.
     */
    public static int windowWidth = 1920;

    /**
     * Window height.
     */
    public static int windowHeight = 1000;

    /**
     * Directory containing the TFRecord files of runs to replay.
     */
    private File saveLoc = new File("src/main/resources/saved_data/");

    public static void main(String[] args) {
        MAIN_PlaybackSaved_TFRecord mc = new MAIN_PlaybackSaved_TFRecord();
        mc.setup();
        mc.run();
    }

    public void setup() {
        runnerPane = new PanelRunner_AnimatedFromStates();
        runnerPane.activateTab();
        runnerPane.yOffsetPixels = 600;
        add(runnerPane);
        Thread runnerThread = new Thread(runnerPane);
        runnerThread.start();

        setTitle("TFRecord run playback");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(windowWidth, windowHeight));
        pack();
        setVisible(true);
        repaint();
    }

    public void run() {
        File[] allFiles = saveLoc.listFiles();
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
        System.out.println("Number of TFRecord files found: " + playbackFiles.size() + ".");

        Collections.shuffle(playbackFiles);

        // Load files one at a time.
        for (File tfrecordFile : playbackFiles) {

            // Read all the sequences from a file.
            List<SequenceExample> dataSeries = TFRecordDataParsers.loadSequencesFromTFRecord(tfrecordFile);

            System.out.println("Read " + dataSeries.size() + " runs from file " + tfrecordFile + ".");

            // Playback each sequence one at a time.
            for (SequenceExample seq : dataSeries) {
                // Pull the states out of the Protobuf-like structure.
                State[] stateVars = TFRecordDataParsers.getStatesFromLoadedSequence(seq);

                // Have the runner panel simulate, and wait until it is done to advance to the next one.
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
}
