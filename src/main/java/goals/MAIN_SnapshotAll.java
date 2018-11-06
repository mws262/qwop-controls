package goals;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;

import data.SavableFileIO;
import data.SavableSingleGame;
import game.GameLoader;
import tree.Node;
import ui.PanelRunner_Snapshot;

/**
 * Playback runs or sections of runs saved in SavableSingleRun files.
 *
 * @author matt
 */

public class MAIN_SnapshotAll extends JFrame {

    public GameLoader game;
    private PanelRunner_Snapshot snapshotPane;

    /**
     * Window width
     */
    public static int windowWidth = 1920;

    /**
     * Window height
     */
    public static int windowHeight = 1000;

    public static int playbackDepth = 5;

    private File saveLoc = new File("src/main/resources/11_2_18");

    public static void main(String[] args) {
        MAIN_SnapshotAll mc = new MAIN_SnapshotAll();
        mc.setup();
        mc.run();
    }

    public void setup() {
        /* Snapshot pane. */
        snapshotPane = new PanelRunner_Snapshot();
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
            if (f.getName().contains("steadyRunPrefix")) { // steadyRunPrefix.SavableSingleGame
                playbackFiles.add(f);
            }
        }

        Node rootNode = new Node();

        SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();
        List<SavableSingleGame> games = new ArrayList<>();

        for (File f : playbackFiles) {
            fileIO.loadObjectsToCollection(f, games);
        }
        Node.makeNodesFromRunInfo(games, rootNode, -1);
        Node currNode = rootNode;
        while (currNode.getTreeDepth() < playbackDepth) {
            currNode = currNode.getChildren()[0];
        }
        System.out.println(currNode.countDescendants());
        snapshotPane.update(currNode);
        repaint();

        //noinspection InfiniteLoopStatement
        while (true) {
            repaint();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
