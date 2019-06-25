package ui.runner;

import tree.node.evaluator.EvaluationFunction_SqDistFromOther;
import game.GameUnified;
import game.state.IState;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphicsBase;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PanelRunner_Comparison extends PanelRunner {

    /**
     * Maximum number of similar node states to display.
     */
    public int maxNumStatesToShow = 50;

    /**
     * Node used for base comparison.
     */
    private NodeQWOPExplorableBase<?> selectedNode;

    private List<NodeQWOPExplorableBase<?>> focusNodes = new ArrayList<>();
    private List<IState> states = new ArrayList<>();
    private List<Stroke> strokes = new ArrayList<>();
    private List<Color> colors = new ArrayList<>();

    public PanelRunner_Comparison() {}

    @Override
    public void update(NodeQWOPGraphicsBase<?> node) {
        NodeQWOPGraphicsBase<?> root = node.getRoot();
        // todo
//        if (root instanceof NodeQWOPGraphicsBase) {
//            NodeQWOPGraphicsBase graphicsRoot = ((NodeQWOPGraphicsBase) root);
//            graphicsRoot.clearN();
//        }

        states.clear();
        focusNodes.clear();
        strokes.clear();
        colors.clear();

        /* Focused node first */
        selectedNode = node;
        // TODO
//        selectedNode.overrideNodeColor = Color.PINK; // Restore its red color
//        selectedNode.displayPoint = true;
        IState nodeState = selectedNode.getState();

        // Make the sequence centered around the selected node state.
        states.add(nodeState);
        strokes.add(boldStroke);
        colors.add(Color.PINK);
        focusNodes.add(node);

        // Get the nearest ones, according to the provided metric.
        EvaluationFunction_SqDistFromOther evFun = new EvaluationFunction_SqDistFromOther(selectedNode);

        Map<Float, NodeQWOPExplorableBase<?>> evaluatedNodeList = new TreeMap<>();

        List<NodeQWOPExplorableBase> allNodes = new ArrayList<>();
        node.getRoot().recurseDownTreeInclusive(allNodes::add);


        for (NodeQWOPExplorableBase<?> n : allNodes) {
            evaluatedNodeList.put(-evFun.getValue(n), n); // Low is better, so reverse so the lowest are at the top.
        }

        Iterator<NodeQWOPExplorableBase<?>> orderedNodes = evaluatedNodeList.values().iterator();

        for (int i = 0; i < maxNumStatesToShow; i++) {
            if (orderedNodes.hasNext()) {
                NodeQWOPExplorableBase<?> closeNode = orderedNodes.next();
                focusNodes.add(closeNode);
                states.add(closeNode.getState());
                strokes.add(normalStroke);
                Color matchColor = NodeQWOPGraphicsBase.getColorFromTreeDepth(i * 5, NodeQWOPGraphicsBase.lineBrightnessDefault);
                colors.add(matchColor);
                //TODO
//                closeNode.overrideNodeColor = matchColor;
//                closeNode.displayPoint = true;
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
                        xOffsetPixels + (int) (-runnerScaling * focusNodes.get(i).getState().getCenterX()), yOffsetPixels,
                        colors.get(i), strokes.get(i));
            }
        }
    }

    @Override
    public void deactivateTab() {
        // TODO
//        if (selectedNode != null) {
//            selectedNode.getRoot().clearNodeOverrideColor();
//        }
        active = false;
        states.clear();
        focusNodes.clear();
        strokes.clear();
        colors.clear();
    }
}
