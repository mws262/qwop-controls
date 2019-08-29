package ui.scatterplot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.action.Command;
import game.state.IState;
import org.apache.commons.lang.ArrayUtils;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;
import tree.node.NodeGameGraphicsBase;
import ui.IUserInterface.TabbedPaneActivator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PanelPlot_Simple.class, name = "plot_simple"),
        @JsonSubTypes.Type(value = PanelPlot_Transformed.class, name = "plot_transformed"),
        @JsonSubTypes.Type(value = PanelPlot_Controls.class, name = "plot_controls"),
        @JsonSubTypes.Type(value = PanelPlot_SingleRun.class, name = "plot_single_run"),
        @JsonSubTypes.Type(value = PanelPlot_States.class, name = "plot_states")
})
public abstract class PanelPlot<C extends Command<?>, S extends IState> extends JPanel implements TabbedPaneActivator<C, S>,
        ChartMouseListener {

    private static final long serialVersionUID = 1L;

    /**
     * Should this panel be drawing or is it hidden.
     */
    protected boolean active = false;

    /**
     * How many plots do we want to squeeze in there horizontally?
     */
    public final int numberOfPlots;

    /**
     * Array of the numberOfPlots number of plots we make.
     */
    protected ChartPanel[] plotPanels;

    /** **/
    protected Map<XYPlot, PlotDataset> plotsAndData = new LinkedHashMap<>(); // Retains order of insertion.

    /**
     * Background color for the plots.
     */
    private Color plotBackgroundColor = new Color(230, 230, 230);

    /**
     * Plotting colors for dots.
     */
    private final Color actionColor1 = NodeGameGraphicsBase.getColorFromTreeDepth(0, NodeGameGraphicsBase.lineBrightnessDefault);
    private final Color actionColor2 = NodeGameGraphicsBase.getColorFromTreeDepth(10, NodeGameGraphicsBase.lineBrightnessDefault);
    private final Color actionColor3 = NodeGameGraphicsBase.getColorFromTreeDepth(20, NodeGameGraphicsBase.lineBrightnessDefault);
    private final Color actionColor4 = NodeGameGraphicsBase.getColorFromTreeDepth(30, NodeGameGraphicsBase.lineBrightnessDefault);

    public PanelPlot(@JsonProperty("numberOfPlots") int numberOfPlots) {
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
            chartPanel.setMaximumDrawHeight(1440);
            chartPanel.setMaximumDrawWidth(2560);
            chartPanel.addChartMouseListener(this);
            chartPanel.setPopupMenu(null);
            chartPanel.setDomainZoomable(false);
            chartPanel.setRangeZoomable(false);
            chartPanel.setVisible(true);
            plotPanels[i] = chartPanel;
            add(chartPanel);
        }
    }

    /**
     * Command all plots to apply updates.
     */
    protected void applyUpdates() {
        Arrays.stream(plotPanels).forEach(panel -> panel.getChart().fireChartChanged());
    }

    /**
     * Tells what plot is clicked by the user.
     */
    public abstract void plotClicked(int plotIdx);

    /**
     * Axis label font.
     */
    private final Font axisFont = new Font("Ariel", Font.BOLD, 12);

    /**
     * My default settings for each plot.
     */
    private JFreeChart createChart(XYDataset dataset, String name) {
        JFreeChart chart = ChartFactory.createScatterPlot(name,
                "X", "Y", dataset, PlotOrientation.VERTICAL, false, false, false);

        chart.setBackgroundPaint(plotBackgroundColor);
        chart.setPadding(new RectangleInsets(-0, -12, -8, -5)); // Pack em in really tight.
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

    /**
     * Add some labels for which command led to this state. Hackish.
     */
    public void addCommandLegend(XYPlot pl) {
        double axisDUB = pl.getDomainAxis().getUpperBound();
        double axisDLB = pl.getDomainAxis().getLowerBound();
        double axisDSpan = (axisDUB - axisDLB);
        double axisRUB = pl.getRangeAxis().getUpperBound();
        double axisRLB = pl.getRangeAxis().getLowerBound();
        double axisRSpan = (axisRUB - axisRLB);

        XYTextAnnotation a1 = new XYTextAnnotation("< -, - >", axisDLB + axisDSpan / 8, axisRUB - axisRSpan / 12);
        a1.setPaint(actionColor1);
        a1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        XYTextAnnotation a2 = new XYTextAnnotation("< W, O >", axisDLB + axisRSpan / 8, axisRUB - 2 * axisRSpan / 12);
        a2.setPaint(actionColor2);
        a2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));


        XYTextAnnotation a3 = new XYTextAnnotation("< -, - >", axisDLB + axisRSpan / 8, axisRUB - 3 * axisRSpan / 12);
        a3.setPaint(actionColor3);
        a3.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        XYTextAnnotation a4 = new XYTextAnnotation("< Q, P >", axisDLB + axisRSpan / 8, axisRUB - 4 * axisRSpan / 12);
        a4.setPaint(actionColor4);
        a4.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        pl.addAnnotation(a1);
        pl.addAnnotation(a2);
        pl.addAnnotation(a3);
        pl.addAnnotation(a4);
    }

    /**
     * Set {@link XYPlot} axis bounds from x and y data. It will make the plot bounds big enough to see all the data,
     * plus a little bit of buffer room all around.
     *
     * @param plot Plot to set the axes bounds of.
     * @param xData Set of data whose min/max will be used to size the horizontal axis bounds.
     * @param yData Set of data whose min/max will be used to size the vertical axis bounds.
     */
    static void setPlotBoundsFromData(XYPlot plot, Float[] xData, Float[] yData) {
        float xLow = Arrays.stream(xData).min(Float::compare).orElse(-1f);
        float xHi = Arrays.stream(xData).max(Float::compare).orElse(1f);
        float xRange = Float.max(xHi - xLow, 0.001f); // Just to keep the the range from ever being zero which can
        // make the upper and lower bounds be equal, which throws an exception.

        float yLow = Arrays.stream(yData).min(Float::compare).orElse(-1f);
        float yHi = Arrays.stream(yData).max(Float::compare).orElse(1f);
        float yRange = Float.max(yHi - yLow, 0.001f);

        // Add a small buffer beyond the range of data in either direction.
        plot.getDomainAxis().setRange(xLow - xRange/25f, xHi + xRange/25f);
        plot.getRangeAxis().setRange(yLow - yRange/25f, yHi + yRange/25f);
    }

    static void setPlotBoundsFromData(XYPlot plot, float[] xData, float[] yData) {
        setPlotBoundsFromData(plot, ArrayUtils.toObject(xData), ArrayUtils.toObject(yData));
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        JFreeChart clickedChart = event.getChart();
        for (int i = 0; i < numberOfPlots; i++) {
            if (plotPanels[i].getChart() == (clickedChart)) {
                plotClicked(i); // Alert implementation specific method.
                break;
            }
        }
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {}

    @Override
    public void activateTab() {
        active = true;
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    @Override
    @JsonIgnore
    public boolean isActive() {
        return active;
    }

    /**
     * {@link XYDataset} gets data for plotting transformed data from PCA here.
     */
    public static class PlotDataset extends AbstractXYDataset {

        private static final long serialVersionUID = 1L;

        private PlotRenderer renderer = new PlotRenderer();

        /**
         * Specific series of data to by plotted. Integer is the plot index.
         */
        private Map<Integer, DataSeries> series = new HashMap<>();

        PlotDataset() {
            super();
        }

        /**
         * Add another data series at the specified plot index.
         */
        public void addSeries(int plotIdx, float[] xData, float[] yData, Color[] cData) {
            DataSeries newDat = new DataSeries_FloatPrimitive(xData, yData, cData);
            series.put(plotIdx, newDat);
        }

        /**
         * Add another data series at the specified plot index.
         */
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
        @JsonIgnore
        public int getSeriesCount() {
            return series.size();
        }

        @Override
        public Comparable getSeriesKey(int series) {
            return null;
        }

        @JsonIgnore
        XYLineAndShapeRenderer getRenderer() {
            return renderer;
        }

        private static class DataSeries_FloatPrimitive implements DataSeries {
            float[] xData, yData;
            Color[] cData;

            private DataSeries_FloatPrimitive(float[] xData, float[] yData, Color[] cData) {
                if (xData.length != yData.length || xData.length != cData.length)
                    throw new RuntimeException("Tried to add a data series with an unequal number of x's y's or " +
							"colors.");
                this.xData = xData;
                this.yData = yData;
                this.cData = cData;
            }

            @Override
            public float getX(int idx) {
                return xData[idx];
            }

            @Override
            public float getY(int idx) {
                return yData[idx];
            }

            @Override
            public Color getColor(int idx) {
                return cData[idx];
            }

            @Override
            public int size() {
                if (xData == null) return 0;
                return xData.length;
            }
        }

        private static class DataSeries_FloatObj implements DataSeries { // It's surprisingly hard to convert between
        	// primitive arrays and object arrays in a non-verbose way. Hence this.
            Float[] xData, yData;
            Color[] cData;

            private DataSeries_FloatObj(Float[] xData, Float[] yData, Color[] cData) {
                if (xData.length != yData.length || xData.length != cData.length)
                    throw new RuntimeException("Tried to add a data series with an unequal number of x's y's or colors.");
                this.xData = xData;
                this.yData = yData;
                this.cData = cData;
            }

            @Override
            public float getX(int idx) {
                return xData[idx];
            }

            @Override
            public float getY(int idx) {
                return yData[idx];
            }

            @Override
            public Color getColor(int idx) {
                return cData[idx];
            }

            @Override
            public int size() {
                if (xData == null) return 0;
                return xData.length;
            }
        }

        public class PlotRenderer extends XYLineAndShapeRenderer {
            private static final long serialVersionUID = 1L;

            PlotRenderer() {
                super(false, true); //boolean lines, boolean shapes
                setSeriesShape(0, new Rectangle2D.Double(-1.0, -1.0, 1.0, 1.0));
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

    private interface DataSeries {
        float getX(int idx);

        float getY(int idx);

        Color getColor(int idx);

        int size();
    }
}
