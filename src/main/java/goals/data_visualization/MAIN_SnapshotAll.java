package goals.data_visualization;

import data.SavableFileIO;
import data.SavableSingleGame;
import game.qwop.GameQWOP;
import game.qwop.CommandQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.node.NodeQWOPGraphics;
import ui.runner.PanelRunner_Snapshot;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Playback runs or sections of runs saved in SavableSingleRun files.
 *
 * @author matt
 */
public class MAIN_SnapshotAll extends JFrame {

    private PanelRunner_Snapshot snapshotPane;

    /**
     * Window width
     */
    private static final int windowWidth = 1920;

    /**
     * Window height
     */
    private static final int windowHeight = 1000;

    /**
     * Display up to this tree depth.
     */
    private static final int playbackDepth = 5;

    /**
     * Location where the files to be played back are located. All SavableSingleGame files will be used.
     */
    private File saveLoc = new File("src/main/resources/saved_data/tmp_testing");

    private static final Logger logger = LogManager.getLogger(MAIN_SnapshotAll.class);

    public static void main(String[] args) {
        MAIN_SnapshotAll mc = new MAIN_SnapshotAll();
        mc.setup();
        mc.run();
    }

    /**
     * Set up the graphics for animating the runner.
     */
    public void setup() {
        snapshotPane = new PanelRunner_Snapshot("Runner snapshot");
        snapshotPane.activateTab();
        snapshotPane.yOffsetPixels = 600;
        add(snapshotPane);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(windowWidth, windowHeight));
        pack();
        setVisible(true);
        repaint();
    }

    public void run() {
        File[] allFiles = saveLoc.listFiles();

        List<File> playbackFiles = new ArrayList<>();
        for (File f : Objects.requireNonNull(allFiles)) {
            if (f.getName().contains("SavableSingleGame")) {
                playbackFiles.add(f);
            }
        }

        if (playbackFiles.isEmpty()) {
            logger.warn("No files found in specified directory. Quitting.");
            return;
        }

        NodeQWOPGraphics<CommandQWOP> rootNode = new NodeQWOPGraphics<>(GameQWOP.getInitialState());

        SavableFileIO<SavableSingleGame<CommandQWOP>> fileIO = new SavableFileIO<>();
        List<SavableSingleGame<CommandQWOP>> games = new ArrayList<>();

        for (File f : playbackFiles) {
            fileIO.loadObjectsToCollection(f, games);
        }

        NodeQWOPGraphics.makeNodesFromRunInfo(games, rootNode);
        NodeQWOPGraphics<CommandQWOP> currNode = rootNode;
        while (currNode.getTreeDepth() < playbackDepth && currNode.getChildCount() > 0) {
            currNode = currNode.getChildByIndex(0);
        }
        snapshotPane.update(currNode);
        repaint();
    }
}
