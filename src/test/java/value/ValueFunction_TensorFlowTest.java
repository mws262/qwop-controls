package value;

import game.IGameInternal;
import game.IGameSerializable;
import game.action.Action;
import game.qwop.*;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeGame;
import tree.node.NodeGameBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ValueFunction_TensorFlowTest {

    @Test
    public void constructor1() {
        IGameInternal<CommandQWOP, StateQWOP> game = new GameQWOP();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(10);
        layerSizes.add(4);

        ValFunTest<StateQWOP> valFun = null;
        try {
            valFun = new ValFunTest<>("src/test/resources/test_net", game, 1, layerSizes, new ArrayList<>(), "");
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

        valFun.evaluate(new NodeGame<>(GameQWOP.getInitialState())); // Just to make sure it doesn't error out. The value is basically
        // meaningless.

//        Assert.assertNotNull(valFun.stateStats.getMean());
//        Assert.assertNotNull(valFun.stateStats.getRange());
//        Assert.assertNotNull(valFun.stateStats.getMin());
//        Assert.assertNotNull(valFun.stateStats.getMax());
//        Assert.assertNotNull(valFun.stateStats.getStdev());

        valFun.close();
        // Bigger output.
        int outputSize = 5;
        try {
            valFun = new ValFunTest<>("src/test/resources/test_net2", game, outputSize, layerSizes, new ArrayList<>(), "");
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

        valFun.evaluate(new NodeGame<>(GameQWOP.getInitialState())); // Just to make sure it doesn't error out. The value is basically

        valFun.close();
        // Bigger input due to delay-embedded version of the game.
        ValFunTest<StateQWOPDelayEmbedded_HigherDifferences> diffValFun = null;
        outputSize = 2;
        GameQWOPCaching<StateQWOPDelayEmbedded_HigherDifferences> diffGame = new GameQWOPCaching<>(1, 3,
                GameQWOPCaching.StateType.HIGHER_DIFFERENCES);
        try {
            diffValFun = new ValFunTest<>("src/test/resources/test_net3",
                    diffGame,
                    outputSize,
                    layerSizes,
                    new ArrayList<>(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(diffValFun);
        Assert.assertTrue(diffValFun.getGraphDefinitionFile().exists());
        diffValFun.getGraphDefinitionFile().deleteOnExit(); // Try not to clog files up with tests.

        Assert.assertTrue(diffValFun.getGraphDefinitionFile().getPath().contains("src/test/resources/test_net3"));

        actualLayerSizes = diffValFun.network.getLayerSizes();

        // Input + output + hidden layers.
        Assert.assertEquals(4, actualLayerSizes.length);
        Assert.assertEquals(diffGame.getStateDimension(), actualLayerSizes[0]);
        Assert.assertEquals(layerSizes.get(0).intValue(), actualLayerSizes[1]);
        Assert.assertEquals(layerSizes.get(1).intValue(), actualLayerSizes[2]);
        Assert.assertEquals(outputSize, actualLayerSizes[3]);

        Assert.assertEquals(diffGame.getStateDimension(), diffValFun.inputSize);
        Assert.assertEquals(outputSize, diffValFun.outputSize);

        diffValFun.evaluate(new NodeGame<>(diffGame.getCurrentState())); // Just to make sure it doesn't error out. The value is
        // basically
        diffValFun.close();
    }

    @Test
    public void constructor2() throws IOException {
        // Constructor 1 to create from scratch.
        ValFunTest<StateQWOP> valFun = null;
        GameQWOP game = new GameQWOP();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(10);
        layerSizes.add(4);

        int outputSize = 1;
        try {
            valFun = new ValFunTest<>("src/test/resources/test_net4", game, outputSize, layerSizes, new ArrayList<>(),
                    "");
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
        ValFunTest<StateQWOP> valFunLoad = null;
        try {
            valFunLoad = new ValFunTest<>(netFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //noinspection ConstantConditions
        Assert.assertNotNull(valFunLoad.getGraphDefinitionFile());

        // Input/output sizes should still match.
        Assert.assertEquals(valFun.getGraphDefinitionFile(), valFunLoad.getGraphDefinitionFile());
        Assert.assertEquals(valFun.inputSize, valFunLoad.inputSize);
        Assert.assertEquals(valFun.outputSize, valFunLoad.outputSize);

        Assert.assertArrayEquals(valFun.assembleInputFromNode(new NodeGame<>(GameQWOP.getInitialState())),
                valFunLoad.assembleInputFromNode(new NodeGame<>(GameQWOP.getInitialState())), 1e-8f);
        Assert.assertArrayEquals(valFun.assembleOutputFromNode(new NodeGame<>(GameQWOP.getInitialState())),
                valFunLoad.assembleOutputFromNode(new NodeGame<>(GameQWOP.getInitialState())), 1e-8f);

        // Checkpoint loading should produce the same results.
        float oldNetEval = valFun.evaluate(new NodeGame<>(GameQWOP.getInitialState()));
        Assert.assertNotEquals(oldNetEval, valFunLoad.evaluate(new NodeGame<>(GameQWOP.getInitialState())), 1e-12f);
        valFunLoad.loadCheckpoint("src/test/resources/test_checkpoint1");
        float newNetEval = valFunLoad.evaluate(new NodeGame<>(GameQWOP.getInitialState()));
        Assert.assertEquals(oldNetEval, newNetEval, 1e-12f);

        List<NodeGame<CommandQWOP, StateQWOP>> updateList = new ArrayList<>();
        updateList.add(new NodeGame<>(GameQWOP.getInitialState()));
        valFun.update(updateList);
        Assert.assertNotEquals(oldNetEval, valFun.evaluate(new NodeGame<>(GameQWOP.getInitialState()))); // Should be different after update.
        Assert.assertEquals(newNetEval, valFunLoad.evaluate(new NodeGame<>(GameQWOP.getInitialState())), 1e-12f); // Update of one net should
        // not affect the other.

        ValFunTest<StateQWOP> valFunCheckpointConstructor = null;
        try {
            valFunCheckpointConstructor = new ValFunTest<>("src/test/resources/test_net4", game, outputSize, layerSizes,
                    new ArrayList<>(), "src/test/resources/test_checkpoint1");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(valFunLoad.evaluate(new NodeGame<>(GameQWOP.getInitialState())),
                valFunCheckpointConstructor.evaluate(new NodeGame<>(GameQWOP.getInitialState())), 1e-10f);
        valFun.close();
        valFunLoad.close();
        valFunCheckpointConstructor.close();
    }

    // Testing stubs.
    class ValFunTest<S extends IStateQWOP> extends ValueFunction_TensorFlow<CommandQWOP, S> {

        ValFunTest(String fileName, IGameInternal<CommandQWOP, S> gameTemplate, int outputSize,
                   List<Integer> hiddenLayerSizes,
                   List<String> additionalArgs, String checkpoint) throws IOException {
            super(fileName,
                    gameTemplate.getStateDimension(),
                    outputSize,
                    hiddenLayerSizes,
                    additionalArgs,
                    checkpoint,
                    1f,
                    false);
        }

        ValFunTest(File existingFile) throws FileNotFoundException {
            super(existingFile, 1f, false);
        }

        @Override
        float[] assembleInputFromNode(NodeGameBase node) {
            return new float[inputSize];
        }

        @Override
        float[] assembleOutputFromNode(NodeGameBase node) {
            return new float[outputSize];
        }

        @Override
        public Action<CommandQWOP> getMaximizingAction(NodeGameBase<?, CommandQWOP, S> currentNode) {
            return null;
        }

        @Override
        public Action<CommandQWOP> getMaximizingAction(NodeGameBase<?, CommandQWOP, S> currentNode,
                                                       IGameSerializable<CommandQWOP, S> game) {
            return null;
        }

        @Override
        public IValueFunction<CommandQWOP, S> getCopy() {
            return null; // todo
        }
    }
}