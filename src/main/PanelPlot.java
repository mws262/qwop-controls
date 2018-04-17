package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import main.IUserInterface.TabbedPaneActivator;

public abstract class PanelPlot extends JPanel implements TabbedPaneActivator, ChartMouseListener {

	private static final long serialVersionUID = 1L;

	/** Should this panel be drawing or is it hidden. **/
	protected boolean active = false;

	/** How many plots do we want to squeeze in there horizontally? **/
	protected final int numberOfPlots;

	/** Array of the numberOfPlots number of plots we make. **/
	protected ChartPanel[] plotPanels;

	/****/
	protected Map<XYPlot, PlotDataset> plotsAndData = new LinkedHashMap<XYPlot, PlotDataset>(); // Retains order of insertion.

	/** Background color for the plots. **/
	protected Color plotBackgroundColor = new Color(230, 230, 230);

	/** Plotting colors for dots. **/
	public final Color actionColor1 = Node.getColorFromTreeDepth(0);
	public final Color actionColor2 = Node.getColorFromTreeDepth(10);
	public final Color actionColor3 = Node.getColorFromTreeDepth(20);
	public final Color actionColor4 = Node.getColorFromTreeDepth(30);

	public PanelPlot(int numberOfPlots) {
		this.numberOfPlots = numberOfPlots;
		plotPanels = new ChartPanel[numberOfPlots];

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		for (int i = 0; i < numberOfPlots; i++) {
			PlotDataset plData = new PlotDataset();
			JFreeChart chart = createChart(plData, null); // Null means no title yet
			XYPlot pl = chart.getXYPlot();
			plotsAndData.put(chart.getXYPlot(), plData);
			pl.setRenderer(plData.getRenderer());

			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.addChartMouseListener(this);
			chartPanel.setPopupMenu(null);
			chartPanel.setDomainZoomable(false);
			chartPanel.setRangeZoomable(false);
			chartPanel.setVisible(true);
			plotPanels[i] = chartPanel;
			add(chartPanel);
		}
	}

	/** Command all plots to apply updates. **/
	public void applyUpdates() {
		Arrays.stream(plotPanels).forEach(panel -> panel.getChart().fireChartChanged());
	}

	/** Check if the bounds need expanding, tell JFreeChart to update, and set the bounds correctly **/
	public abstract void update(Node plotNode);

	/** Tells what plot is clicked by the user. **/
	public abstract void plotClicked(int plotIdx);

	/** Axis label font. **/
	private final Font axisFont = new Font("Ariel", Font.BOLD, 12);

	/** My default settings for each plot. **/
	private JFreeChart createChart(XYDataset dataset,String name) {
		JFreeChart chart = ChartFactory.createScatterPlot(name,
				"X", "Y", dataset, PlotOrientation.VERTICAL, false, false, false);

		chart.setBackgroundPaint(plotBackgroundColor);
		chart.setPadding(new RectangleInsets(-0,-12,-8,-5)); // Pack em in really tight.
		chart.setBorderVisible(false);

		XYPlot plot = (XYPlot) chart.getPlot();

		plot.setNoDataMessage("NO DATA");
		plot.setDomainZeroBaselineVisible(true);
		plot.setRangeZeroBaselineVisible(true);
		plot.setBackgroundPaint(Color.WHITE); // Background of actual plotting area, not the surrounding border area.


		NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
		domainAxis.setAutoRangeIncludesZero(false);
		domainAxis.setTickMarkInsideLength(2.0f);
		domainAxis.setTickMarkOutsideLength(0.0f);
		domainAxis.setLabelFont(axisFont);
		domainAxis.setRange(new Range(-10, 10));

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setTickMarkInsideLength(2.0f);
		rangeAxis.setTickMarkOutsideLength(0.0f);
		rangeAxis.setLabelFont(axisFont);
		rangeAxis.setRange(new Range(-10, 10));
		return chart;
	}

