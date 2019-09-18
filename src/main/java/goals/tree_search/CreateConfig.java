package goals.tree_search;

import controllers.Controller_Random;
import game.IGameSerializable;
import game.action.ActionGenerator_UniformNoRepeats;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOPCaching;
import game.qwop.IStateQWOP;
import game.qwop.StateQWOPDelayEmbedded_Differences;
import game.state.transform.ITransform;
import game.state.transform.Transform_Autoencoder;
import game.state.transform.Transform_PCA;
import savers.DataSaver_Null;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.node.filter.NodeFilter_SurvivalHorizon;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.IRolloutPolicy;
import tree.sampler.rollout.RolloutPolicyBase;
import tree.sampler.rollout.RolloutPolicy_DecayingHorizon;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class CreateConfig {

    static GameQWOPCaching<StateQWOPDelayEmbedded_Differences> game
            = new GameQWOPCaching<>(1,2, GameQWOPCaching.StateType.DIFFERENCES);

    static ITransform<StateQWOPDelayEmbedded_Differences> stateNormalizer;

    static {
        try {
            stateNormalizer = new StateQWOPDelayEmbedded_Differences.Normalizer(
                    StateQWOPDelayEmbedded_Differences.Normalizer.NormalizationMethod.STDEV);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        // Value function setup.
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(128);
        layerSizes.add(64);
        ValueFunction_TensorFlow<CommandQWOP, StateQWOPDelayEmbedded_Differences> valueFunction = null;
        List<String> opts = new ArrayList<>();
        opts.add("--learnrate");
        opts.add("1e-4");
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly<>(
                    "src/main/resources/tflow_models/test.pb",
                    game.getCopy(),
                    stateNormalizer,
                    layerSizes,
                    opts,
                    "",
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        SearchConfiguration.Machine machine = new SearchConfiguration.Machine(0.7f,
                1, 32, "INFO");

        SearchConfiguration.Tree<CommandQWOP> tree =
                new SearchConfiguration.Tree<>(
                        //        ActionGenerator_FixedSequence.makeExtendedGenerator(-1)
                        ActionGenerator_UniformNoRepeats.makeDefaultGenerator()
                );

        List<SearchConfiguration.SearchOperation<CommandQWOP, StateQWOPDelayEmbedded_Differences,
                GameQWOPCaching<StateQWOPDelayEmbedded_Differences>>> searchOperations = new ArrayList<>();

        IUserInterface<CommandQWOP, StateQWOPDelayEmbedded_Differences> ui
                = CreateConfig.setupFullUI(game.getCopy(), stateNormalizer);

        searchOperations.add(new SearchConfiguration.SearchOperation<>(
                new TreeStage_FixedGames<>(80000),
                game.getCopy(),
                new Sampler_UCB<>(new EvaluationFunction_Distance<>(),
                        new RolloutPolicy_DecayingHorizon<>(
                                new EvaluationFunction_Distance<>(),
                                RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                                new Controller_Random<>()),
                        new ValueUpdater_Average<>(),
                        5f,
                        10f),
                new DataSaver_Null<>()));


        TreeStage<CommandQWOP, StateQWOPDelayEmbedded_Differences> tstage1
                = new TreeStage_FixedGames<>(10000);
        TreeStage<CommandQWOP, StateQWOPDelayEmbedded_Differences> tstage2
                = new TreeStage_ValueFunctionUpdate<>(valueFunction, "src/main" +
                "/resources/tflow_models/checkpoints/checkpoint_name", 1);

        List<TreeStage<CommandQWOP, StateQWOPDelayEmbedded_Differences>> stageList = new ArrayList<>();
        stageList.add(tstage1);
        stageList.add(tstage2);
        TreeStage<CommandQWOP, StateQWOPDelayEmbedded_Differences> stagegroup = new TreeStage_Grouping<>(stageList);



        IRolloutPolicy<CommandQWOP, StateQWOPDelayEmbedded_Differences> rollout1 = new RolloutPolicy_Window<>(
//                new RolloutPolicy_EntireRun<>( // RolloutPolicy_DecayingHorizon(
//                        //new EvaluationFunction_Constant(10f),
//                        RolloutPolicyBase.getQWOPRolloutActionGenerator(),
//                        new Controller_ValueFunction<>(new ValueFunction_TensorFlow_StateOnly<>(
//                                "src/main/resources/tflow_models/test.pb",
//                                game.getCopy(),
//                                new StateQWOPDelayEmbedded_Differences
//                                        .Normalizer(StateQWOPDelayEmbedded_Differences
//                                        .Normalizer.NormalizationMethod.STDEV),
//                                layerSizes,
//                                opts,
//                                "",
//                                false)))
                new RolloutPolicy_DecayingHorizon<>(
                        new EvaluationFunction_Distance<>(),
                        RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                        new Controller_Random<>())
        );

        searchOperations.add(new SearchConfiguration.SearchOperation<>(stagegroup,
                game,
                new Sampler_UCB<>(
                        new EvaluationFunction_Constant<>(5f),
                        rollout1,
                        new ValueUpdater_Average<>(),
                        5,
                        1),
                new DataSaver_Null<>(), 5));


        SearchConfiguration<CommandQWOP, StateQWOPDelayEmbedded_Differences, GameQWOPCaching<StateQWOPDelayEmbedded_Differences>> configuration
                = new SearchConfiguration<>(machine, game.getCopy(), tree, searchOperations, ui); // TODO fix UI


//        SearchConfiguration.serializeToXML(new File("./src/main/resources/config/default.xml"), configuration);
//        SearchConfiguration.serializeToJson(new File("./src/main/resources/config/default.json"), configuration);
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
    private static <S extends IStateQWOP> UI_Full<CommandQWOP, S> setupFullUI(IGameSerializable<CommandQWOP, S> game,
                                                                              ITransform<S> stateNormalizer) {
        UI_Full<CommandQWOP, S> fullUI = new UI_Full<>();

        /* Make each UI component */
        PanelRunner_AnimatedTransformed runnerPanel = new PanelRunner_AnimatedTransformed("Run Animation");
        PanelRunner_Snapshot<S> snapshotPane = new PanelRunner_Snapshot<>("StateQWOP Viewer");
        PanelRunner_Comparison<S> comparisonPane = new PanelRunner_Comparison<>("StateQWOP Compare");
        PanelPlot_States<CommandQWOP, S> statePlotPane = new PanelPlot_States<>("StateQWOP Plots", 6); // 6
        // plots per view at
        // the bottom.
        PanelPie_ViableFutures<CommandQWOP, S> viableFuturesPane = new PanelPie_ViableFutures<>("Viable " +
                "Futures");
        PanelHistogram_LeafDepth<CommandQWOP, S> leafDepthPane = new PanelHistogram_LeafDepth<>("Leaf depth " +
                "distribution");
        PanelPlot_Transformed<CommandQWOP, S> pcaPlotPane =
                new PanelPlot_Transformed<>(new Transform_PCA<>(IntStream.range(0, 72).toArray()), "PCA Plots",6);
        PanelPlot_Controls<CommandQWOP, S> controlsPlotPane = new PanelPlot_Controls<>("Controls Plots", 6);
        // 6 plots per view at the bottom.
        PanelPlot_Transformed<CommandQWOP, S> autoencPlotPane =
                new PanelPlot_Transformed<>(new Transform_Autoencoder<>("src/main/resources/tflow_models" +
                        "/AutoEnc_72to12_6layer.pb", 12), "Autoenc Plots", 6);
        autoencPlotPane.addFilter(new NodeFilter_SurvivalHorizon<>(1));
        PanelPlot_SingleRun singleRunPlotPane = new PanelPlot_SingleRun("Single Run Plots", 6);
//        workerMonitorPanel = new PanelTimeSeries_WorkerLoad("Worker status", maxWorkers);

        PanelRunner_ControlledTFlow<S> controlledRunnerPane = new PanelRunner_ControlledTFlow<>(
                "ValFun controller",
                game.getCopy(),
                stateNormalizer,
                "src/main/resources/tflow_models",
                "src/main/resources/tflow_models/checkpoints");

        fullUI.addTab((IUserInterface.TabbedPaneActivator<CommandQWOP, S>) runnerPanel); // TODO
        fullUI.addTab(snapshotPane);
        fullUI.addTab(comparisonPane);
        fullUI.addTab(statePlotPane);
        fullUI.addTab(viableFuturesPane);
        fullUI.addTab(leafDepthPane);
        fullUI.addTab(controlsPlotPane);
//        fullUI.addTab(singleRunPlotPane); // TODO
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
