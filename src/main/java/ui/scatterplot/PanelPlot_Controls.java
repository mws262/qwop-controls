package ui.scatterplot;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import game.qwop.StateQWOP;
import game.state.StateVariable6D;
import game.state.transform.ITransform;
import game.state.transform.Transform_Autoencoder;
import org.jfree.chart.plot.XYPlot;
import tree.node.NodeGameExplorableBase;
import tree.node.NodeGameGraphicsBase;
import tree.node.filter.INodeFilter;
import tree.node.filter.NodeFilter_Downsample;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PanelPlot_Controls<C extends Command<?>> extends PanelPlot<C> implements KeyListener {

    /**
     * Transformer to use to transform normal states into reduced coordinates.
     */
    private ITransform transformer = new Transform_Autoencoder("src/main/resources/tflow_models" +
			"/AutoEnc_72to8_6layer.pb", 8);//new Transform_Identity();

    /**
     * Filters to be applied to the node list.
     */
    private List<INodeFilter<C>> nodeFilters = new ArrayList<>();

    /**
     * Downsampler to reduce the number of nodes we're trying to process and display
     */
    private INodeFilter<C> plotDownsampler = new NodeFilter_Downsample<>(5000);

    private List<float[]> transformedStates;

    /**
     * How many plots to squeeze in one displayed row.
     */
    private int plotsPerView;

    /**
     * Which plot, in the grid of potential plots, is currently being plotted in the first spot on the left.
     */
    private int firstPlotRow = 0;

    /**
     * Total number of plots -- not necessarily all displayed at once.
     */
    private final int numPlots;

    /**
     * Nodes to be processed and plotted.
     */
    private List<NodeGameExplorableBase<?, C>> nodes = new ArrayList<>();

    private final String name;

    public PanelPlot_Controls(@JsonProperty("name") String name, @JsonProperty("numberOfPlots") int numberOfPlots) {
        super(numberOfPlots);
        this.name = name;
        numPlots = transformer.getOutputSize();
        this.plotsPerView = numberOfPlots;
        addKeyListener(this);
        setFocusable(true);
    }

    @Override
    public void update(NodeGameGraphicsBase<?, C> plotNode) {
        nodes.clear();
        plotNode.recurseDownTreeExclusive(nodes::add);

        // Apply any added filters (may be none).
        for (INodeFilter<C> filter : nodeFilters) {
            filter.filter(nodes);
        }
        plotDownsampler.filter(nodes); // Reduce number of nodes to transform if necessary. Plotting is a bottleneck.

        List<IState> statesBelow = nodes.stream().map(NodeGameExplorableBase::getState).collect(Collectors.toList());
        // Convert from node list to state list.
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
            Color[] cData =
                    nodes.stream().map(n -> NodeGameGraphicsBase.getColorFromTreeDepth(n.getTreeDepth(),
                            NodeGameGraphicsBase.lineBrightnessDefault)).toArray(Color[]::new);

            pl.getRangeAxis().setLabel("Command duration");
            pl.getDomainAxis().setLabel(StateQWOP.ObjectName.values()[firstPlotRow].toString() + " " +
                    StateVariable6D.StateName.values()[count].toString());

            dat.addSeries(0, xData, yData, cData);
            setPlotBoundsFromData(pl, xData, yData);
            count++;
        }
        applyUpdates();
    }

    @Override
    public void plotClicked(int plotIdx) {}

    @Override
    public void keyTyped(KeyEvent e) {}

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
                if (firstPlotRow >= (transformer.getOutputSize() - plotsPerView) / plotsPerView) return;
                firstPlotRow++;
                break;
            default:
                // Nothing.
                break;
        }
        changePlots();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public String getName() {
        return name;
    }
}
