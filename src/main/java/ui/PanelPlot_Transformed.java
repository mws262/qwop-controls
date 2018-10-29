package ui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jfree.chart.plot.XYPlot;

import filters.NodeFilter_Downsample;
import game.State;
import filters.INodeFilter;
import transformations.ITransform;
import tree.Node;

public class PanelPlot_Transformed extends PanelPlot implements KeyListener {

    /**
     * Transformer to use to transform normal states into reduced coordinates.
     */
    private final ITransform transformer;

    /**
     * Filters to be applied to the node list.
     */
    private List<INodeFilter> nodeFilters = new ArrayList<>();

    /**
     * Downsampler to reduce the number of nodes we're trying to process and display
     */
    private NodeFilter_Downsample plotDownsampler = new NodeFilter_Downsample(5000);
    private NodeFilter_Downsample transformDownsampler = new NodeFilter_Downsample(2000);

    /**
     * How many plots to squeeze in one displayed row.
     */
    private int plotsPerView;

    /**
     * Keep track of the last transformed states and their nodes for graphical updates that don't need recalculation.
     */
    private List<Node> nodesToTransform = new ArrayList<>();
    private List<float[]> transformedStates;

    /**
     * Which plot, in the grid of potential plots, is currently being plotted in the first spot on the left.
     */
    private int firstPlotRow = 0;
    private int firstPlotCol = 0;

    public PanelPlot_Transformed(ITransform transformer, int plotsPerView) {
        super(plotsPerView);
        this.plotsPerView = plotsPerView;
        this.transformer = transformer;

        addKeyListener(this);
        setFocusable(true);
    }

    @Override
    public synchronized void update(Node plotNode) {
        // Do transform update if necessary:
        nodesToTransform.clear();
        plotNode.getRoot().getNodesBelow(nodesToTransform, true);
        transformDownsampler.filter(nodesToTransform);
        List<State> statesBelow = nodesToTransform.stream().map(Node::getState).collect(Collectors.toList());
        // Convert from node list to state list.
        transformer.updateTransform(statesBelow); // Update transform with all states.

        // Pick which to actually plot.
        nodesToTransform.clear();
        plotNode.getNodesBelow(nodesToTransform, true);

        // Apply any added filters (may be none).
        for (INodeFilter filter : nodeFilters) {
            filter.filter(nodesToTransform);
        }
        plotDownsampler.filter(nodesToTransform); // Reduce number of nodes to transform if necessary. Plotting is a
        // bottleneck.

        statesBelow = nodesToTransform.stream().map(Node::getState).collect(Collectors.toList()); // Convert from node
        // list to state list.
        transformedStates = transformer.transform(statesBelow); // Dimensionally reduced states

        changePlots();
    }

    public void changePlots() {
        requestFocus();

        Iterator<Entry<XYPlot, PlotDataset>> it = plotsAndData.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Entry<XYPlot, PlotDataset> plotAndData = it.next();
            XYPlot pl = plotAndData.getKey();
            PlotDataset dat = plotAndData.getValue();

            int currCol = firstPlotCol + count;
            Float[] xData = transformedStates.stream().map(ts -> ts[currCol]).toArray(Float[]::new);
            Float[] yData = transformedStates.stream().map(ts -> ts[firstPlotRow]).toArray(Float[]::new);
            Color[] cData =
                    nodesToTransform.stream().map(n -> Node.getColorFromTreeDepth(n.getTreeDepth())).toArray(Color[]::new);

            pl.getRangeAxis().setLabel("Component" + " " + firstPlotRow);
            pl.getDomainAxis().setLabel("Component" + " " + currCol);

            dat.addSeries(0, xData, yData, cData);
            setPlotBoundsFromData(pl, xData, yData);
            count++;
        }
        applyUpdates();
    }

    /**
     * Add a filter to be applied to the list of nodes to be plotted.
     */
    public void addFilter(INodeFilter filter) {
        nodeFilters.add(filter);
    }

    @Override
    public void plotClicked(int plotIdx) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (transformedStates == null || transformedStates.isEmpty()) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                if (firstPlotCol >= transformer.getOutputStateSize() - plotsPerView) return;
                firstPlotCol++;
                break;
            case KeyEvent.VK_LEFT:
                if (firstPlotCol <= 0) return;
                firstPlotCol--;
                break;
            case KeyEvent.VK_UP:
                if (firstPlotRow <= 0) return;
                firstPlotRow--;
                break;
            case KeyEvent.VK_DOWN:
                if (firstPlotRow >= transformer.getOutputStateSize() - plotsPerView) return;
                firstPlotRow++;
                break;
        }
        changePlots();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
