package ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import org.jbox2d.common.XForm;

import main.Node;
import main.QWOPGame;
import main.State;
import main.TensorflowAutoencoder;

public class PanelRunner_AnimatedAutoencoder extends PanelRunner_Animated{

	private static final long serialVersionUID = 1L;

	private List<TensorflowAutoencoder> encoders;

	public PanelRunner_AnimatedAutoencoder() {
		super();

		encoders = new ArrayList<TensorflowAutoencoder>();
		encoders.add(new TensorflowAutoencoder("AutoEnc_72to1_6layer.pb", "1 output"));
		encoders.add(new TensorflowAutoencoder("AutoEnc_72to2_6layer.pb", "2 output"));
		encoders.add(new TensorflowAutoencoder("AutoEnc_72to6_6layer.pb", "6 output"));
		encoders.add(new TensorflowAutoencoder("AutoEnc_72to8_6layer.pb", "8 output"));
		encoders.add(new TensorflowAutoencoder("AutoEnc_72to12_6layer.pb", "12 output"));
		encoders.add(new TensorflowAutoencoder("AutoEnc_72to16_6layer.pb", "16 output"));

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (super.isActive() && game != null) {
			for (int i = 0; i < encoders.size(); i++) {
				State predState = new State(encoders.get(i).getPrediction(game.getCurrentGameState()));
				XForm[] predXForms = predState.getTransforms();
				QWOPGame.drawExtraRunner((Graphics2D)g, predXForms, encoders.get(i).encoderName, super.runnerScaling, super.xOffsetPixels + i*100 + 150, super.yOffsetPixels, Node.getColorFromTreeDepth(i), normalStroke);
			}
		}
	}
}
