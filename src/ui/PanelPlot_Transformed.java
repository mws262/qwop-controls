package ui;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jblas.FloatMatrix;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.AbstractXYDataset;

import main.ITransform;
import main.Node;
import main.PanelPlot;
import main.State;
import main.Utility;

public class PanelPlot_Transformed extends PanelPlot {
	
	private static final long serialVersionUID = 1L;
	
	/** Maximum allowed datapoints. Will downsample if above. Prevents extreme lag. **/
	public int maxPlotPoints = 2000;
	
	/** Maximum number of points used to update the transform. **/
	public int maxTransformUpdatePoints = 2000;
	
	/** Node from which states are referenced. **/
	private Node selectedNode;
	
	/** Transformer to use to transform normal states into reduced coordinates. **/
	private final ITransform transformer;
	
	/** Total number of plots -- not necessarily all displayed at once. **/
	private final int numPlots;
	
	/** How many plots to squeeze in one displayed row. **/
	private static final int plotsPerView = 6;
	
	/** Currently displayed row of plots. **/
	private int currentView = 0;
	
	public PanelPlot_Transformed(ITransform transformer) {
		super(plotsPerView);
		this.transformer = transformer;
		numPlots = transformer.getOutputStateSize() * transformer.getOutputStateSize(); // Every output vs. every other output.
		//addKeyListener(this);
		setFocusable(true);
	}

	@Override
	public void update(Node plotNode) {
		
		// Do transform update if necessary: TODO decide conditions and limitations.
		List<Node> nodesBelow = new ArrayList<Node>();
		plotNode.getRoot().getNodesBelow(nodesBelow);
		downsampleNodeList(nodesBelow, maxTransformUpdatePoints);
		List<State> statesBelow = nodesBelow.stream().map(n -> n.state).collect(Collectors.toList()); // Convert from node list to state list.
		transformer.updateTransform(statesBelow); // Update transform with all states.
		// Pick which to actually plot.
		nodesBelow.clear();
		plotNode.getNodesBelow(nodesBelow);
		downsampleNodeList(nodesBelow, maxPlotPoints); // Reduce number of nodes to transform if necessary. Plotting is a bottleneck.
		statesBelow = nodesBelow.stream().map(n -> n.state).collect(Collectors.toList()); // Convert from node list to state list.
		List<float[]> transformedStates = transformer.transform(statesBelow); // Dimensionally reduced states
		requestFocus();
		
		Iterator<Entry<XYPlot, PlotDataset>> it = plotsAndData.entrySet().iterator();	
		int count = 0;
		while (it.hasNext()) {
			Entry<XYPlot, PlotDataset> plotAndData = it.next();
			XYPlot pl = plotAndData.getKey();
			PlotDataset dat = plotAndData.getValue();

			// Pick x and y from the transformed stuff:
			int datIdx = currentView * plotsPerView + count;
//			float[] xData = new float[transformedStates.size()];
//			float[] yData = new float[transformedStates.size()];
//			
//			for (int i = 0; i < transformedStates.size(); i++) {
//				xData[i] = transformedStates.get(i)[currentView];
//				yData[i] = transformedStates.get(i)[datIdx];
//			}
			
			
			Float[] xData = transformedStates.stream().map(ts -> ts[datIdx]).toArray(Float[] :: new);
			Float[] yData = transformedStates.stream().map(ts -> ts[currentView]).toArray(Float[] :: new);
			Color[] cData = nodesBelow.stream().map(n -> Node.getColorFromTreeDepth(n.treeDepth)).toArray(Color[] :: new);
			
			pl.getRangeAxis().setLabel("Component" + " " + currentView);
			pl.getDomainAxis().setLabel("Component" + " " + datIdx);
			
			dat.addSeries(0, xData, yData, cData);
			count++;
		}
		//addCommandLegend(firstPlot);
		applyUpdates();
		



			// TODO: resizing plots axes
			//XYPlot plot = (XYPlot)(plotPanels[i].getChart().getPlot());
			//plot.getDomainAxis().setRange(range);
			//plot.getRangeAxis().setRange(range);
	}

	public void plotShift(int xShift, int yShift) {
//		if (xShift != 0 || yShift != 0) {
//			// First set of plots are the 0 vs 1-6 eig
//
//			XYPlot pl = (XYPlot)plotPanels[0].getChart().getPlot();
//			PCATransformedData dat = (PCATransformedData)pl.getDataset();
//			int totalEigs = dat.evals.length;
//
//			if (dataSelect[0][0] + xShift < 0 || dataSelect[numberOfPlots - 1][0] + xShift > totalEigs - 1) {
//				xShift = 0;
//			}
//
//			if (dataSelect[0][1] + yShift < 0 || dataSelect[numberOfPlots - 1][1] + yShift > totalEigs - 1) {
//				yShift = 0;
//			}
//
//			for (int i = 0; i < numberOfPlots; i++) {
//
//				dataSelect[i][0] = dataSelect[i][0] + xShift; // Clamp within the actual number of eigenvalues we have.
//				dataSelect[i][1] = dataSelect[i][1] + yShift;
//			}
//			update();
//		}
	}

	@Override
	public void plotClicked(int plotIdx) {
		// TODO Auto-generated method stub
		
	}
	

}
