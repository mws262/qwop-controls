package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import evaluators.Evaluator_SqDistFromOther;
import game.GameLoader;
import main.Node;
import main.PanelRunner;

public class PanelRunner_Comparison extends PanelRunner {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum number of similar node states to display.
     **/
    public int maxNumStatesToShow = 50;

    /**
     * Its unique copy of the game. Only used for plotting, but important to keep separate.
     **/
    private final GameLoader game = new GameLoader();

    /**
     * Node used for base comparison.
     **/
    private Node selectedNode;

    private List<Node> focusNodes = new ArrayList<>();
    private List<Object[]> transforms = new ArrayList<>();
    private List<Stroke> strokes = new ArrayList<>();
    private List<Color> colors = new ArrayList<>();


    public PanelRunner_Comparison() {

    }

    /**
     * Assign a selected node for the snapshot pane to display.
     **/
    @Override
    public void update(Node node) {
        node.getRoot().clearNodeOverrideColor();

        transforms.clear();
        focusNodes.clear();
        strokes.clear();
        colors.clear();

        /***** Focused node first *****/
        selectedNode = node;
        selectedNode.overrideNodeColor = Color.PINK; // Restore its red color
        selectedNode.displayPoint = true;
        Object[] nodeTransform = game.getXForms(selectedNode.state);

        // Make the sequence centered around the selected node state.
        transforms.add(nodeTransform);
        strokes.add(boldStroke);
        colors.add(Color.PINK);
        focusNodes.add(node);

        /***** Get the nearest ones, according to the provided metric *****/
        Evaluator_SqDistFromOther evFun = new Evaluator_SqDistFromOther(selectedNode);

        Map<Float, Node> evaluatedNodeList = new TreeMap<>();
        List<Node> allNodes = node.getRoot().getNodesBelow(new ArrayList<>());

        for (Node n : allNodes) {
            evaluatedNodeList.put(evFun.getValue(n), n);
        }

        Iterator<Node> orderedNodes = evaluatedNodeList.values().iterator();
        for (int i = 0; i < maxNumStatesToShow; i++) {
            if (orderedNodes.hasNext()) {
                Node closeNode = orderedNodes.next();
                focusNodes.add(closeNode);
                transforms.add(game.getXForms(closeNode.state));
                strokes.add(normalStroke);
                Color matchColor = Node.getColorFromTreeDepth(i * 5);
                colors.add(matchColor);
                closeNode.overrideNodeColor = matchColor;
                closeNode.displayPoint = true;
            } else {
                break;
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (selectedNode != null && selectedNode.state != null) {
            for (int i = 0; i < transforms.size(); i++) {
                game.drawExtraRunner(g2, transforms.get(i), "", runnerScaling,
                        xOffsetPixels + (int) (-runnerScaling * focusNodes.get(i).state.body.x), yOffsetPixels,
                        colors.get(i), strokes.get(i));
            }
        }
    }

    @Override
    public void deactivateTab() {
        if (selectedNode != null) {
            selectedNode.getRoot().clearNodeOverrideColor();
        }
        active = false;
        transforms.clear();
        focusNodes.clear();
        strokes.clear();
        colors.clear();
    }
}
