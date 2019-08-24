package value;

import game.GameUnified;
import game.GameUnifiedCaching;
import game.IGameSerializable;
import game.action.Action;
import game.action.CommandQWOP;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ValueFunction_TensorFlowTest {

    @Test
    public void constructor1() {
        GameUnified game = new GameUnified();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(10);
        layerSizes.add(4);

        ValFunTest valFun = null;
        try {
            valFun = new ValFunTest("src/test/resources/test_net", game, 1, layerSizes, new ArrayList<>(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(valFun.getGraphDefinitionFile().getPath().contains("src/test/resources/test_net"));

        int[] actualLayerSizes = valFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(game.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(1, actualLayerSizes[3]);

        Assert.assertEquals(game.getStateDimension(), valFun.inputSize);
        Assert.assertEquals(1, valFun.outputSize);

        valFun.evaluate(new NodeQWOP<>(null)); // Just to make sure it doesn't error out. The value is basically
        // meaningless.

        Assert.assertNotNull(valFun.stateStats.getMean());
        Assert.assertNotNull(valFun.stateStats.getRange());
        Assert.assertNotNull(valFun.stateStats.getMin());
        Assert.assertNotNull(valFun.stateStats.getMax());
        Assert.assertNotNull(valFun.stateStats.getStdev());

        valFun.close();
        // Bigger output.
        int outputSize = 5;
        try {
            valFun = new ValFunTest("src/test/resources/test_net2", game, outputSize, layerSizes, new ArrayList<>(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(valFun.getGraphDefinitionFile().getPath().contains("src/test/resources/test_net2"));

        actualLayerSizes = valFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(game.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(outputSize, actualLayerSizes[3]);

        Assert.assertEquals(game.getStateDimension(), valFun.inputSize);
        Assert.assertEquals(outputSize, valFun.outputSize);

        valFun.evaluate(new NodeQWOP<>(null)); // Just to make sure it doesn't error out. The value is basically

        valFun.close();
        // Bigger input due to delay-embedded version of the game.
        outputSize = 2;
        game = new GameUnifiedCaching(1, 3, GameUnifiedCaching.StateType.HIGHER_DIFFERENCES);
        try {
            valFun = new ValFunTest("src/test/resources/test_net3", game, outputSize, layerSizes, new ArrayList<>(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(valFun.getGraphDefinitionFile().getPath().contains("src/test/resources/test_net3"));

        actualLayerSizes = valFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(game.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(outputSize, actualLayerSizes[3]);

        Assert.assertEquals(game.getStateDimension(), valFun.inputSize);
        Assert.assertEquals(outputSize, valFun.outputSize);

        valFun.evaluate(new NodeQWOP<>(null)); // Just to make sure it doesn't error out. The value is basically
        valFun.close();
    }

    @Test
    public void constructor2() throws IOException {
        // Constructor 1 to create from scratch.
        ValFunTest valFun = null;
        GameUnified game = new GameUnified();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(10);
        layerSizes.add(4);

        int outputSize = 1;
        try {
            valFun = new ValFunTest("src/test/resources/test_net4", game, outputSize, layerSizes, new ArrayList<>(), "");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        List<File> checkpoint;
        checkpoint = valFun.saveCheckpoint("src/test/resources/test_checkpoint1");
        checkpoint.forEach(File::deleteOnExit);

        File netFile = valFun.getGraphDefinitionFile();

        // Load existing graph with constructor 2.
        ValFunTest valFunLoad = null;
        try {
            valFunLoad = new ValFunTest(netFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFunLoad.getGraphDefinitionFile());

        // Input/output sizes should still match.
        Assert.assertEquals(valFun.getGraphDefinitionFile(), valFunLoad.getGraphDefinitionFile());
        Assert.assertEquals(valFun.inputSize, valFunLoad.inputSize);
        Assert.assertEquals(valFun.outputSize, valFunLoad.outputSize);

        Assert.assertArrayEquals(valFun.assembleInputFromNode(new NodeQWOP<>(null)),
                valFunLoad.assembleInputFromNode(new NodeQWOP<>(null)), 1e-8f);
        Assert.assertArrayEquals(valFun.assembleOutputFromNode(new NodeQWOP<>(null)),
                valFunLoad.assembleOutputFromNode(new NodeQWOP<>(null)), 1e-8f);

        // Checkpoint loading should produce the same results.
        float oldNetEval = valFun.evaluate(new NodeQWOP<>(null));
        Assert.assertNotEquals(oldNetEval, valFunLoad.evaluate(new NodeQWOP<>(null)), 1e-12f);
        valFunLoad.loadCheckpoint("src/test/resources/test_checkpoint1");
        float newNetEval = valFunLoad.evaluate(new NodeQWOP<>(null));
        Assert.assertEquals(oldNetEval, newNetEval, 1e-12f);

        List<NodeQWOP<CommandQWOP>> updateList = new ArrayList<>();
        updateList.add(new NodeQWOP<>(null));
        valFun.update(updateList);
        Assert.assertNotEquals(oldNetEval, valFun.evaluate(new NodeQWOP<>(null))); // Should be different after update.
        Assert.assertEquals(newNetEval, valFunLoad.evaluate(new NodeQWOP<>(null)), 1e-12f); // Update of one net should
        // not affect the other.

        ValFunTest valFunCheckpointConstructor = null;
        try {
            valFunCheckpointConstructor = new ValFunTest("src/test/resources/test_net4", game, outputSize, layerSizes,
                    new ArrayList<>(), "src/test/resources/test_checkpoint1");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(valFunLoad.evaluate(new NodeQWOP<>(null)),
                valFunCheckpointConstructor.evaluate(new NodeQWOP<>(null)), 1e-10f);
        valFun.close();
        valFunLoad.close();
        valFunCheckpointConstructor.close();
    }

    // Testing stubs.
    class ValFunTest extends ValueFunction_TensorFlow<CommandQWOP> {

        ValFunTest(String fileName, GameUnified gameTemplate, int outputSize, List<Integer> hiddenLayerSizes,
                   List<String> additionalArgs, String checkpoint) throws IOException {
            super(fileName,
                    gameTemplate.getStateDimension(),
                    outputSize,
                    hiddenLayerSizes,
                    additionalArgs,
                    checkpoint,
                    false);
        }

        ValFunTest(File existingFile) throws FileNotFoundException {
            super(existingFile, false);
        }

        @Override
        float[] assembleInputFromNode(NodeQWOPBase node) {
            return new float[inputSize];
        }

        @Override
        float[] assembleOutputFromNode(NodeQWOPBase node) {
            return new float[outputSize];
        }

        @Override
        public Action<CommandQWOP> getMaximizingAction(NodeQWOPBase<?, CommandQWOP> currentNode) {
            return null;
        }

        @Override
        public Action<CommandQWOP> getMaximizingAction(NodeQWOPBase<?, CommandQWOP> currentNode,
                                                       IGameSerializable<CommandQWOP> game) {
            return null;
        }

        @Override
        public IValueFunction<CommandQWOP> getCopy() {
            return null; // todo
        }
    }
}