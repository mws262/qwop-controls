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
public class MAIN_SnapshotAll extends JFrame{

	public GameLoader game;
	private PanelRunner_Snapshot snapshotPane;

	/** Window width **/
	public static int windowWidth = 1920;

	/** Window height **/
	public static int windowHeight = 1000;


	File saveLoc = new File("./4_9_18");

	List<Node> leafNodes = new ArrayList<Node>(); 

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
		this.add(snapshotPane);
		
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

		List<File> playbackFiles = new ArrayList<File>();
		for (File f : allFiles){
			if (f.getName().contains("recoveries")) {
				playbackFiles.add(f);
			}
		}
		
		Node rootNode = new Node();
		
		SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
		List<SaveableSingleGame> games = new ArrayList<SaveableSingleGame>();
		
		for (File f : playbackFiles) {
			games.addAll(fileIO.loadObjectsOrdered(f.getAbsolutePath()));
		}
		Node.makeNodesFromRunInfo(games, rootNode, -1);
		Node currNode = rootNode;
//		while (currNode.treeDepth < 11) {
//			currNode = currNode.children.get(0);
//		}
		System.out.println(currNode.countDescendants());
		snapshotPane.giveSelectedNode(currNode);
		repaint();
		while(true) {
			repaint();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
