package ui;

import org.jfree.data.statistics.HistogramDataset;
import tree.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PanelHistogram_LeafDepth extends PanelHistogram implements IUserInterface.TabbedPaneActivator {

    protected boolean active = false;

    private Node currentNode;

    public PanelHistogram_LeafDepth() {
        super("Leaf depth beyond selected Node");
    }

    @Override
    public void activateTab() {
        active = true;
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void update(Node node) {

        List<Node> leafList = new ArrayList<>();
        node.getLeaves(leafList);
        int maxDepth = leafList.stream().map(Node::getTreeDepth).max(Comparator.naturalOrder()).orElse(5);
        int minDepth = leafList.stream().map(Node::getTreeDepth).min(Comparator.naturalOrder()).orElse(0);
        double[] depths = leafList.stream().map(Node::getTreeDepth).mapToDouble(Integer::doubleValue).toArray();

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("",depths, maxDepth - minDepth + 1, 0, maxDepth - minDepth + 1);

        setDataset(dataset);
        currentNode = node;
    }
}
