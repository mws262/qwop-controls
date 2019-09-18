package ui.scatterplot;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import game.state.StateVariable6D;
import game.state.transform.ITransform;
import game.state.transform.Transform_Autoencoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.plot.XYPlot;
import tree.node.NodeGameExplorableBase;
import tree.node.NodeGameGraphicsBase;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.IntStream;

/**
 * This plotter makes plots from all the states along a single run, not
 * just at the nodes. This involves re-simulating the run before plotting
 * to re-obtain those states.
 *
 * @author matt
 */
public class PanelPlot_SingleRun extends PanelPlot<CommandQWOP, StateQWOP> implements KeyListener {

    /**
     * Copy of the game used to obtain all the states along a single run by re-simulating it.
     */
    private IGameInternal<CommandQWOP, StateQWOP> game;

    /**
     * Transformer to use to transform normal states into reduced coordinates.
     */
    private ITransform<StateQWOP> transformer = new Transform_Autoencoder<>("src/main/resources/tflow_models" +
            "/AutoEnc_72to8_6layer.pb", 8);//new Transform_Identity();

    /**
     * Stores the qwop game.command we're going to execute.
     */
    private ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();

    /**
     * List of all the states that we got from simulating. Not just at nodes.
     */
    private List<StateQWOP> stateList = new ArrayList<>();
    private List<float[]> transformedStates = new ArrayList<>();
    private List<CommandQWOP> commandList = new ArrayList<>();

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
     * Node that we're plotting the game.command/states up to.
     */
    private NodeGameExplorableBase<?, CommandQWOP, StateQWOP> selectedNode;

    private final String name;

    private static final Logger logger = LogManager.getLogger(PanelPlot_SingleRun.class);


    public PanelPlot_SingleRun(@JsonProperty("name") String name, @JsonProperty("numberOfPlots") int numberOfPlots) {
        super(numberOfPlots);
        this.name = name;
        game = new GameQWOP();

        numPlots = transformer.getOutputSize();
        this.plotsPerView = numberOfPlots;
        addKeyListener(this);
        setFocusable(true);
    }

    /**
     * Run the simulation to collect the state info we want to plot.
     */
    private void simRunToNode(NodeGameExplorableBase<?, CommandQWOP, StateQWOP> node) {
        stateList.clear();
        transformedStates.clear();
        commandList.clear();
        actionQueue.clearAll();
        game.resetGame();

        ArrayList<Action<CommandQWOP>> actionList = new ArrayList<>();
        node.getSequence(actionList);
        actionQueue.addSequence(actionList);
        for (Action a : actionList) {
            logger.info(a);
        }

        stateList.add(game.getCurrentState()); // Add initial state.
        while (!actionQueue.isEmpty()) {
            CommandQWOP nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            game.step(nextCommand); // Execute timestep.
            stateList.add(game.getCurrentState());
            commandList.add(nextCommand);
        }
    }

    @Override
    public void update(NodeGameGraphicsBase<?, CommandQWOP, StateQWOP> plotNode) { // Note that this is different from
        // the other
        // PlotPanes.
        // It plots UP TO this
        // node rather than below this node.
        if (plotNode.getTreeDepth() == 0) return; // Nothing to see from root.
        selectedNode = plotNode;
        simRunToNode(plotNode);
        transformedStates = transformer.transform(stateList); // Dimensionally reduced states

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
            Float[] yData =
                    commandList.stream().map(b -> (float) ((b.get()[0] ? 1 : 0) + (b.get()[1] ? 2 : 0) + (b.get()[2] ?
                    4 : 0) + (b.get()[3] ? 8 : 0))).toArray(Float[]::new);
            Color[] cData =
                    IntStream.range(0, yData.length).mapToObj(i -> NodeGameGraphicsBase.getColorFromTreeDepth((int) (i / (float) xData.length * (float) selectedNode.getTreeDepth()), NodeGameGraphicsBase.lineBrightnessDefault)).toArray(Color[]::new);

            pl.getRangeAxis().setLabel("Command combination");
            pl.getDomainAxis().setLabel(StateQWOP.ObjectName.values()[firstPlotRow].toString() + " " +
                    StateVariable6D.StateName.values()[count].toString());

            dat.addSeries(0, Arrays.copyOf(xData, xData.length - 1), yData, cData); // Have more states than game.command,
            // so will kill the last one.

            setPlotBoundsFromData(pl, xData, yData);
            count++;
        }
        applyUpdates();
    }


    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (selectedNode == null) return;

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
                break;
        }
        changePlots();
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void plotClicked(int plotIdx) {}

    @Override
    public String getName() {
        return name;
    }
}
