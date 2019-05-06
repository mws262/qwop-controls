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

import evaluators.EvaluationFunction_SqDistFromOther;
import game.GameUnified;
import game.State;
import tree.Node;

public class PanelRunner_Comparison extends PanelRunner {

    /**
     * Maximum number of similar node states to display.
     */
    public int maxNumStatesToShow = 50;

    /**
     * Node used for base comparison.
     */
    private Node selectedNode;

    private List<Node> focusNodes = new ArrayList<>();
    private List<State> states = new ArrayList<>();
    private List<Stroke> strokes = new ArrayList<>();
    private List<Color> colors = new ArrayList<>();

    public PanelRunner_Comparison() {}

    @Override
    public void update(Node node) {
        node.getRoot().clearNodeOverrideColor();

        states.clear();
        focusNodes.clear();
        strokes.clear();
        colors.clear();

        /* Focused node first */
        selectedNode = node;
        selectedNode.overrideNodeColor = Color.PINK; // Restore its red color
        selectedNode.displayPoint = true;
        State nodeState = selectedNode.getState();

        // Make the sequence centered around the selected node state.
        states.add(nodeState);
        strokes.add(boldStroke);
        colors.add(Color.PINK);
        focusNodes.add(node);

        // Get the nearest ones, according to the provided metric.
        EvaluationFunction_SqDistFromOther evFun = new EvaluationFunction_SqDistFromOther(selectedNode);

        Map<Float, Node> evaluatedNodeList = new TreeMap<>();
        List<Node> allNodes = node.getRoot().getNodesBelow(new ArrayList<>(), true);

        for (Node n : allNodes) {
            evaluatedNodeList.put(-evFun.getValue(n), n); // Low is better, so reverse so the lowest are at the top.
        }

        Iterator<Node> orderedNodes = evaluatedNodeList.values().iterator();
        for (int i = 0; i < maxNumStatesToShow; i++) {
            if (orderedNodes.hasNext()) {
                Node closeNode = orderedNodes.next();
                focusNodes.add(closeNode);
                states.add(closeNode.getState());
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

        if (selectedNode != null && selectedNode.getState() != null) {
            for (int i = 0; i < states.size(); i++) {
                GameUnified.drawExtraRunner(g2, states.get(i), "", runnerScaling,
                        xOffsetPixels + (int) (-runnerScaling * focusNodes.get(i).getState().torso.getX()), yOffsetPixels,
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
        states.clear();
        focusNodes.clear();
        strokes.clear();
        colors.clear();
    }
}
