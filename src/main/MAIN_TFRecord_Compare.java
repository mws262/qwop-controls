package main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JFrame;
import javax.swing.JPanel;

import controllers.Controller_NearestNeighborApprox;
import controllers.Controller_NearestNeighborApprox.RunHolder;
import game.GameLoader;


/**
 * Playback runs or sections of runs saved in SaveableSingleRun files.
 * 
 * @author matt
 *
 */

@SuppressWarnings("serial")
public class MAIN_TFRecord_Compare extends JFrame implements Runnable {

	private GameLoader game = new GameLoader();

	private Controller_NearestNeighborApprox justForLoading;

	/** Window width **/
	private static int windowWidth = 1920;

	/** Window height **/
	private static int windowHeight = 1000;

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	private int xOffsetPixels = 250;
	private int yOffsetPixels = 450;

	/** Runner coordinates to pixels. **/
	private float runnerScaling = 20f;

	int bodyXOffset = -1000;

	private Color backgroundColor = Color.DARK_GRAY; //new Color(GLPanelGeneric.darkBackground[0],GLPanelGeneric.darkBackground[1],GLPanelGeneric.darkBackground[2]);
	Panel mainViewPanel;

	boolean doneInit = false;
	
	public static void main(String[] args) {
		MAIN_TFRecord_Compare mc = new MAIN_TFRecord_Compare();
		mc.setup();

	}

	public void setup() {


		File saveLoc = new File(Utility.getExcutionPath() + "saved_data/training_data");

		File[] allFiles = saveLoc.listFiles();
		if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

		List<File> exampleDataFiles = new ArrayList<File>();
		int count = 0;
		for (File f : allFiles){
			if (f.getName().contains("TFRecord") && !f.getName().contains("recovery")) {
				System.out.println("Found save file: " + f.getName());
				if (count == 12) {//if (count < 10 && count > 6) { // 10 is bad? 9 meh, 8 good
					exampleDataFiles.add(f);
				}
//8 not match
// 9 not match
				count++;
			}
		}

		justForLoading = new Controller_NearestNeighborApprox(exampleDataFiles);


		game.mainRunnerStroke = new BasicStroke(5);
		mainViewPanel = new Panel();
		this.setLayout(new BorderLayout());
		add(mainViewPanel, BorderLayout.CENTER);


		game.mainRunnerColor = Color.ORANGE;
		mainViewPanel.setBackground(backgroundColor);


		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(new Dimension(windowWidth, windowHeight));
		setContentPane(this.getContentPane());
		revalidate();
		repaint();
		pack();


		doneInit = true;
		Thread graphicsThread = new Thread(this);
		graphicsThread.start(); // Makes it smoother by updating the graphics faster than the timestep updates.
		
		setVisible(true); 
		mainViewPanel.setVisible(true);
	}


	@Override
	public void run() {
				while(true) {
					//repaint();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	}

	private void draw(Graphics g) {
		if (!doneInit) return;
		if (game != null) {
			Iterator<RunHolder> iter = justForLoading.runs.iterator();
			System.out.println(justForLoading.runs.size());
			while (iter.hasNext()) {
				RunHolder drawTraj = iter.next();

				if (drawTraj != null) {
					int killThis = drawTraj.actionDurations.get(0) + drawTraj.actionDurations.get(1);
					float specificXOffset = drawTraj.states.get(killThis).state.body.x;
					int count = 0;
					for (int i = 1 + killThis; i < drawTraj.states.size()*2/3; i += drawTraj.actionDurations.get(count) ) {
						count++;
						game.drawExtraRunner((Graphics2D)g, drawTraj.states.get(i).state, "",
								runnerScaling, xOffsetPixels - (int)(runnerScaling*specificXOffset), yOffsetPixels, 
								Node.getColorFromScaledValue(2*i, drawTraj.states.size()*2/3, 
										0.8f), PanelRunner.normalStroke);
					}
					game.drawExtraRunner((Graphics2D)g, drawTraj.states.get(killThis).state, "",
							runnerScaling, xOffsetPixels - (int)(runnerScaling*specificXOffset), yOffsetPixels, 
							Color.BLACK, PanelRunner.boldStroke);
					
				}
			}
		}
	}
	private class Panel extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			if (!game.initialized) return;
			super.paintComponent(g);
			draw(g);
		}
	}
}
