package ui.histogram;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.HistogramDataset;
import tree.node.NodeGenericBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphicsBase;
import ui.IUserInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PanelHistogram_LeafDepth extends PanelHistogram implements IUserInterface.TabbedPaneActivator {

    protected boolean active = false;

    public PanelHistogram_LeafDepth() {
        super("Leaf depth beyond selected Node");
        XYPlot plot = (XYPlot) chart.getPlot();
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        chart.getLegend().setVisible(false);
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
    public void update(NodeQWOPGraphicsBase<?> node) {

        List<NodeQWOPExplorableBase<?>> leafList = new ArrayList<>();
        node.recurseDownTreeInclusive(n -> {
            if (n.getChildCount() == 0) {
                leafList.add(n);
            }
        });
        int maxDepth = leafList.stream().map(NodeGenericBase::getTreeDepth).max(Comparator.naturalOrder()).orElse(5);
        int minDepth = leafList.stream().map(NodeGenericBase::getTreeDepth).min(Comparator.naturalOrder()).orElse(0);
        double[] depths = leafList.stream().map(NodeGenericBase::getTreeDepth).mapToDouble(Integer::doubleValue).toArray();

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("",depths, maxDepth - minDepth + 1, 0, maxDepth - minDepth + 1);

        setDataset(dataset);
    }
}
