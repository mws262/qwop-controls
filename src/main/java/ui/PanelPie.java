package ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class PanelPie extends ChartPanel {

    private DefaultPieDataset dataset =  new DefaultPieDataset();

    public PanelPie() {
        this("");
    }

    public PanelPie(String title) {
        super(null);

        // Create chart
        JFreeChart chart = ChartFactory.createPieChart(
                title,
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

        setMaximumDrawHeight(1440);
        setMaximumDrawWidth(2560);
        setMinimumDrawHeight(100);
        setMinimumDrawWidth(100);

        setChart(chart);
    }

    public DefaultPieDataset getDataset() {
        return dataset;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PanelPie example = new PanelPie();
            JFrame jframe = new JFrame();
            BoxLayout bl = new BoxLayout(jframe.getContentPane(), BoxLayout.X_AXIS);
            jframe.setLayout(bl);
            jframe.add(example);
            jframe.add(new PanelPie());
            jframe.add(new PanelPie());
            jframe.add(new PanelPie());

            jframe.setSize(800, 400);
            jframe.setLocationRelativeTo(null);
            jframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jframe.setVisible(true);

            example.getDataset().insertValue(0, "DFDFDF", 1000);
        });
    }

}
