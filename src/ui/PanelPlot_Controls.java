package ui;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jfree.chart.plot.XYPlot;

import filters.NodeFilter_Downsample;
import filters.NodeFilter_MissingInfo;
import game.State;
import filters.INodeFilter;
import transformations.ITransform;
import main.Node;
import main.PanelPlot;
import main.Utility;
import transformations.Transform_Autoencoder;

public class PanelPlot_Controls extends PanelPlot implements KeyListener {

    private static final long serialVersionUID = 1L;

    /**
     * Transformer to use to transform normal states into reduced coordinates.
     **/
    private ITransform transformer = new Transform_Autoencoder(Utility.getExcutionPath() + "tflow_models" +
			"/AutoEnc_72to8_6layer.pb", 8);//new Transform_Identity();

    /**
     * Filters to be applied to the node list.
     **/
    List<INodeFilter> nodeFilters = new ArrayList<>();

    /**
     * Downsampler to reduce the number of nodes we're trying to process and display
     **/
    private INodeFilter plotDownsampler = new NodeFilter_Downsample(5000);
    private INodeFilter transformDownsampler = new NodeFilter_Downsample(2000);

    private INodeFilter filterMissingInfo = new NodeFilter_MissingInfo();

    List<float[]> transformedStates;

    /**
     * How many plots to squeeze in one displayed row.
     **/
    private int plotsPerView;

    /**
     * Which plot, in the grid of potential plots, is currently being plotted in the first spot on the left.
     **/
    private int firstPlotRow = 0;

    /**
     * Total number of plots -- not necessarily all displayed at once.
     **/
    private final int numPlots;

    /**
     * Nodes to be processed and plotted.
     **/
    List<Node> nodes = new ArrayList<>();

    public PanelPlot_Controls(int numberOfPlots) {
        super(numberOfPlots);
        numPlots = transformer.getOutputStateSize();
        this.plotsPerView = numberOfPlots;
        addKeyListener(this);
        setFocusable(true);
    }

    @Override
    public void update(Node plotNode) {

        nodes.clear();
        plotNode.getNodesBelow(nodes);

        filterMissingInfo.filter(nodes);
        // Apply any added filters (may be none).
        for (INodeFilter filter : nodeFilters) {
            filter.filter(nodes);
        }
        plotDownsampler.filter(nodes); // Reduce number of nodes to transform if necessary. Plotting is a bottleneck.

        List<State> statesBelow = nodes.stream().map(n -> n.state).collect(Collectors.toList()); // Convert from node
		// list to state list.
        transformedStates = transformer.transform(statesBelow); // Dimensionally reduced states

        changePlots();
    }

    public void changePlots() {
        requestFocus();

        Iterator<Entry<XYPlot, PlotDataset>> it = plotsAndData.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            if (count >= numPlots) break;
            Entry<XYPlot, PlotDataset> plotAndData = it.next();
            XYPlot pl = plotAndData.getKey();
            PlotDataset dat = plotAndData.getValue();

            int currCol = count + firstPlotRow * plotsPerView;
            Float[] xData = transformedStates.stream().map(ts -> ts[currCol]).toArray(Float[]::new);
            Float[] yData = nodes.stream().map(n -> (float) n.getAction().getTimestepsTotal()).toArray(Float[]::new);
            Color[] cData = nodes.stream().map(n -> Node.getColorFromTreeDepth(n.treeDepth)).toArray(Color[]::new);

            pl.getRangeAxis().setLabel("Command duration");
            pl.getDomainAxis().setLabel(State.ObjectName.values()[firstPlotRow].toString() + " " +
                    State.StateName.values()[count].toString());

            dat.addSeries(0, xData, yData, cData);

            if (xData.length > 0) {
                float xLow = Arrays.stream(xData).min(Float::compare).get();
                float xHi = Arrays.stream(xData).max(Float::compare).get();

                float yLow = Arrays.stream(yData).min(Float::compare).get();
                float yHi = Arrays.stream(yData).max(Float::compare).get();

                pl.getDomainAxis().setRange(xLow - 0.05, xHi + 0.05); // Range gets whiney if you select one node and
				// try to set the range upper and lower to the same thing.
                pl.getRangeAxis().setRange(yLow - 0.05, yHi + 0.05);
            }
            count++;
        }
        //addCommandLegend(firstPlot);
        applyUpdates();
    }

    @Override
    public void plotClicked(int plotIdx) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (nodes.isEmpty()) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                break;
            case KeyEvent.VK_LEFT:
                break;
            case KeyEvent.VK_UP:
                if (firstPlotRow <= 0) return;
                firstPlotRow--;
                break;
            case KeyEvent.VK_DOWN:
                if (firstPlotRow >= (transformer.getOutputStateSize() - plotsPerView) / plotsPerView) return;
                firstPlotRow++;
                break;
        }
        changePlots();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}
