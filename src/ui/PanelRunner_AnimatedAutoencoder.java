package ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import main.ITransform;
import main.Node;
import main.State;
import main.Transform_Autoencoder;

public class PanelRunner_AnimatedAutoencoder extends PanelRunner_Animated{

	private static final long serialVersionUID = 1L;

	private List<ITransform> encoders;
	private List<State> inStates = new ArrayList<State>();

	public PanelRunner_AnimatedAutoencoder() {
		super();

		encoders = new ArrayList<ITransform>();
		encoders.add(new Transform_Autoencoder("AutoEnc_72to1_6layer.pb", "1 output", 1));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to2_6layer.pb", "2 output", 2));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to6_6layer.pb", "6 output", 6));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to8_6layer.pb", "8 output", 8));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to12_6layer.pb", "12 output", 12));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to16_6layer.pb", "16 output", 16));

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (super.isActive() && game != null) {
			inStates.add(game.getCurrentState());
			for (int i = 0; i < encoders.size(); i++) {
				State predState = encoders.get(i).compressAndDecompress(inStates).get(0);
				game.drawExtraRunner((Graphics2D)g, game.getXForms(predState), String.valueOf(encoders.get(i).getOutputStateSize()), super.runnerScaling, super.xOffsetPixels + i*100 + 150, super.yOffsetPixels, Node.getColorFromTreeDepth(i), normalStroke);
			}
			inStates.clear();
		}
	}
}
