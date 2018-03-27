package ui;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import ui.PanelPlot_States.LinkStateCombination;

public class PanelPlot_Transformed extends PanelPlot {
	
	private static final long serialVersionUID = 1L;
	
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
		
		// Do transform update if necessary: TODO decide conditions
		List<Node> nodesBelow = new ArrayList<Node>();
		plotNode.getNodesBelow(nodesBelow);
		List<State> statesBelow = nodesBelow.stream().map(n -> n.state).collect(Collectors.toList()); // Convert from node list to state list.
		transformer.updateTransform(statesBelow);
		
		// Do transformation on nodes below selected TODO: make distinction between above
		List<float[]> transformedStates = transformer.transform(statesBelow); // Dimensionally reduced states
		
		requestFocus();
		for (int i = 0; i < numberOfPlots; i++) {
			XYPlot pl = (XYPlot)plotPanels[i].getChart().getPlot();
			TransformedData plotDat = new TransformedData();
			pl.setRenderer(plotDat.getRenderer());
			plotDat.addSeries(0, null,null); //TODO
			pl.setDataset(plotDat);
			pl.getRangeAxis().setLabel("placeholder" + " " + "placeholder"); //TODO
			pl.getDomainAxis().setLabel("placeholder" + " " + "placeholder"); //TODO
			
			JFreeChart chart = plotPanels[i].getChart();
			chart.fireChartChanged();
			
			// TODO: resizing plots axes
			//XYPlot plot = (XYPlot)(plotPanels[i].getChart().getPlot());
			//plot.getDomainAxis().setRange(range);
			//plot.getRangeAxis().setRange(range);
		}
		XYPlot firstPlot = (XYPlot)plotPanels[0].getChart().getPlot();
		if (!plotColorsByDepth) {
			addCommandLegend(firstPlot);
		}
	}

	public void plotShift(int xShift, int yShift) {
		if (xShift != 0 || yShift != 0) {
			// First set of plots are the 0 vs 1-6 eig

			XYPlot pl = (XYPlot)plotPanels[0].getChart().getPlot();
			PCATransformedData dat = (PCATransformedData)pl.getDataset();
			int totalEigs = dat.evals.length;

			if (dataSelect[0][0] + xShift < 0 || dataSelect[numberOfPlots - 1][0] + xShift > totalEigs - 1) {
				xShift = 0;
			}

			if (dataSelect[0][1] + yShift < 0 || dataSelect[numberOfPlots - 1][1] + yShift > totalEigs - 1) {
				yShift = 0;
			}

			for (int i = 0; i < numberOfPlots; i++) {

				dataSelect[i][0] = dataSelect[i][0] + xShift; // Clamp within the actual number of eigenvalues we have.
				dataSelect[i][1] = dataSelect[i][1] + yShift;
			}
			update();
		}
	}

	@Override
	public void plotClicked(int plotIdx) {
		// TODO Auto-generated method stub
		
	}
	
	/** XYDataset gets data for plotting transformed data from PCA here. **/
	public class TransformedData extends AbstractXYDataset{

		private TFormPlotRenderer renderer = new TFormPlotRenderer();

		/** Specific series of data to by plotted. Integer is the plotindex,  **/
		private Map<Integer, float[]> xData = new HashMap<Integer, float[]>();
		private Map<Integer, float[]> yData = new HashMap<Integer, float[]>();

		public TransformedData() {
			super();
		}

		/** Add another data series at the specified plot index. **/
		public void addSeries(int plotIdx, float[] xData, float[] yData) {
			this.xData.put(plotIdx, xData);	
			this.yData.put(plotIdx, yData);	
		}

		@Override
		public Number getX(int series, int item) {
			return xData.get(series)[item];
		}

		@Override
		public Number getY(int series, int item) {
			return yData.get(series)[item];
		}

		@Override
		public int getItemCount(int series) {
			return xData.get(series).length;
		}

		@Override
		public int getSeriesCount() {
			return xData.size();
		}

		@Override
		public Comparable getSeriesKey(int series) {
			return null;
		}

		public XYLineAndShapeRenderer getRenderer() {
			return renderer;
		}

		public class TFormPlotRenderer extends XYLineAndShapeRenderer {
			private static final long serialVersionUID = 1L;
			
			/** Color points by corresponding depth in the tree or by command leading to this point. **/
			public boolean colorByDepth = plotColorsByDepth;

			public TFormPlotRenderer() {
				super(false, true); //boolean lines, boolean shapes
				setSeriesShape( 0, new Rectangle2D.Double( -2.0, -2.0, 2.0, 2.0 ) );
				setUseOutlinePaint(false);
			}

			@Override
			public Paint getItemPaint(int series, int item) {
				if (colorByDepth) {
					return Node.getColorFromTreeDepth(nodeList.get(item).treeDepth);
				}else{
					Color dotColor = Color.RED;
					switch (nodeList.get(item).treeDepth % 4) {
					case 0:
						dotColor = actionColor1;
						break;
					case 1:
						dotColor = actionColor2;
						break;
					case 2:
						dotColor = actionColor3;
						break;
					case 3:
						dotColor = actionColor4;
						break;
					}
					return dotColor;
				}
			}
			@Override
			public Shape getItemShape(int row, int col) { 
				return super.getItemShape(row, col);
			}
		}
	}
}

}
