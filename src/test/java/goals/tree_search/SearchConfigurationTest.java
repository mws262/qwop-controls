package goals.tree_search;

import controllers.Controller_Null;
import controllers.Controller_Random;
import controllers.Controller_ValueFunction;
import distributions.Distribution_Equal;
import distributions.Distribution_Normal;
import game.GameUnified;
import game.action.*;
import game.state.IState;
import game.state.State;
import game.state.transform.Transform_Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import savers.*;
import tree.node.NodeQWOPExplorable;
import tree.node.evaluator.*;
import tree.sampler.*;
import tree.sampler.rollout.*;
import ui.PanelTree;
import ui.UI_Full;
import ui.UI_Headless;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.*;
import ui.scatterplot.*;
import ui.timeseries.PanelTimeSeries_WorkerLoad;
import value.ValueFunction_Constant;
import value.ValueFunction_TensorFlow_StateOnly;
import value.updaters.ValueUpdater_Average;
import value.updaters.ValueUpdater_HardSet;
import value.updaters.ValueUpdater_StdDev;
import value.updaters.ValueUpdater_TopNChildren;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchConfigurationTest {

    private final IState testState1 = GameUnified.getInitialState();
    private final NodeQWOPExplorable sampleNode1 = new NodeQWOPExplorable(testState1,
            ActionGenerator_FixedSequence.makeDefaultGenerator(-1));

    private IState testState2;
    private NodeQWOPExplorable sampleNode2;

    @Before
    public void setup() {
        GameUnified game = new GameUnified();
        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
        }
        testState2 = game.getCurrentState();
        sampleNode2 = new NodeQWOPExplorable(testState2, ActionGenerator_FixedSequence.makeDefaultGenerator(-1));
    }

    @Test
    public void yamlDistribution_Equal() throws IOException {
        File file = File.createTempFile("disteq", "yaml");
        file.deleteOnExit();
        Distribution_Equal dist = new Distribution_Equal();
        SearchConfiguration.serializeToYaml(file, dist);
        Assert.assertTrue(file.exists());

        Distribution_Equal distLoaded = SearchConfiguration.deserializeYaml(file, Distribution_Equal.class);
        Assert.assertNotNull(distLoaded);
        Assert.assertEquals(dist, distLoaded);
    }

    @Test
    public void yamlDistribution_Normal() throws IOException {
        File file = File.createTempFile("distnorm", "yaml");
        file.deleteOnExit();
        Distribution_Normal dist = new Distribution_Normal(12, 3);
        SearchConfiguration.serializeToYaml(file, dist);
        Assert.assertTrue(file.exists());

        Distribution_Normal distLoaded = SearchConfiguration.deserializeYaml(file, Distribution_Normal.class);
        Assert.assertNotNull(distLoaded);
        Assert.assertEquals(dist, distLoaded);
    }

    @Test
    public void yamlAction() throws IOException {
        File file = File.createTempFile("action", "yaml");
        file.deleteOnExit();
        Action action = new Action(52, false, true, true, false).getCopy();
        SearchConfiguration.serializeToYaml(file, action);
        Assert.assertTrue(file.exists());

        Action actionLoaded = SearchConfiguration.deserializeYaml(file, Action.class);
        Assert.assertNotNull(actionLoaded);
        Assert.assertArrayEquals(action.peek(), actionLoaded.peek());
        Assert.assertEquals(action.getTimestepsTotal(), actionLoaded.getTimestepsTotal());
        Assert.assertEquals(action.getTimestepsRemaining(), actionLoaded.getTimestepsRemaining());
        Assert.assertFalse(actionLoaded.isMutable());

        Assert.assertEquals(action.getKeys(), actionLoaded.getKeys());
        Assert.assertEquals(actionLoaded, actionLoaded.getCopy());
    }

    @Test
    public void yamlActionList() throws IOException {
        File file = File.createTempFile("actionlist", "yaml");
        file.deleteOnExit();
        ActionList actionList = ActionList.makeExhaustiveActionList(4, 7, new Distribution_Normal(6, 1));

        SearchConfiguration.serializeToYaml(file, actionList);
        Assert.assertTrue(file.exists());

        ActionList alistLoaded = SearchConfiguration.deserializeYaml(file, ActionList.class);
        Assert.assertNotNull(alistLoaded);
        Assert.assertEquals(actionList, alistLoaded);
    }

    @Test
    public void yamlActionGenerator_FixedActions() throws IOException{
        File file = File.createTempFile("actiongenfixed", "yaml");
        file.deleteOnExit();
        ActionGenerator_FixedActions agen = new ActionGenerator_FixedActions(ActionList.makeExhaustiveActionList(8,
                11, new Distribution_Equal()));
        SearchConfiguration.serializeToYaml(file, agen);
        Assert.assertTrue(file.exists());

        ActionGenerator_FixedActions agenLoaded = SearchConfiguration.deserializeYaml(file, ActionGenerator_FixedActions.class);
        Assert.assertNotNull(agenLoaded);
        Assert.assertEquals(agen, agenLoaded);
    }

    @Test
    public void yamlActionGenerator_FixedSequence() throws IOException {
        File file = File.createTempFile("actiongenfixedseq", "yaml");
        file.deleteOnExit();
        ActionGenerator_FixedSequence agen = ActionGenerator_FixedSequence.makeDefaultGenerator(5);
        SearchConfiguration.serializeToYaml(file, agen);
        Assert.assertTrue(file.exists());

        ActionGenerator_FixedSequence agenLoaded = SearchConfiguration.deserializeYaml(file, ActionGenerator_FixedSequence.class);
        Assert.assertNotNull(agenLoaded);
        Assert.assertEquals(agen, agenLoaded);
    }

    @Test
    public void yamlActionGenerator_Null() throws IOException {
        File file = File.createTempFile("actiongennull", "yaml");
        file.deleteOnExit();
        ActionGenerator_Null agen = new ActionGenerator_Null();
        SearchConfiguration.serializeToYaml(file, agen);
        Assert.assertTrue(file.exists());

        ActionGenerator_Null agenLoaded = SearchConfiguration.deserializeYaml(file, ActionGenerator_Null.class);
        Assert.assertNotNull(agenLoaded);
        Assert.assertEquals(agen, agenLoaded);
    }

    @Test
    public void yamlActionGenerator_UniformNoRepeats() throws IOException {
        File file = File.createTempFile("actiongennorepeats", "yaml");
        file.deleteOnExit();
        ActionGenerator_UniformNoRepeats agen = ActionGenerator_UniformNoRepeats.makeDefaultGenerator();
        SearchConfiguration.serializeToYaml(file, agen);
        Assert.assertTrue(file.exists());

        ActionGenerator_UniformNoRepeats agenLoaded = SearchConfiguration.deserializeYaml(file, ActionGenerator_UniformNoRepeats.class);
        Assert.assertNotNull(agenLoaded);
        Assert.assertEquals(agen, agenLoaded);
    }

    @Test
    public void yamlEvaluationFunction_Constant() throws IOException {
        File file = File.createTempFile("evalconst", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Constant evaluationFunction = new EvaluationFunction_Constant(4.5f);
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Constant loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Constant.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getConstantValue(), loaded.getConstantValue(), 1e-12f);

        Assert.assertEquals(evaluationFunction.getValue(sampleNode1),
                loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_Distance() throws IOException {
        File file = File.createTempFile("evaldist", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Distance evaluationFunction = new EvaluationFunction_Distance();
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Distance loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Distance.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.scalingFactor, loaded.scalingFactor, 1e-12f);

        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_Random() throws IOException {
        File file = File.createTempFile("evalrand", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Random evaluationFunction = new EvaluationFunction_Random();
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Random loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Random.class);

        Assert.assertNotNull(loaded);
        // Ok, so technically we could get the same two random numbers....
        Assert.assertNotEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_HandTunedOnState() throws IOException {
        File file = File.createTempFile("evalhand", "yaml");
        file.deleteOnExit();
        EvaluationFunction_HandTunedOnState evaluationFunction = new EvaluationFunction_HandTunedOnState();
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_HandTunedOnState loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_HandTunedOnState.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_SqDistFromOther() throws IOException {
        File file = File.createTempFile("evalsqdist", "yaml");
        file.deleteOnExit();
        EvaluationFunction_SqDistFromOther evaluationFunction =
                new EvaluationFunction_SqDistFromOther(new State(new float[72], false));

        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_SqDistFromOther loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_SqDistFromOther.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
        Assert.assertEquals(evaluationFunction.getComparisonState(), loaded.getComparisonState());
    }

    @Test
    public void yamlEvaluationFunction_Velocity() throws IOException {
        File file = File.createTempFile("evalvel", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Velocity evaluationFunction = new EvaluationFunction_Velocity();

        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Velocity loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Velocity.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlController_Null() throws IOException {
        File file = File.createTempFile("controlnull", "yaml");
        file.deleteOnExit();
        Controller_Null controller = new Controller_Null();

        SearchConfiguration.serializeToYaml(file, controller);
        Assert.assertTrue(file.exists());

        Controller_Null loaded = SearchConfiguration.deserializeYaml(file, Controller_Null.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(controller, loaded);
    }

    @Test
    public void yamlController_Random() throws IOException {
        File file = File.createTempFile("controlrand", "yaml");
        file.deleteOnExit();
        Controller_Random controller = new Controller_Random();

        SearchConfiguration.serializeToYaml(file, controller);
        Assert.assertTrue(file.exists());

        Controller_Random loaded = SearchConfiguration.deserializeYaml(file, Controller_Random.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(controller, loaded);
    }

    @Test
    public void yamlController_ValueFunction() throws IOException {
        File file = File.createTempFile("controlvalue", "yaml");
        file.deleteOnExit();
        // Base case for simple value function.
        Controller_ValueFunction controller = new Controller_ValueFunction(new ValueFunction_Constant(45f));

        SearchConfiguration.serializeToYaml(file, controller);
        Assert.assertTrue(file.exists());

        Controller_ValueFunction loaded = SearchConfiguration.deserializeYaml(file, Controller_ValueFunction.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(controller.policy(sampleNode1), loaded.policy(sampleNode1));
        Assert.assertEquals(controller.policy(sampleNode2), loaded.policy(sampleNode2));


        // Now for tflow value function also.
        File modelFile = File.createTempFile("valfunstatemodelcontrol", "pb");
        file.deleteOnExit();

        GameUnified game = new GameUnified();
        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(2);
        hiddenLayerSizes.add(3);
        List<String> extraArgs = new ArrayList<>();
        extraArgs.add("--learnrate");
        extraArgs.add("1e-1");

        ValueFunction_TensorFlow_StateOnly valFun = new ValueFunction_TensorFlow_StateOnly(
                modelFile.getPath(),
                game,
                hiddenLayerSizes,
                extraArgs,
                "");

        List<File> files = valFun.saveCheckpoint("src/test/resources/testyamlmore");
        files.forEach(File::deleteOnExit);
        Assert.assertTrue(modelFile.exists());

        controller = new Controller_ValueFunction(valFun);

        SearchConfiguration.serializeToYaml(file, controller);
        Assert.assertTrue(file.exists());

        loaded = SearchConfiguration.deserializeYaml(file, Controller_ValueFunction.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(controller.policy(sampleNode1), loaded.policy(sampleNode1));
        Assert.assertEquals(controller.policy(sampleNode2), loaded.policy(sampleNode2));
    }

    @Test
    public void yamlValueFunction_Constant() throws IOException {
        File file = File.createTempFile("controlrand", "yaml");
        file.deleteOnExit();
        ValueFunction_Constant valFun = new ValueFunction_Constant(9.1f);

        SearchConfiguration.serializeToYaml(file, valFun);
        Assert.assertTrue(file.exists());

        ValueFunction_Constant loaded = SearchConfiguration.deserializeYaml(file, ValueFunction_Constant.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(valFun, loaded);
    }
    @Test
    public void yamlValueFunction_TensorFlow_StateOnly() throws IOException {
        File file = File.createTempFile("tflowvaluestate", "yaml");
        File modelFile = File.createTempFile("valfunstatemodel", "pb");
        modelFile.deleteOnExit();
        file.deleteOnExit();

        GameUnified game = new GameUnified();
        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(10);
        hiddenLayerSizes.add(3);
        List<String> extraArgs = new ArrayList<>();
        extraArgs.add("--learnrate");
        extraArgs.add("1e-5");

        ValueFunction_TensorFlow_StateOnly valFun = new ValueFunction_TensorFlow_StateOnly(
                modelFile.getPath(),
                game,
                hiddenLayerSizes,
                extraArgs,
                "");

        List<File> files = valFun.saveCheckpoint("src/test/resources/testyaml");
        files.forEach(File::deleteOnExit);

        Assert.assertTrue(modelFile.exists());

        SearchConfiguration.serializeToYaml(file, valFun);
        Assert.assertTrue(file.exists());

        ValueFunction_TensorFlow_StateOnly loaded = SearchConfiguration.deserializeYaml(file, ValueFunction_TensorFlow_StateOnly.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(valFun.gameTemplate.getStateDimension(), loaded.gameTemplate.getStateDimension());
        Assert.assertEquals(valFun.fileName, loaded.fileName);
        Assert.assertEquals(valFun.hiddenLayerSizes, loaded.hiddenLayerSizes);
        Assert.assertEquals(valFun.inputSize, loaded.inputSize);
        Assert.assertEquals(valFun.outputSize, loaded.outputSize);
        Assert.assertEquals(valFun.additionalNetworkArgs, loaded.additionalNetworkArgs);

        Assert.assertEquals(valFun.evaluate(sampleNode1), loaded.evaluate(sampleNode1), 1e-8f);
        Assert.assertEquals(valFun.evaluate(sampleNode2), loaded.evaluate(sampleNode2), 1e-8f);


        // Make sure that loading creates the file if it doesn't already exist.
        Assert.assertTrue(modelFile.delete());
        Assert.assertFalse(modelFile.exists());
        loaded = SearchConfiguration.deserializeYaml(file, ValueFunction_TensorFlow_StateOnly.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(modelFile.getName(), loaded.getGraphDefinitionFile().getName());
        Assert.assertTrue(loaded.getGraphDefinitionFile().exists());
    }

    @Test
    public void yamlRolloutPolicy_EndScore() throws IOException {
        File file = File.createTempFile("rolloutendscore", "yaml");
        file.deleteOnExit();
        RolloutPolicy_EndScore rollout =
                new RolloutPolicy_EndScore(new EvaluationFunction_Constant(3f), new Controller_Null(), 142);
        rollout.failureMultiplier = 3.14f;
        rollout.rolloutActionGenerator = ActionGenerator_UniformNoRepeats.makeDefaultGenerator();

        SearchConfiguration.serializeToYaml(file, rollout);
        Assert.assertTrue(file.exists());

        RolloutPolicy_EndScore loaded = SearchConfiguration.deserializeYaml(file,
                RolloutPolicy_EndScore.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rollout.failureMultiplier, loaded.failureMultiplier, 1e-12f);
        Assert.assertEquals(142, rollout.maxTimesteps);
        Assert.assertEquals(142, loaded.maxTimesteps);
        Assert.assertEquals(rollout.evaluationFunction.getValue(sampleNode1),
                loaded.evaluationFunction.getValue(sampleNode1), 1e-10);
        Assert.assertEquals(rollout.getRolloutController().policy(sampleNode2),
                loaded.getRolloutController().policy(sampleNode2));
        Assert.assertEquals(rollout.rolloutActionGenerator, loaded.rolloutActionGenerator);
    }

    @Test
    public void yamlRolloutPolicy_DeltaScore() throws IOException {
        File file = File.createTempFile("rolloutdeltascore", "yaml");
        file.deleteOnExit();
        RolloutPolicy_DeltaScore rollout =
                new RolloutPolicy_DeltaScore(new EvaluationFunction_Constant(3f), new Controller_Null(), 142);
        rollout.failureMultiplier = 3.14f;
        rollout.rolloutActionGenerator = ActionGenerator_UniformNoRepeats.makeDefaultGenerator();

        SearchConfiguration.serializeToYaml(file, rollout);
        Assert.assertTrue(file.exists());

        RolloutPolicy_DeltaScore loaded = SearchConfiguration.deserializeYaml(file,
                RolloutPolicy_DeltaScore.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rollout.failureMultiplier, loaded.failureMultiplier, 1e-12f);
        Assert.assertEquals(142, rollout.maxTimesteps);
        Assert.assertEquals(142, loaded.maxTimesteps);
        Assert.assertEquals(rollout.evaluationFunction.getValue(sampleNode1),
                loaded.evaluationFunction.getValue(sampleNode1), 1e-10);
        Assert.assertEquals(rollout.getRolloutController().policy(sampleNode2),
                loaded.getRolloutController().policy(sampleNode2));
        Assert.assertEquals(rollout.rolloutActionGenerator, loaded.rolloutActionGenerator);
    }

    @Test
    public void yamlRolloutPolicy_JustEvaluate() throws IOException {
        File file = File.createTempFile("rolloutdeltascore", "yaml");
        file.deleteOnExit();
        RolloutPolicy_JustEvaluate rollout =
                new RolloutPolicy_JustEvaluate(new EvaluationFunction_Constant(3f));

        SearchConfiguration.serializeToYaml(file, rollout);
        Assert.assertTrue(file.exists());

        RolloutPolicy_JustEvaluate loaded = SearchConfiguration.deserializeYaml(file,
                RolloutPolicy_JustEvaluate.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rollout.evaluationFunction.getValue(sampleNode1),
                loaded.evaluationFunction.getValue(sampleNode1), 1e-10);
    }

    @Test
    public void yamlRolloutPolicy_RandomColdStart() throws IOException {
        File file = File.createTempFile("rolloutdeltascore", "yaml");
        file.deleteOnExit();
        RolloutPolicy_RandomColdStart rollout =
                new RolloutPolicy_RandomColdStart(new EvaluationFunction_Constant(3f));
        rollout.rolloutActionGenerator = ActionGenerator_UniformNoRepeats.makeDefaultGenerator();

        SearchConfiguration.serializeToYaml(file, rollout);
        Assert.assertTrue(file.exists());

        RolloutPolicy_RandomColdStart loaded = SearchConfiguration.deserializeYaml(file,
                RolloutPolicy_RandomColdStart.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rollout.evaluationFunction.getValue(sampleNode1),
                loaded.evaluationFunction.getValue(sampleNode1), 1e-10);
        Assert.assertEquals(rollout.failureMultiplier, loaded.failureMultiplier, 1e-10f);
        Assert.assertEquals(rollout.maxTimesteps, loaded.maxTimesteps, 1e-10f);
        Assert.assertEquals(rollout.rolloutActionGenerator, loaded.rolloutActionGenerator);
    }

    @Test
    public void yamlRolloutPolicy_WeightWithValueFunction() throws IOException {
        File file = File.createTempFile("rolloutdeltascore", "yaml");
        file.deleteOnExit();
        RolloutPolicy_WeightWithValueFunction rollout =
                new RolloutPolicy_WeightWithValueFunction(new RolloutPolicy_EndScore(new EvaluationFunction_Constant(2f), new Controller_Null()), new ValueFunction_Constant(3f));
        rollout.valueFunctionWeight = 0.2f;

        SearchConfiguration.serializeToYaml(file, rollout);
        Assert.assertTrue(file.exists());

        RolloutPolicy_WeightWithValueFunction loaded = SearchConfiguration.deserializeYaml(file,
                RolloutPolicy_WeightWithValueFunction.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rollout.valueFunctionWeight, loaded.valueFunctionWeight, 1e-10f);

        Assert.assertEquals(rollout.getIndividualRollout().rollout(sampleNode1, new GameUnified()),
                loaded.getIndividualRollout().rollout(sampleNode1, new GameUnified()), 1e-10f);

        Assert.assertEquals(rollout.getValueFunction().evaluate(sampleNode2),
                loaded.getValueFunction().evaluate(sampleNode2), 1e-10f);
    }

    @Test
    public void yamlRolloutPolicy_Window() throws IOException {
        File file = File.createTempFile("rolloutdeltascore", "yaml");
        file.deleteOnExit();
        RolloutPolicy_Window rollout =
                new RolloutPolicy_Window(new RolloutPolicy_EndScore(new EvaluationFunction_Constant(1f),
                        new Controller_Null()));
        rollout.selectionCriteria = RolloutPolicy_Window.Criteria.WORST;

        SearchConfiguration.serializeToYaml(file, rollout);
        Assert.assertTrue(file.exists());

        RolloutPolicy_Window loaded = SearchConfiguration.deserializeYaml(file,
                RolloutPolicy_Window.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rollout.selectionCriteria, loaded.selectionCriteria);
        Assert.assertEquals(rollout.getIndividualRollout().getClass(), loaded.getIndividualRollout().getClass());
        Assert.assertEquals(rollout.getIndividualRollout().rollout(sampleNode1, new GameUnified()),
                loaded.getIndividualRollout().rollout(sampleNode1, new GameUnified()), 1e-10);
    }

    @Test
    public void yamlDataSave_DenseJava() throws IOException {
        File file = File.createTempFile("savedensejava", "yaml");
        file.deleteOnExit();

        DataSaver_DenseJava saver = new DataSaver_DenseJava();
        saver.setSaveInterval(101);
        saver.setSavePath("stuff");

        SearchConfiguration.serializeToYaml(file, saver);
        Assert.assertTrue(file.exists());

        DataSaver_DenseJava loaded = SearchConfiguration.deserializeYaml(file,
                DataSaver_DenseJava.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(saver.getSaveInterval(), loaded.getSaveInterval());
        Assert.assertEquals(saver.getSavePath(), loaded.getSavePath());
    }

    @Test
    public void yamlDataSave_DenseTFRecord() throws IOException {
        File file = File.createTempFile("savedensetf", "yaml");
        file.deleteOnExit();

        DataSaver_DenseTFRecord saver = new DataSaver_DenseTFRecord();
        saver.setSaveInterval(101);
        saver.setSavePath("stuff");
        saver.filenameOverride = "morestuff";

        SearchConfiguration.serializeToYaml(file, saver);
        Assert.assertTrue(file.exists());

        DataSaver_DenseTFRecord loaded = SearchConfiguration.deserializeYaml(file,
                DataSaver_DenseTFRecord.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(saver.getSaveInterval(), loaded.getSaveInterval());
        Assert.assertEquals(saver.getSavePath(), loaded.getSavePath());
        Assert.assertEquals(saver.filenameOverride, loaded.filenameOverride);
        Assert.assertEquals(saver.fileExtension, saver.fileExtension);
        Assert.assertEquals(saver.filePrefix, saver.filePrefix);
    }

    @Test
    public void yamlDataSave_Null() throws IOException {
        File file = File.createTempFile("savenull", "yaml");
        file.deleteOnExit();

        DataSaver_Null saver = new DataSaver_Null();

        SearchConfiguration.serializeToYaml(file, saver);
        Assert.assertTrue(file.exists());

        DataSaver_Null loaded = SearchConfiguration.deserializeYaml(file, DataSaver_Null.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlDataSave_Sparse() throws IOException {
        File file = File.createTempFile("savesparse", "yaml");
        file.deleteOnExit();

        DataSaver_Sparse saver = new DataSaver_Sparse();
        saver.setSaveInterval(10101);
        saver.setSavePath("wefsdf");

        SearchConfiguration.serializeToYaml(file, saver);
        Assert.assertTrue(file.exists());

        DataSaver_Sparse loaded = SearchConfiguration.deserializeYaml(file, DataSaver_Sparse.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(saver.fileExtension, loaded.fileExtension);
        Assert.assertEquals(saver.filePrefix, loaded.filePrefix);
        Assert.assertEquals(saver.getSaveInterval(), loaded.getSaveInterval());
        Assert.assertEquals(saver.getSavePath(), loaded.getSavePath());
    }

    @Test
    public void yamlDataSave_StageSelected() throws IOException {
        File file = File.createTempFile("savestateselect", "yaml");
        file.deleteOnExit();

        DataSaver_StageSelected saver = new DataSaver_StageSelected();
        saver.setSavePath("wefsdf");
        saver.overrideFilename = "test";

        SearchConfiguration.serializeToYaml(file, saver);
        Assert.assertTrue(file.exists());

        DataSaver_StageSelected loaded = SearchConfiguration.deserializeYaml(file, DataSaver_StageSelected.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(saver.fileExtension, loaded.fileExtension);
        Assert.assertEquals(saver.overrideFilename, loaded.overrideFilename);
        Assert.assertEquals(saver.filePrefix, loaded.filePrefix);
        Assert.assertEquals(saver.getSavePath(), loaded.getSavePath());
    }

    @Test
    public void yamlSampler_Deterministic() throws IOException {
        File file = File.createTempFile("samplerdeterministic", "yaml");
        file.deleteOnExit();

        Sampler_Deterministic sampler = new Sampler_Deterministic();

        SearchConfiguration.serializeToYaml(file, sampler);
        Assert.assertTrue(file.exists());

        Sampler_Deterministic loaded = SearchConfiguration.deserializeYaml(file, Sampler_Deterministic.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlSampler_Distribution() throws IOException {
        File file = File.createTempFile("samplerdist", "yaml");
        file.deleteOnExit();

        Sampler_Distribution sampler = new Sampler_Distribution();

        SearchConfiguration.serializeToYaml(file, sampler);
        Assert.assertTrue(file.exists());

        Sampler_Distribution loaded = SearchConfiguration.deserializeYaml(file, Sampler_Distribution.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlSampler_FixedDepth() throws IOException {
        File file = File.createTempFile("samplerfixed", "yaml");
        file.deleteOnExit();

        Sampler_FixedDepth sampler = new Sampler_FixedDepth(7);

        SearchConfiguration.serializeToYaml(file, sampler);
        Assert.assertTrue(file.exists());

        Sampler_FixedDepth loaded = SearchConfiguration.deserializeYaml(file, Sampler_FixedDepth.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(sampler.getHorizonDepth(), loaded.getHorizonDepth());
    }

    @Test
    public void yamlSampler_Greedy() throws IOException {
        File file = File.createTempFile("samplergreedy", "yaml");
        file.deleteOnExit();

        Sampler_Greedy sampler = new Sampler_Greedy(new EvaluationFunction_Constant(4f));
        sampler.samplesAt0 = 34535;
        sampler.depthN = 1211;
        sampler.samplesAtN = 9898;
        sampler.samplesAtInf = 573;
        sampler.forwardJump = 8385;
        sampler.backwardsJump = 66;
        sampler.backwardsJumpMin = 24;
        sampler.backwardsJumpFailureMultiplier = 4.87f;

        SearchConfiguration.serializeToYaml(file, sampler);
        Assert.assertTrue(file.exists());

        Sampler_Greedy loaded = SearchConfiguration.deserializeYaml(file, Sampler_Greedy.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(sampler.getEvaluationFunction().getValue(sampleNode1),
                loaded.getEvaluationFunction().getValue(sampleNode1), 1e-10);
        Assert.assertEquals(sampler.samplesAt0, loaded.samplesAt0);
        Assert.assertEquals(sampler.depthN, loaded.depthN);
        Assert.assertEquals(sampler.samplesAtN, loaded.samplesAtN);
        Assert.assertEquals(sampler.samplesAtInf, loaded.samplesAtInf);
        Assert.assertEquals(sampler.forwardJump, loaded.forwardJump);
        Assert.assertEquals(sampler.backwardsJump, loaded.backwardsJump);
        Assert.assertEquals(sampler.backwardsJumpMin, loaded.backwardsJumpMin);
        Assert.assertEquals(sampler.backwardsJumpFailureMultiplier, loaded.backwardsJumpFailureMultiplier, 1e-10f);
    }

    @Test
    public void yamlSampler_Random() throws IOException {
        File file = File.createTempFile("samplerrandom", "yaml");
        file.deleteOnExit();

        Sampler_Random sampler = new Sampler_Random();

        SearchConfiguration.serializeToYaml(file, sampler);
        Assert.assertTrue(file.exists());

        Sampler_Random loaded = SearchConfiguration.deserializeYaml(file, Sampler_Random.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlSampler_UCB() throws IOException {
        File file = File.createTempFile("samplerucb", "yaml");
        file.deleteOnExit();

        Sampler_UCB sampler = new Sampler_UCB(new EvaluationFunction_Constant(1.6f),
                new RolloutPolicy_JustEvaluate(new EvaluationFunction_Constant(9.9f)), 5, 1);

        SearchConfiguration.serializeToYaml(file, sampler);
        Assert.assertTrue(file.exists());

        Sampler_UCB loaded = SearchConfiguration.deserializeYaml(file, Sampler_UCB.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(sampler.explorationConstant, loaded.explorationConstant, 1e-10f);
        Assert.assertEquals(sampler.explorationRandomFactor, loaded.explorationRandomFactor, 1e-10f);
        Assert.assertEquals(sampler.getEvaluationFunction().getValue(sampleNode1),
                loaded.getEvaluationFunction().getValue(sampleNode1), 1e-10f);
        Assert.assertEquals(sampler.getRolloutPolicy().rollout(sampleNode2, new GameUnified()),
                sampler.getRolloutPolicy().rollout(sampleNode2, new GameUnified()), 1e-10f);
    }

    @Test
    public void yamlHistogram_LeafDepth() throws IOException {
        File file = File.createTempFile("histodepth", "yaml");
        file.deleteOnExit();

        PanelHistogram_LeafDepth panel = new PanelHistogram_LeafDepth("myname");
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelHistogram_LeafDepth loaded = SearchConfiguration.deserializeYaml(file, PanelHistogram_LeafDepth.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
    }

    @Test
    public void yamlPie_ViableFutures() throws IOException {
        File file = File.createTempFile("piefuture", "yaml");
        file.deleteOnExit();

        PanelPie_ViableFutures panel = new PanelPie_ViableFutures("myname");
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelPie_ViableFutures loaded = SearchConfiguration.deserializeYaml(file, PanelPie_ViableFutures.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
    }

    @Test
    public void yamlRunner_Animated() throws IOException {
        File file = File.createTempFile("runneranimated", "yaml");
        file.deleteOnExit();

        PanelRunner_Animated panel = new PanelRunner_Animated("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_Animated loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_Animated.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);

        panel.deactivateTab();
    }

    @Test
    public void yamlRunner_AnimatedFromStates() throws IOException {
        File file = File.createTempFile("animatedfromstates", "yaml");
        file.deleteOnExit();

        PanelRunner_AnimatedFromStates panel = new PanelRunner_AnimatedFromStates("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_AnimatedFromStates loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_AnimatedFromStates.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);

        panel.deactivateTab();
    }

    @Test
    public void yamlRunner_AnimatedTranformed() throws IOException {
        File file = File.createTempFile("animatedtransformed", "yaml");
        file.deleteOnExit();

        PanelRunner_AnimatedTransformed panel = new PanelRunner_AnimatedTransformed("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_AnimatedTransformed loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_AnimatedTransformed.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);

        panel.deactivateTab();
    }

    @Test
    public void yamlRunner_Comparison() throws IOException {
        File file = File.createTempFile("runnercomparison", "yaml");
        file.deleteOnExit();

        PanelRunner_Comparison panel = new PanelRunner_Comparison("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.maxNumStatesToShow = 78;
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_Comparison loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_Comparison.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);
        Assert.assertEquals(panel.maxNumStatesToShow, loaded.maxNumStatesToShow);

        panel.deactivateTab();
    }

    @Test
    public void yamlRunner_MultiState() throws IOException {
        File file = File.createTempFile("runnermultistate", "yaml");
        file.deleteOnExit();

        PanelRunner_MultiState panel = new PanelRunner_MultiState("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.customStrokeExtra = new BasicStroke(202);
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_MultiState loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_MultiState.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);
        Assert.assertNotEquals(panel.customStrokeExtra, loaded.customStrokeExtra);

        panel.deactivateTab();
    }

    @Test
    public void yamlRunner_SimpleState() throws IOException {
        File file = File.createTempFile("runnersimplestate", "yaml");
        file.deleteOnExit();

        PanelRunner_Comparison panel = new PanelRunner_Comparison("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.maxNumStatesToShow = 78;
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_Comparison loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_Comparison.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);
        Assert.assertEquals(panel.maxNumStatesToShow, loaded.maxNumStatesToShow);

        panel.deactivateTab();
    }

    @Test
    public void yamlRunner_Snapshot() throws IOException {
        File file = File.createTempFile("runnersnapshot", "yaml");
        file.deleteOnExit();

        PanelRunner_Snapshot panel = new PanelRunner_Snapshot("myname");
        panel.yOffsetPixels++;
        panel.xOffsetPixels++;
        panel.customStroke = new BasicStroke(202);
        panel.numHistoryStatesDisplay++;
        panel.activateTab();

        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelRunner_Snapshot loaded = SearchConfiguration.deserializeYaml(file, PanelRunner_Snapshot.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertFalse(loaded.isActive());
        Assert.assertNotEquals(panel.xOffsetPixels, loaded.xOffsetPixels);
        Assert.assertNotEquals(panel.yOffsetPixels, loaded.yOffsetPixels);
        Assert.assertNotEquals(panel.customStroke, loaded.customStroke);
        Assert.assertEquals(panel.numHistoryStatesDisplay, loaded.numHistoryStatesDisplay);

        panel.deactivateTab();
    }

    @Test
    public void yamlPlot_Controls() throws IOException {
        File file = File.createTempFile("plotcontrols", "yaml");
        file.deleteOnExit();

        PanelPlot_Controls panel = new PanelPlot_Controls("myname", 7);
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelPlot_Controls loaded = SearchConfiguration.deserializeYaml(file, PanelPlot_Controls.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.numberOfPlots, loaded.numberOfPlots);
        Assert.assertEquals(panel.getName(), loaded.getName());
    }

    @Test
    public void yamlPlot_Simple() throws IOException {
        File file = File.createTempFile("plotsimple", "yaml");
        file.deleteOnExit();

        PanelPlot_Simple panel = new PanelPlot_Simple("myname");
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelPlot_Simple loaded = SearchConfiguration.deserializeYaml(file, PanelPlot_Simple.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.numberOfPlots, loaded.numberOfPlots);
        Assert.assertEquals(panel.getName(), loaded.getName());
    }

    @Test
    public void yamlPlot_SingleRun() throws IOException {
        File file = File.createTempFile("plotsingle", "yaml");
        file.deleteOnExit();

        PanelPlot_SingleRun panel = new PanelPlot_SingleRun("myname", 7);
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelPlot_SingleRun loaded = SearchConfiguration.deserializeYaml(file, PanelPlot_SingleRun.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.numberOfPlots, loaded.numberOfPlots);
        Assert.assertEquals(panel.getName(), loaded.getName());
    }

    @Test
    public void yamlPlot_States() throws IOException {
        File file = File.createTempFile("plotstates", "yaml");
        file.deleteOnExit();

        PanelPlot_States panel = new PanelPlot_States("myname", 5);
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelPlot_States loaded = SearchConfiguration.deserializeYaml(file, PanelPlot_States.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.numberOfPlots, loaded.numberOfPlots);
        Assert.assertEquals(panel.getName(), loaded.getName());
    }

    @Test
    public void yamlPlot_Transformed() throws IOException {
        File file = File.createTempFile("plottransformed", "yaml");
        file.deleteOnExit();

        PanelPlot_Transformed panel = new PanelPlot_Transformed(new Transform_Identity(), "myname", 7);
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelPlot_Transformed loaded = SearchConfiguration.deserializeYaml(file, PanelPlot_Transformed.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.numberOfPlots, loaded.numberOfPlots);
        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertTrue(loaded.transformer instanceof Transform_Identity);

        // TODO handle serializing the filters too.
    }

    @Test
    public void yamlTimeSeries_Worker() throws IOException {
        File file = File.createTempFile("plotworkerseries", "yaml");
        file.deleteOnExit();

        PanelTimeSeries_WorkerLoad panel = new PanelTimeSeries_WorkerLoad("myname", 7);
        panel.maxPtsPerPlot = 111;
        Assert.assertEquals("myname", panel.getName());

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelTimeSeries_WorkerLoad loaded = SearchConfiguration.deserializeYaml(file, PanelTimeSeries_WorkerLoad.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(panel.maxPtsPerPlot, loaded.maxPtsPerPlot);
        Assert.assertEquals(panel.getName(), loaded.getName());
        Assert.assertEquals(panel.getNumberOfPlots(), loaded.getNumberOfPlots());
    }

    @Test
    public void yamlPanel_Tree() throws IOException {
        File file = File.createTempFile("paneltree", "yaml");
        file.deleteOnExit();

        PanelTree panel = new PanelTree();

        SearchConfiguration.serializeToYaml(file, panel);
        Assert.assertTrue(file.exists());

        PanelTree loaded = SearchConfiguration.deserializeYaml(file, PanelTree.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlUI_Headless() throws IOException {
        File file = File.createTempFile("uiheadless", "yaml");
        file.deleteOnExit();

        UI_Headless ui = new UI_Headless();

        SearchConfiguration.serializeToYaml(file, ui);
        Assert.assertTrue(file.exists());

        UI_Headless loaded = SearchConfiguration.deserializeYaml(file, UI_Headless.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlUI_Full() throws IOException {
        File file = File.createTempFile("uiheadless", "yaml");
        file.deleteOnExit();

        UI_Full ui = new UI_Full();
        ui.addTab(new PanelPlot_States("myname", 4));

        SearchConfiguration.serializeToYaml(file, ui);
        Assert.assertTrue(file.exists());

        UI_Full loaded = SearchConfiguration.deserializeYaml(file, UI_Full.class);
        Assert.assertNotNull(loaded);

        Assert.assertEquals(1, loaded.getTabbedPanes().size());
        Assert.assertEquals(ui.getTabbedPanes().get(0).getName(), loaded.getTabbedPanes().get(0).getName());
        Assert.assertTrue(loaded.getTabbedPanes().get(0) instanceof PanelPlot_States);
        Assert.assertEquals(4, ((PanelPlot_States) loaded.getTabbedPanes().get(0)).numberOfPlots);
    }

    @Test
    public void yamlUpdaterAvg() throws IOException {
        File file = File.createTempFile("updateravg", "yaml");
        file.deleteOnExit();

        ValueUpdater_Average valUpdater = new ValueUpdater_Average();

        SearchConfiguration.serializeToYaml(file, valUpdater);
        Assert.assertTrue(file.exists());

        ValueUpdater_Average loaded = SearchConfiguration.deserializeYaml(file, ValueUpdater_Average.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlUpdaterHardSet() throws IOException {
        File file = File.createTempFile("updaterhardset", "yaml");
        file.deleteOnExit();

        ValueUpdater_HardSet valUpdater = new ValueUpdater_HardSet();

        SearchConfiguration.serializeToYaml(file, valUpdater);
        Assert.assertTrue(file.exists());

        ValueUpdater_HardSet loaded = SearchConfiguration.deserializeYaml(file, ValueUpdater_HardSet.class);
        Assert.assertNotNull(loaded);
    }

    @Test
    public void yamlUpdaterStdDev() throws IOException {
        File file = File.createTempFile("updaterstdev", "yaml");
        file.deleteOnExit();

        ValueUpdater_StdDev valUpdater = new ValueUpdater_StdDev();
        valUpdater.stdevAbove = 4.3f;

        SearchConfiguration.serializeToYaml(file, valUpdater);
        Assert.assertTrue(file.exists());

        ValueUpdater_StdDev loaded = SearchConfiguration.deserializeYaml(file, ValueUpdater_StdDev.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(valUpdater.stdevAbove, loaded.stdevAbove, 1e-10f);
    }

    @Test
    public void yamlUpdaterTopNChildren() throws IOException {
        File file = File.createTempFile("updatertopnchildren", "yaml");
        file.deleteOnExit();

        ValueUpdater_TopNChildren valUpdater = new ValueUpdater_TopNChildren(15);

        SearchConfiguration.serializeToYaml(file, valUpdater);
        Assert.assertTrue(file.exists());

        ValueUpdater_TopNChildren loaded = SearchConfiguration.deserializeYaml(file, ValueUpdater_TopNChildren.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(valUpdater.numChildrenToAvg, loaded.numChildrenToAvg);
    }
}