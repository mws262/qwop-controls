package ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;

import javax.swing.*;
import java.util.Random;

public class PanelHistogram extends ChartPanel {

    private HistogramDataset dataset = new HistogramDataset();

    JFreeChart chart;

    public PanelHistogram() {
        this("");
    }

    public PanelHistogram(String title) {
        super(null);

        chart = ChartFactory.createHistogram(
                title,
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        // flat bars look best...
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);

        setMaximumDrawHeight(1440);
        setMaximumDrawWidth(2560);
        setMinimumDrawHeight(100);
        setMinimumDrawWidth(100);

        setChart(chart);
    }

    public HistogramDataset getDataset() {
        return dataset;
    }

    public void setDataset(HistogramDataset dataset) {
        this.dataset = dataset;
        chart.getXYPlot().setDataset(dataset);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();

        PanelHistogram hist = new PanelHistogram("Test");

        frame.add(hist);

        double[] values = new double[1000];
        Random generator = new Random(12345678L);
        for (int i = 0; i < 1000; i++) {
            values[i] = generator.nextGaussian() + 5;
        }
        hist.getDataset().addSeries("H1", values, 100, 2.0, 8.0);

        frame.setSize(800, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
