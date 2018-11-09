package ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class PanelPie extends JPanel {

    public PanelPie() {

        // Create dataset
        PieDataset dataset = createDataset();

        // Create chart
        JFreeChart chart = ChartFactory.createPieChart(
                "Pie Chart Example",
                dataset,
                true,
                true,
                false);

        //Format Label
        PieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator(
                "Marks {0} : ({2})", new DecimalFormat("0"), new DecimalFormat("0%"));
        PiePlot plot = ((PiePlot) chart.getPlot());

        plot.setLabelGenerator(labelGenerator);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(Color.WHITE);
        plot.setShadowGenerator(null);
        plot.setBackgroundPaint(Color.WHITE);

        add(new ChartPanel(chart));
        add(new ChartPanel(chart));
        add(new ChartPanel(chart));
    }

    private PieDataset createDataset() {

        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("80-100", 120);
        dataset.setValue("60-79", 80);
        dataset.setValue("40-59", 20);
        dataset.setValue("20-39", 7);
        dataset.setValue("0-19", 3);
        return dataset;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PanelPie example = new PanelPie();
            JFrame jframe = new JFrame();
            jframe.add(example);
            jframe.setSize(800, 400);
            jframe.setLocationRelativeTo(null);
            jframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jframe.setVisible(true);
        });
    }

}
