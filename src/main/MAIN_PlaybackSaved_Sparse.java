package main;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import data.SaveableFileIO;
import data.SaveableSingleGame;
import game.GameLoader;
import ui.PanelRunner_Animated;
import ui.PanelRunner_Snapshot;

/**
 * Playback runs or sections of runs saved in SaveableSingleRun files.
 * 
 * @author matt
 *
 */

@SuppressWarnings("serial")
public class MAIN_PlaybackSaved_Sparse extends JFrame{

	public GameLoader game;
	private PanelRunner_Animated runnerPane;

	/** Window width **/
	public static int windowWidth = 1920;

	/** Window height **/
	public static int windowHeight = 1000;


	File saveLoc = new File("./4_25_18");

	List<Node> leafNodes = new ArrayList<Node>(); 

	public static void main(String[] args) {
		MAIN_PlaybackSaved_Sparse mc = new MAIN_PlaybackSaved_Sparse();
		mc.setup();
		mc.run();
	}

	public void setup() {
		/* Runner pane */   
		runnerPane = new PanelRunner_Animated();
		runnerPane.activateTab();
		runnerPane.yOffsetPixels = 600;
		this.add(runnerPane);
		Thread runnerThread = new Thread(runnerPane);
		runnerThread.start();
		
		/*******************/

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
		
		List<File> playbackFiles = new ArrayList<File>();
		for (File f : allFiles){
			if (f.getName().contains("recoveries") && f.getName().contains("420") && !f.getName().contains("unsuccessful")) {
				playbackFiles.add(f);
			}
		}
		Collections.shuffle(playbackFiles);
		
		SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
		for (File f : playbackFiles) {
			Node rootNode = new Node();
			List<SaveableSingleGame> loadedGames = fileIO.loadObjectsOrdered(f.getAbsolutePath());
			Node.makeNodesFromRunInfo(loadedGames, rootNode, -1);
			leafNodes.clear();
			rootNode.getLeaves(leafNodes);
			Node endNode = leafNodes.get(0);
			Node startNode = endNode;
			while (startNode.treeDepth > 12) {
				startNode = startNode.parent;
			}
			runnerPane.simRunToNode(startNode, endNode.parent.parent); // Leaving off the last two because they usually are stupid.
			repaint();

			while (!runnerPane.isFinishedWithRun()) {
				runnerPane.repaint();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
