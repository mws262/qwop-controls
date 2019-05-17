package ui;

import org.jfree.chart.plot.XYPlot;
import tree.NodeQWOPGraphicsBase;

import java.awt.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Simple plotter for adding one or more xy data series and plotting them on a single axis. Only does scatterplots;
 * no connected lines. This is mostly for plotting standalone information to take a similar role as plot() in MATLAB.
 *
 * @author matt
 */
public class PanelPlot_Simple extends PanelPlot {

    public PanelPlot_Simple() {
        super(1); // Currently only does 1 plot in a single panel.
    }

    /**
     * Set the data in the plot panel. Overwrites any existing plot data.
     *
     * @param xData Horizontal axis data.
     * @param yData Vertical axis data.
     * @param color Color associated with each datapoint.
     * @param xLabel Label for the horizontal axis.
     * @param yLabel Label for the vertical axis.
     */
    public void setPlotData(float[] xData, float[] yData, Color[] color, String xLabel, String yLabel) {
        requestFocus();

        Iterator<Map.Entry<XYPlot, PlotDataset>> it = plotsAndData.entrySet().iterator();

        Map.Entry<XYPlot, PlotDataset> plotAndData = it.next();
        XYPlot pl = plotAndData.getKey();
        PlotDataset dat = plotAndData.getValue();

        pl.getRangeAxis().setLabel(yLabel);
        pl.getDomainAxis().setLabel(xLabel);

        dat.addSeries(0, xData, yData, color);
        setPlotBoundsFromData(pl, xData, yData);

        assert !it.hasNext(); // Should only have one plot.

        applyUpdates();
    }

    /**
     * Set the data in the plot panel. Overwrites any existing plot data.
     *
     * @param xData Horizontal axis data.
     * @param yData Vertical axis data.
     * @param color Color associated with ALL datapoints.
     * @param xLabel Label for the horizontal axis.
     * @param yLabel Label for the vertical axis.
     */
    public void setPlotData(float[] xData, float[] yData, Color color, String xLabel, String yLabel) {

        Color[] cData = new Color[xData.length];
        Arrays.fill(cData, color);

        setPlotData(xData, yData, cData, xLabel, yLabel);
    }

    /**
     * Set the data in the plot panel, overwriting any existing plot data. This method will do multiple series in the
     * same plot panel.
     *
     * @param xData List of horizontal axis data. Each list element is one dataseries.
     * @param yData List of vertical axis data. Each list element is one dataseries.
     * @param colorList List of colors for each dataseries (not for each datapoint).
     * @param xLabel Label for the horizontal axis.
     * @param yLabel Label for the vertical axis.
     */
    public void setPlotData(List<float[]> xData, List<float[]> yData, List<Color> colorList, String xLabel,
                            String yLabel) {
        int dataLength = 0;
        for (float[] dat : xData) {
            dataLength += dat.length;
        }

        float[] xDatConcat = new float[dataLength];
        float[] yDatConcat = new float[dataLength];
        Color[] colorsConcat = new Color[dataLength];

        int count = 0;
        for (int i = 0; i < xData.size(); i++) {
            for (int j = 0; j < xData.get(i).length; j++) {
                xDatConcat[count] = xData.get(i)[j];
                yDatConcat[count] = yData.get(i)[j];
                colorsConcat[count] = colorList.get(i);
                count++;
            }
        }
        setPlotData(xDatConcat, yDatConcat, colorsConcat, xLabel, yLabel);
    }

    @Override
    public void update(NodeQWOPGraphicsBase<?> plotNode) {}

    @Override
    public void plotClicked(int plotIdx) {}
}
