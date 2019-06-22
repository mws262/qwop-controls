package value;

import game.GameUnified;
import game.GameUnifiedCaching;
import game.IGameSerializable;
import game.action.Action;
import org.junit.Assert;
import org.junit.Test;
import tflowtools.TrainableNetwork;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ValueFunction_TensorFlowTest {

    @Test
    public void loadCheckpoint() {
    }

    @Test
    public void saveCheckpoint() {
    }

    @Test
    public void getCheckpointPath() {
    }

    @Test
    public void getGraphDefinitionFile() {
    }

    @Test
    public void setTrainingBatchSize() {
    }

    @Test
    public void setTrainingStepsPerBatch() {
    }

    @Test
    public void update() {
    }

    @Test
    public void evaluate() {
    }


    @Test
    public void constructor1() {
        GameUnified game = new GameUnified();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(10);
        layerSizes.add(4);

        ValFunTest valFun = null;
        try {
            valFun = new ValFunTest("test_net", game, 1, layerSizes, new ArrayList<>());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.network.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(valFun.getGraphDefinitionFile().getName().contains("test_net"));

        int[] actualLayerSizes = valFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(game.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(1, actualLayerSizes[3]);

        Assert.assertEquals(game.getStateDimension(), valFun.inputSize);
        Assert.assertEquals(1, valFun.outputSize);

        valFun.evaluate(new NodeQWOP(null)); // Just to make sure it doesn't error out. The value is basically
        // meaningless.

        Assert.assertNotNull(valFun.stateStats.getMean());
        Assert.assertNotNull(valFun.stateStats.getRange());
        Assert.assertNotNull(valFun.stateStats.getMin());
        Assert.assertNotNull(valFun.stateStats.getMax());
        Assert.assertNotNull(valFun.stateStats.getStdev());

        // Bigger output.
        int outputSize = 5;
        try {
            valFun = new ValFunTest("test_net2", game, outputSize, layerSizes, new ArrayList<>());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.network.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(valFun.getGraphDefinitionFile().getName().contains("test_net2"));

        actualLayerSizes = valFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(game.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(outputSize, actualLayerSizes[3]);

        Assert.assertEquals(game.getStateDimension(), valFun.inputSize);
        Assert.assertEquals(outputSize, valFun.outputSize);

        valFun.evaluate(new NodeQWOP(null)); // Just to make sure it doesn't error out. The value is basically

        // Bigger input due to delay-embedded version of the game.
        outputSize = 2;
        game = new GameUnifiedCaching(1, 3);
        try {
            valFun = new ValFunTest("test_net3", game, outputSize, layerSizes, new ArrayList<>());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(valFun);
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.network.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(valFun.getGraphDefinitionFile().getName().contains("test_net3"));

        actualLayerSizes = valFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(game.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(outputSize, actualLayerSizes[3]);

        Assert.assertEquals(game.getStateDimension(), valFun.inputSize);
        Assert.assertEquals(outputSize, valFun.outputSize);

        valFun.evaluate(new NodeQWOP(null)); // Just to make sure it doesn't error out. The value is basically
    }

    // Testing stubs.
    class ValFunTest extends ValueFunction_TensorFlow {

        ValFunTest(String fileName, GameUnified gameTemplate, int outputSize, List<Integer> hiddenLayerSizes, List<String> additionalArgs) throws FileNotFoundException {
            super(fileName, gameTemplate, outputSize, hiddenLayerSizes, additionalArgs);
        }

        ValFunTest(File existingFile, GameUnified gameTemplate) throws FileNotFoundException {
            super(existingFile, gameTemplate);
        }

        ValFunTest(TrainableNetwork network, GameUnified gameTemplate) {
            super(network, gameTemplate);
        }

        @Override
        float[] assembleInputFromNode(NodeQWOPBase<?> node) {
            return new float[inputSize];
        }

        @Override
        float[] assembleOutputFromNode(NodeQWOPBase<?> node) {
            return new float[outputSize];
        }

        @Override
        public Action getMaximizingAction(NodeQWOPBase<?> currentNode) {
            return null;
        }

        @Override
        public Action getMaximizingAction(NodeQWOPBase<?> currentNode, IGameSerializable game) {
            return null;
        }
    }
}