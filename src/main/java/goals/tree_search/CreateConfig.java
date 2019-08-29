package goals.tree_search;

import controllers.Controller_ValueFunction;
import game.qwop.GameQWOPCaching;
import game.action.ActionGenerator_UniformNoRepeats;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import game.state.transform.Transform_Autoencoder;
import game.state.transform.Transform_PCA;
import savers.DataSaver_Null;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.filter.NodeFilter_SurvivalHorizon;
import tree.sampler.Sampler_Distribution;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.IRolloutPolicy;
import tree.sampler.rollout.RolloutPolicyBase;
import tree.sampler.rollout.RolloutPolicy_EntireRun;
import tree.sampler.rollout.RolloutPolicy_Window;
import tree.stage.TreeStage;
import tree.stage.TreeStage_FixedGames;
import tree.stage.TreeStage_Grouping;
import tree.stage.TreeStage_ValueFunctionUpdate;
import ui.IUserInterface;
import ui.UI_Full;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner_AnimatedTransformed;
import ui.runner.PanelRunner_Comparison;
import ui.runner.PanelRunner_ControlledTFlow;
import ui.runner.PanelRunner_Snapshot;
import ui.scatterplot.PanelPlot_Controls;
import ui.scatterplot.PanelPlot_SingleRun;
import ui.scatterplot.PanelPlot_States;
import ui.scatterplot.PanelPlot_Transformed;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import value.updaters.ValueUpdater_Average;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class CreateConfig {

    static GameQWOPCaching game = new GameQWOPCaching(1,2, GameQWOPCaching.StateType.DIFFERENCES);

    public static void main(String[] args) throws IOException {

        // Value function setup.
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(128);
        layerSizes.add(64);
        ValueFunction_TensorFlow<CommandQWOP, StateQWOP> valueFunction = null;
        List<String> opts = new ArrayList<>();
        opts.add("--learnrate");
        opts.add("1e-4");
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(
                    "src/main/resources/tflow_models/test.pb",
                    game.getCopy(),
                    layerSizes,
                    opts,
                    "",
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SearchConfiguration.Machine machine = new SearchConfiguration.Machine(0.7f, 1, 32, "INFO");
        SearchConfiguration.Tree<CommandQWOP> tree =
                new SearchConfiguration.Tree<>(
                        //        ActionGenerator_FixedSequence.makeExtendedGenerator(-1)
                        ActionGenerator_UniformNoRepeats.makeDefaultGenerator()
                );

        List<SearchConfiguration.SearchOperation<CommandQWOP, StateQWOP>> searchOperations = new ArrayList<>();
        IUserInterface ui = CreateConfig.setupFullUI();

        searchOperations.add(new SearchConfiguration.SearchOperation<>(
                new TreeStage_FixedGames<>(80000),
                new Sampler_Distribution<>(),
                new DataSaver_Null<>()));


        TreeStage<CommandQWOP, StateQWOP> tstage1 = new TreeStage_FixedGames<>(10000);
        TreeStage<CommandQWOP, StateQWOP> tstage2 = new TreeStage_ValueFunctionUpdate<>(valueFunction, "src/main" +
                "/resources/tflow_models/checkpoints/checkpoint_name", 1);
        TreeStage<CommandQWOP, StateQWOP> stagegroup = new TreeStage_Grouping<>(new TreeStage[] {tstage1, tstage2});

        IRolloutPolicy<CommandQWOP, StateQWOP> rollout1 = new RolloutPolicy_Window<>(
                new RolloutPolicy_EntireRun<>( // RolloutPolicy_DecayingHorizon(
                        //new EvaluationFunction_Constant(10f),
                        RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                        new Controller_ValueFunction<>(new ValueFunction_TensorFlow_StateOnly(
                                "src/main/resources/tflow_models/test.pb",
                                game.getCopy(),
                                layerSizes,
                                opts,
                                "",
                                false))));

        searchOperations.add(new SearchConfiguration.SearchOperation<>(stagegroup,
                new Sampler_UCB<>(
                        new EvaluationFunction_Constant<>(5f),
                        rollout1,
                        new ValueUpdater_Average<>(),
                        5,
                        1),
                new DataSaver_Null<>()));


        SearchConfiguration<CommandQWOP, StateQWOP> configuration = new SearchConfiguration<>(machine, tree,
                searchOperations, ui);


        SearchConfiguration.serializeToXML(new File("./src/main/resources/config/default.xml"), configuration);
        SearchConfiguration.serializeToJson(new File("./src/main/resources/config/default.json"), configuration);
        SearchConfiguration.serializeToYaml(new File("./src/main/resources/config/default.yaml"), configuration);
        // configuration.searchOperations.add(configuration.searchOperations.get(0));
//        serializeToXML(new File("./src/main/resources/config/config.xml"), configuration);
//        serializeToJson(new File("./src/main/resources/config/config.json"), configuration);
//        serializeToYaml(new File("./src/main/resources/config/config.yaml"), configuration);

        configuration = SearchConfiguration.deserializeYaml(new File("./src/main/resources/config/default.yaml"),
                SearchConfiguration.class);
    }
    /**
     * This is the heavyweight, full UI, with tree visualization and a bunch of data visualization tabs. Includes some
     * TFlow components which are troublesome on some computers.
     */
    @SuppressWarnings("Duplicates")
    public static UI_Full setupFullUI() {
        UI_Full fullUI = new UI_Full();

        /* Make each UI component */
        PanelRunner_AnimatedTransformed runnerPanel = new PanelRunner_AnimatedTransformed("Run Animation");
        PanelRunner_Snapshot snapshotPane = new PanelRunner_Snapshot("StateQWOP Viewer");
        PanelRunner_Comparison comparisonPane = new PanelRunner_Comparison("StateQWOP Compare");
        PanelPlot_States statePlotPane = new PanelPlot_States("StateQWOP Plots", 6); // 6 plots per view at the bottom.
        PanelPie_ViableFutures viableFuturesPane = new PanelPie_ViableFutures("Viable Futures");
        PanelHistogram_LeafDepth leafDepthPane = new PanelHistogram_LeafDepth("Leaf depth distribution");
        PanelPlot_Transformed pcaPlotPane =
                new PanelPlot_Transformed<>(new Transform_PCA<>(IntStream.range(0, 72).toArray()), "PCA Plots",6);
        PanelPlot_Controls controlsPlotPane = new PanelPlot_Controls("Controls Plots", 6); // 6 plots per view at the
        // bottom.
        PanelPlot_Transformed autoencPlotPane =
                new PanelPlot_Transformed<>(new Transform_Autoencoder<>("src/main/resources/tflow_models" +
                        "/AutoEnc_72to12_6layer.pb", 12), "Autoenc Plots", 6);
        autoencPlotPane.addFilter(new NodeFilter_SurvivalHorizon<>(1));
        PanelPlot_SingleRun singleRunPlotPane = new PanelPlot_SingleRun("Single Run Plots", 6);
//        workerMonitorPanel = new PanelTimeSeries_WorkerLoad("Worker status", maxWorkers);

        PanelRunner_ControlledTFlow controlledRunnerPane = new PanelRunner_ControlledTFlow("ValFun " +
                "controller", game.getCopy(), "src/main/resources/tflow_models", "src/main/resources/tflow_models" +
                "/checkpoints");

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
        fullUI.addTab(controlledRunnerPane);

//        fullUI.addTab(workerMonitorPanel);

        fullUI.start();

//        Thread monitorThread = new Thread(workerMonitorPanel);
//        monitorThread.start();

//        logger.info("GUI: Running in full graphics mode.");
        return fullUI;
    }
}
