package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.XForm;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import game.GameLoader;
import game.State;
import transformations.Transform_Autoencoder;

@SuppressWarnings("serial")
public class MAIN_Controlled extends JFrame{

	public GameLoader game;
	private Tensorflow_Predictor pred = new Tensorflow_Predictor();
	private RunnerPane runnerPane;

	/** Window width **/
	public static int windowWidth = 1920;

	/** Window height **/
	public static int windowHeight = 1000;

	final Font QWOPLittle = new Font("Ariel", Font.BOLD,21);
	final Font QWOPBig = new Font("Ariel", Font.BOLD,28);

	/** Runner coordinates to pixels. **/
	public float runnerScaling = 10f;

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	public int xOffsetPixels_init = 700;
	public int xOffsetPixels = xOffsetPixels_init;
	public int yOffsetPixels = 600;

	private int phase = 0;

	public static void main(String[] args) {
		MAIN_Controlled mc = new MAIN_Controlled();
		mc.setup();
		mc.run();
	}

	public void setup() {
		/* Runner pane */   
		runnerPane = new RunnerPane();
		this.add(runnerPane);
		/*******************/

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(windowWidth, windowHeight));
		this.setContentPane(this.getContentPane());
		this.pack();
		this.setVisible(true); 
		repaint();
	}

	int toSwitchCount = Integer.MAX_VALUE;
	public void run() {
		game = new GameLoader();
		game.makeNewWorld();

		while (true) {

			try {
				switch(3) {
				case 0:
					game.stepGame(false,false,false,false);
					break;
				case 1:
					game.stepGame(false,true,true,false);
					break;
				case 2:
					game.stepGame(false,false,false,false);
					break;
				case 3:
					game.stepGame(true,false,false,true);
					break;
				default: 
					throw new RuntimeException("Sequence phase is busted: " + phase);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			float prediction = pred.getPrediction(game.getCurrentState());
			// System.out.println(prediction);

			if (toSwitchCount > 10 && prediction <1.8f) {

				//System.out.println("SWITCHING SOON");
				toSwitchCount = Math.round(prediction); 
			}

			if (toSwitchCount == 0) {
				phase = (phase + 1) % 4;
				toSwitchCount = Integer.MAX_VALUE;
			}
			toSwitchCount--;
			repaint();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Pane for displaying the animated runner executing a sequence selected on the tree. A tab.
	 * @author Matt
	 */
	public class RunnerPane extends JPanel {

		public int headPos;

		/** Highlight stroke for line drawing. **/
		private final Stroke normalStroke = new BasicStroke(0.5f);

		boolean active = true;
		
		private List<State> st = new ArrayList<State>();

		Transform_Autoencoder enc = new Transform_Autoencoder("AutoEnc_72to12_6layer.pb", 12);

		public RunnerPane() {}

		@Override
		public void paintComponent(Graphics g) {
			if (!active || game == null) return;
			super.paintComponent(g);

			game.draw(g, 10f, 960, 500);
			State currState = game.getCurrentState();
			st.add(currState);
			List<State> predState = enc.compressAndDecompress(st);
			st.clear();
			game.drawExtraRunner((Graphics2D)g, game.getXForms(predState.get(0)), "Encoded->Decoded", 10f, 960, 500, Color.RED, normalStroke);

			//    	g.drawString(dc.format(-(headpos+30)/40.) + " metres", 500, 110);
			xOffsetPixels = -headPos + xOffsetPixels_init;
		}


		public void keyDrawer(Graphics g, boolean q, boolean w, boolean o, boolean p) {

			int qOffset = (q ? 10:0);
			int wOffset = (w ? 10:0);
			int oOffset = (o ? 10:0);
			int pOffset = (p ? 10:0);

			int offsetBetweenPairs = getWidth()/4;
			int startX = -45;
			int startY = yOffsetPixels - 200;
			int size = 40;

			Font activeFont;
			FontMetrics fm;
			Graphics2D g2 = (Graphics2D)g;

			g2.setColor(Color.DARK_GRAY);
			g2.drawRoundRect(startX + 80 - qOffset/2, startY - qOffset/2, size + qOffset, size + qOffset, (size + qOffset)/10, (size + qOffset)/10);
			g2.drawRoundRect(startX + 160 - wOffset/2, startY - wOffset/2, size + wOffset, size + wOffset, (size + wOffset)/10, (size + wOffset)/10);
			g2.drawRoundRect(startX + 240 - oOffset/2 + offsetBetweenPairs, startY - oOffset/2, size + oOffset, size + oOffset, (size + oOffset)/10, (size + oOffset)/10);
			g2.drawRoundRect(startX + 320 - pOffset/2 + offsetBetweenPairs, startY - pOffset/2, size + pOffset, size + pOffset, (size + pOffset)/10, (size + pOffset)/10);

			g2.setColor(Color.LIGHT_GRAY);
			g2.fillRoundRect(startX + 80 - qOffset/2, startY - qOffset/2, size + qOffset, size + qOffset, (size + qOffset)/10, (size + qOffset)/10);
			g2.fillRoundRect(startX + 160 - wOffset/2, startY - wOffset/2, size + wOffset, size + wOffset, (size + wOffset)/10, (size + wOffset)/10);
			g2.fillRoundRect(startX + 240 - oOffset/2 + offsetBetweenPairs, startY - oOffset/2, size + oOffset, size + oOffset, (size + oOffset)/10, (size + oOffset)/10);
			g2.fillRoundRect(startX + 320 - pOffset/2 + offsetBetweenPairs, startY - pOffset/2, size + pOffset, size + pOffset, (size + pOffset)/10, (size + pOffset)/10);

			g2.setColor(Color.BLACK);

			//Used for making sure text stays centered.

			activeFont = q ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("Q", startX + 80 + size/2-fm.stringWidth("Q")/2, startY + size/2+fm.getHeight()/3);


			activeFont = w ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("W", startX + 160 + size/2-fm.stringWidth("W")/2, startY + size/2+fm.getHeight()/3);

			activeFont = o ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("O", startX + 240 + size/2-fm.stringWidth("O")/2 + offsetBetweenPairs, startY + size/2+fm.getHeight()/3);

			activeFont = p ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("P", startX + 320 + size/2-fm.stringWidth("P")/2 + offsetBetweenPairs, startY + size/2+fm.getHeight()/3);

		}
	}

}
