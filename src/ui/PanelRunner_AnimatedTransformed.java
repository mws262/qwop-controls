package ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import filters.NodeFilter_Downsample;
import game.State;
import transformations.ITransform;
import main.Node;
import main.Utility;
import transformations.Transform_Autoencoder;
import transformations.Transform_PCA;

public class PanelRunner_AnimatedTransformed extends PanelRunner_Animated {

    private static final long serialVersionUID = 1L;

    private boolean transformsInitialized = false;

    /**
     * Maximum number of nodes used for updating transforms. These will try to be
     * evenly spaced around the tree. Keeps speed up especially for PCA.
     */
    private NodeFilter_Downsample transformDownsampler = new NodeFilter_Downsample(2000);

    private List<ITransform> encoders;
    private List<State> inStates = new ArrayList<>();

    public PanelRunner_AnimatedTransformed() {
        super();
        String modelDir = Utility.getExcutionPath() + "./tflow_models/";
        encoders = new ArrayList<>();
        encoders.add(new Transform_Autoencoder(modelDir + "AutoEnc_72to1_6layer.pb", 1));
        encoders.add(new Transform_Autoencoder(modelDir + "AutoEnc_72to2_6layer.pb", 2));
        encoders.add(new Transform_Autoencoder(modelDir + "AutoEnc_72to6_6layer.pb", 6));
        encoders.add(new Transform_Autoencoder(modelDir + "AutoEnc_72to8_6layer.pb", 8));
        encoders.add(new Transform_Autoencoder(modelDir + "AutoEnc_72to12_6layer.pb", 12));
        encoders.add(new Transform_Autoencoder(modelDir + "AutoEnc_72to16_6layer.pb", 16));
        encoders.add(new Transform_PCA(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}));
    }

    @Override
    public void simRunToNode(Node node) {
        List<Node> nlist = new ArrayList<>();
        node.getRoot().getNodesBelow(nlist);

        transformDownsampler.filter(nlist);
        List<State> slist = nlist.stream().map(Node::getState).collect(Collectors.toList());

        for (ITransform trans : encoders) {
            trans.updateTransform(slist);
        }
        transformsInitialized = true;
        super.simRunToNode(node);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (super.isActive() && game.isGameInitialized() && transformsInitialized) {
            State currState = game.getCurrentState();
            if (currState != null) inStates.add(currState);
            for (int i = 0; i < encoders.size(); i++) {
                List<State> predStateList = encoders.get(i).compressAndDecompress(inStates);
                State predState = predStateList.get(0);
                game.drawExtraRunner((Graphics2D) g, game.getXForms(predState), encoders.get(i).getName(), super.runnerScaling, super.xOffsetPixels + i * 100 + 150, super.yOffsetPixels, Node.getColorFromTreeDepth(i), normalStroke);
            }
            inStates.clear();
        }
    }

    @Override
    public void deactivateTab() {
        super.deactivateTab();

    }
}