	/** Add some labels for which action led to this state. Hackish. **/
	public void addCommandLegend(XYPlot pl) {
		double axisDUB = pl.getDomainAxis().getUpperBound();
		double axisDLB = pl.getDomainAxis().getLowerBound();
		double axisDSpan = (axisDUB - axisDLB);
		double axisRUB = pl.getRangeAxis().getUpperBound();
		double axisRLB = pl.getRangeAxis().getLowerBound();
		double axisRSpan = (axisRUB - axisRLB);

		XYTextAnnotation a1 = new XYTextAnnotation("< -, - >", axisDLB + axisDSpan/8, axisRUB - axisRSpan/12);
		a1.setPaint(actionColor1);
		a1.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));

		XYTextAnnotation a2 = new XYTextAnnotation("< W, O >", axisDLB + axisRSpan/8, axisRUB - 2*axisRSpan/12);
		a2.setPaint(actionColor2);
		a2.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));


		XYTextAnnotation a3 = new XYTextAnnotation("< -, - >", axisDLB + axisRSpan/8, axisRUB - 3*axisRSpan/12);
		a3.setPaint(actionColor3);
		a3.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));

		XYTextAnnotation a4 = new XYTextAnnotation("< Q, P >", axisDLB + axisRSpan/8, axisRUB - 4*axisRSpan/12);
		a4.setPaint(actionColor4);
		a4.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));
		pl.addAnnotation(a1);
		pl.addAnnotation(a2);
		pl.addAnnotation(a3);
		pl.addAnnotation(a4);
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {
		JFreeChart clickedChart = event.getChart();
		for (int i = 0; i < numberOfPlots; i++) {
			if(plotPanels[i].getChart() == (clickedChart)) {
				plotClicked(i); // Alert implementation specific method.
				break;
			}
		}
	}
	@Override
	public void chartMouseMoved(ChartMouseEvent event) {}
	@Override
	public void activateTab() { active = true; }
	@Override
	public void deactivateTab() { active = false; }
	@Override
	public boolean isActive() { return active; }
	
	/** XYDataset gets data for plotting transformed data from PCA here. **/
	public class PlotDataset extends AbstractXYDataset{

		private static final long serialVersionUID = 1L;

		private PlotRenderer renderer = new PlotRenderer();

		/** Specific series of data to by plotted. Integer is the plotindex,  **/
		private Map<Integer, DataSeries> series = new HashMap<Integer, DataSeries>();

		public PlotDataset() {
			super();
		}

		/** Add another data series at the specified plot index. **/
		public void addSeries(int plotIdx, float[] xData, float[] yData, Color[] cData) {
			DataSeries newDat = new DataSeries_FloatPrimitive(xData, yData, cData);
			series.put(plotIdx, newDat);
		}

		/** Add another data series at the specified plot index. **/
		public void addSeries(int plotIdx, Float[] xData, Float[] yData, Color[] cData) {
			DataSeries newDat = new DataSeries_FloatObj(xData, yData, cData);
			series.put(plotIdx, newDat);
		}

		@Override
		public Number getX(int seriesIdx, int dataIdx) {
			return series.get(seriesIdx).getX(dataIdx);
		}

		@Override
		public Number getY(int seriesIdx, int dataIdx) {
			return series.get(seriesIdx).getY(dataIdx);
		}

		@Override
		public int getItemCount(int seriesIdx) {
			if (series.get(seriesIdx) == null) return 0;
			return series.get(seriesIdx).size();
		}

		@Override
		public int getSeriesCount() {
			return series.size();
		}

		@Override
		public Comparable getSeriesKey(int series) {
			return null;
		}

		public XYLineAndShapeRenderer getRenderer() {
			return renderer;
		}

		private class DataSeries_FloatPrimitive implements DataSeries{
			float[] xData, yData;
			Color[] cData;
			private DataSeries_FloatPrimitive(float[] xData, float[] yData, Color[] cData) {
				if (xData.length != yData.length || xData.length != cData.length) throw new RuntimeException("Tried to add a data series with an unequal number of x's y's or colors.");
				this.xData = xData;
				this.yData = yData;
				this.cData = cData;
			}
			@Override
			public float getX(int idx) { return xData[idx]; }
			@Override
			public float getY(int idx) { return yData[idx]; }
			@Override
			public Color getColor(int idx) { return cData[idx]; }
			@Override
			public int size() {
				if (xData == null) return 0;
				return xData.length; 
			}
		}

		private class DataSeries_FloatObj implements DataSeries{ // It's surprisingly hard to convert between primitive arrays and object arrays in a non-verbose way. Hence this.
			Float[] xData, yData;
			Color[] cData;
			private DataSeries_FloatObj(Float[] xData, Float[] yData, Color[] cData) {
				if (xData.length != yData.length || xData.length != cData.length) throw new RuntimeException("Tried to add a data series with an unequal number of x's y's or colors.");
				this.xData = xData;
				this.yData = yData;
				this.cData = cData;
			}
			@Override
			public float getX(int idx) { return xData[idx]; }
			@Override
			public float getY(int idx) { return yData[idx]; }
			@Override
			public Color getColor(int idx) { return cData[idx]; }
			@Override
			public int size() {
				if (xData == null) return 0;
				return xData.length; 
			}
		}

		public class PlotRenderer extends XYLineAndShapeRenderer {
			private static final long serialVersionUID = 1L;

			public PlotRenderer() {
				super(false, true); //boolean lines, boolean shapes
				setSeriesShape( 0, new Rectangle2D.Double( -1.0, -1.0, 1.0, 1.0 ) );
				setUseOutlinePaint(false);
			}

			@Override
			public Paint getItemPaint(int seriesIdx, int dataIdx) {
				return series.get(seriesIdx).getColor(dataIdx);
			}
			@Override
			public Shape getItemShape(int row, int col) { 
				return super.getItemShape(row, col);
			}
		}
	}

	private interface DataSeries{
		public float getX(int idx);
		public float getY(int idx);
		public Color getColor(int idx);
		public int size();
	}
}
