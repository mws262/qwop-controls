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
	
	/** Maximum number of nodes used for updating transforms. These will try to be
	 * evenly spaced around the tree. Keeps speed up especially for PCA.
	 */
	public int maxNodesToUpdateTransforms = 2000;

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
		
		downsampleNodeList(nlist, maxNodesToUpdateTransforms);
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
	@Override
	public void deactivateTab() {
		super.deactivateTab();
		
	}
	
	/** Get an evenly spaced list of nodes from one which is too big. If the list size isn't above maxSize,
	 *  then nothing happens. Note: this downsampling is IN PLACE, meaning that the original list is changed.
	 **/
	public static List<Node> downsampleNodeList(List<Node> nodes, int maxSize) {
		int numNodes = nodes.size();
		if (numNodes > maxSize) {
			float ratio = numNodes/(float)maxSize;
			for (int i = 0; i < maxSize; i++) {
				nodes.set(i, nodes.get((int)(ratio * i))); // Reassign the ith element with the spaced out one later in the arraylist.	
			}

			for (int i = numNodes; i > maxSize; i--) {
				nodes.remove(i - 1);
			}
		}
		return nodes;
	}
}
