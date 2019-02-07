package tflowtools;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrainableNetworkTest {

    private List<Integer> layerSizes;

    private TrainableNetwork testNetwork;

    @Before
    public void setup() {
        layerSizes = new ArrayList<>();
        layerSizes.add(4);
        layerSizes.add(10);
        layerSizes.add(5);
        layerSizes.add(2);

        testNetwork = TrainableNetwork.makeNewNetwork("unit_test_graph", layerSizes);
        testNetwork.graphDefinition.deleteOnExit(); // Will remove the unit_test_graph.pb file after running.
    }

    @Test
    public void trainingStep() {

        float[][] inputs = new float[][] {
                {4.3f, 5.5f, -1.2f, 9f},
                {-3f, 5f, 9f, -3f},
                {-3.3f, 10f, -20f, 1f},
                {10f, 15f, 1f, 4f}
        };

        float[][] outputs = new float[][] {
                {1, -1},
                {1, -1},
                {1, -1},
                {1, -1}
        };

        float loss1 = testNetwork.trainingStep(inputs, outputs, 1); // Do a single step.
        Assert.assertTrue("Loss should be non-negative.", loss1 > 0f);

        float loss2 = testNetwork.trainingStep(inputs, outputs, 1000); // Do many steps.
        Assert.assertTrue("Loss should be better after many more steps.", loss1 > loss2);
        Assert.assertTrue("Loss should be very close to zero after many training steps. Was " + Math.abs(loss2) + ".",
                Math.abs(loss2) < 1e-4);
    }

    @Test
    public void saveCheckpoint() {
        testNetwork.saveCheckpoint("tmp_unit_test_ckpt");
        File checkpointPath = new File(testNetwork.checkpointPath);
        Assert.assertTrue(checkpointPath.exists());
        Assert.assertTrue(checkpointPath.isDirectory());
        File[] filesInCheckpointPath = checkpointPath.listFiles();

        // Should find two files: the <name>.data-.... and <name>.index.
        int foundFiles = 0;
        for (File f : filesInCheckpointPath) {
            if (f.getName().contains("tmp_unit_test_ckpt")) {
                f.deleteOnExit(); // Remove when done.
                foundFiles++;
            }
        }
        Assert.assertEquals(2, foundFiles);
    }

    @Test
    public void loadCheckpoint() {
        testNetwork.saveCheckpoint("tmp_unit_test_load_ckpt");

        TrainableNetwork networkForLoading = new TrainableNetwork(testNetwork.graphDefinition);
        networkForLoading.loadCheckpoint("tmp_unit_test_load_ckpt");

        float[][] inputs = new float[][] {
                {-20f, 15f, -50f, 90f},
        };

        float[][] outOld = testNetwork.evaluateInput(inputs);
        float[][] outNew = networkForLoading.evaluateInput(inputs);

        Assert.assertTrue("Old network and reloaded network should evaluate the same.", Arrays.equals(outOld[0],
                outNew[0]));
    }

    @Test
    public void evaluateInput() {
        // Single example in.
        float[][] singleInput = new float[][] {
                {10f, 10f, 10f, 10f}
        };
        float[][] singleOutput = testNetwork.evaluateInput(singleInput);

        Assert.assertEquals(1, singleOutput.length);
        Assert.assertEquals(2, singleOutput[0].length);

        // Multiple examples in.
        float[][] multiInput = new float[][] {
                {-100f, -100f, -100f, -100f},
                singleInput[0] // Same as before for second example.
        };

        float[][] multiOutput = testNetwork.evaluateInput(multiInput);

        Assert.assertEquals(2, multiOutput.length);
        Assert.assertEquals(2, multiOutput[0].length);

        Assert.assertTrue("Same evaluation should be replicable.", Arrays.equals(singleOutput[0], multiOutput[1]));
    }

    @Test
    public void makeNewNetwork() {}
}