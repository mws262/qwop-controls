package ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import game.State;
import main.ITransform;
import main.Node;
import transformations.Transform_Autoencoder;
import transformations.Transform_PCA;

public class PanelRunner_AnimatedTransformed extends PanelRunner_Animated{

	private static final long serialVersionUID = 1L;

	private List<ITransform> encoders;
	private List<State> inStates = new ArrayList<State>();

	public PanelRunner_AnimatedTransformed() {
		super();

		encoders = new ArrayList<ITransform>();
		encoders.add(new Transform_Autoencoder("AutoEnc_72to1_6layer.pb", 1));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to2_6layer.pb", 2));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to6_6layer.pb", 6));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to8_6layer.pb", 8));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to12_6layer.pb", 12));
		encoders.add(new Transform_Autoencoder("AutoEnc_72to16_6layer.pb", 16));
		encoders.add(new Transform_PCA(new int[] {0,1,2,3,4,5,6,7,8,9,10,11}));
	}
	
	@Override
	public void simRunToNode(Node node) {
		List<Node> nlist = new ArrayList<Node>();
		node.getRoot().getNodesBelow(nlist);
		List<State> slist = nlist.stream().map(n -> n.state).collect(Collectors.toList());
		
		for (ITransform trans : encoders) {
			trans.updateTransform(slist);
		}
		super.simRunToNode(node);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (super.isActive() && game != null) {
			inStates.add(game.getCurrentState());
			for (int i = 0; i < encoders.size(); i++) {
				List<State> predStateList = encoders.get(i).compressAndDecompress(inStates);
				State predState = predStateList.get(0);
				game.drawExtraRunner((Graphics2D)g, game.getXForms(predState), encoders.get(i).getName(), super.runnerScaling, super.xOffsetPixels + i*100 + 150, super.yOffsetPixels, Node.getColorFromTreeDepth(i), normalStroke);
			}
			inStates.clear();
		}
	}
}
