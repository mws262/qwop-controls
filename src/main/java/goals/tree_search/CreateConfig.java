package goals.tree_search;

import controllers.Controller_Null;
import game.state.transform.Transform_Autoencoder;
import game.state.transform.Transform_PCA;
import savers.DataSaver_Null;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.filter.NodeFilter_SurvivalHorizon;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.RolloutPolicy_EndScore;
import tree.stage.TreeStage_MaxDepth;
import ui.UI_Full;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner_AnimatedTransformed;
import ui.runner.PanelRunner_Comparison;
import ui.runner.PanelRunner_Snapshot;
import ui.scatterplot.PanelPlot_Controls;
import ui.scatterplot.PanelPlot_SingleRun;
import ui.scatterplot.PanelPlot_States;
import ui.scatterplot.PanelPlot_Transformed;

import java.io.File;
import java.util.stream.IntStream;

public class CreateConfig {

    public static void main(String[] args) {
        SearchConfiguration configuration = new SearchConfiguration();
        configuration.ui = new SearchConfiguration.UI(setupFullUI());
        configuration.machine = new SearchConfiguration.Machine(0.7f, 1, 32, "INFO");
        configuration.searchOperations.add(new SearchConfiguration.SearchOperation(
                new TreeStage_MaxDepth(10, 10000),
                new Sampler_UCB(
                        new EvaluationFunction_Constant(5f),
                        new RolloutPolicy_EndScore(new EvaluationFunction_Constant(10f),
                                new Controller_Null())),
                new DataSaver_Null()));

        SearchConfiguration.serializeToXML(new File("./src/main/resources/config/config.xml"), configuration);
    }
    /**
     * This is the heavyweight, full UI, with tree visualization and a bunch of data visualization tabs. Includes some
     * TFlow components which are troublesome on some computers.
     */
    public static UI_Full setupFullUI() {
        UI_Full fullUI = new UI_Full();

        /* Make each UI component */
        PanelRunner_AnimatedTransformed runnerPanel = new PanelRunner_AnimatedTransformed("Run Animation");
        PanelRunner_Snapshot snapshotPane = new PanelRunner_Snapshot("State Viewer");
        PanelRunner_Comparison comparisonPane = new PanelRunner_Comparison("State Compare");
        PanelPlot_States statePlotPane = new PanelPlot_States("State Plots", 6); // 6 plots per view at the bottom.
        PanelPie_ViableFutures viableFuturesPane = new PanelPie_ViableFutures("Viable Futures");
        PanelHistogram_LeafDepth leafDepthPane = new PanelHistogram_LeafDepth("Leaf depth distribution");
        PanelPlot_Transformed pcaPlotPane =
                new PanelPlot_Transformed(new Transform_PCA(IntStream.range(0, 72).toArray()), "PCA Plots",6);
        PanelPlot_Controls controlsPlotPane = new PanelPlot_Controls("Controls Plots", 6); // 6 plots per view at the
        // bottom.
        PanelPlot_Transformed autoencPlotPane =
                new PanelPlot_Transformed(new Transform_Autoencoder("src/main/resources/tflow_models" +
                        "/AutoEnc_72to12_6layer.pb", 12), "Autoenc Plots", 6);
        autoencPlotPane.addFilter(new NodeFilter_SurvivalHorizon(1));
        PanelPlot_SingleRun singleRunPlotPane = new PanelPlot_SingleRun("Single Run Plots", 6);
//        workerMonitorPanel = new PanelTimeSeries_WorkerLoad("Worker status", maxWorkers);

        fullUI.addTab(runnerPanel);
        fullUI.addTab(snapshotPane);
        fullUI.addTab(comparisonPane);
        fullUI.addTab(statePlotPane);
        fullUI.addTab(viableFuturesPane);
        fullUI.addTab(leafDepthPane);
        fullUI.addTab(controlsPlotPane);
        fullUI.addTab(singleRunPlotPane);
        fullUI.addTab(pcaPlotPane);
        fullUI.addTab(autoencPlotPane);
//        fullUI.addTab(workerMonitorPanel);

        Thread runnerPanelThread = new Thread(runnerPanel); // All components with a copy of the GameThreadSafe should
        // have their own threads.
        runnerPanelThread.start();

//        Thread monitorThread = new Thread(workerMonitorPanel);
//        monitorThread.start();

//        logger.info("GUI: Running in full graphics mode.");
        return fullUI;
    }
}
