package tflowtools;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// IF this fails, there's a good chance you don't have python tensorflow installed. Or, "python" calls the
// wrong version of python for your tensorflow install. The TensorFlow version in the pom.xml should match the
// version installed to python with pip.
public class TrainableNetworkTest {

    private static TrainableNetwork testNetwork;

    @BeforeClass
    public static void setUp() {
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(4);
        layerSizes.add(10);
        layerSizes.add(5);
        layerSizes.add(2);

        try {
            testNetwork = TrainableNetwork.makeNewNetwork("unit_test_graph", layerSizes, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        testNetwork.getGraphDefinitionFile().deleteOnExit(); // Will remove the unit_test_graph.pb file after running.
    }

    @AfterClass
    public static void tearDown() {
        if (testNetwork != null)
            testNetwork.close();
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

        float loss2 = testNetwork.trainingStep(inputs, outputs, 5000); // Do many steps.
        Assert.assertTrue("Loss should be better after many more steps.", loss1 > loss2);
        Assert.assertTrue("Loss should be very close to zero after many training steps. Was " + Math.abs(loss2) + ".",
                Math.abs(loss2) < 1e-3);

        float[][] outEval = testNetwork.evaluateInput(inputs);

        Assert.assertEquals("Network evaluation didn't come up with a matching result.", outEval[0][1], outputs[0][1]
                , 1e-3);
    }

    @Test
    public void saveCheckpoint() throws IOException {
        String filePrefix = "src/test/resources/tmp_unit_test_ckpt";
        testNetwork.saveCheckpoint(filePrefix);
        int numFiles = flagCheckpointForRemoval(filePrefix);
        Assert.assertEquals(2, numFiles);
    }

    @Test
    public void loadCheckpoint() throws IOException {
        String filePrefix = "src/test/resources/tmp_unit_test_load_ckpt";
        testNetwork.saveCheckpoint(filePrefix);
        int numFiles = flagCheckpointForRemoval(filePrefix);
        Assert.assertEquals(2, numFiles);

        TrainableNetwork networkForLoading = null;
        try {
            networkForLoading = new TrainableNetwork(testNetwork.getGraphDefinitionFile(), false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert networkForLoading != null;
        networkForLoading.loadCheckpoint("src/test/resources/tmp_unit_test_load_ckpt");

        float[][] inputs = new float[][] {
                {-20f, 15f, -50f, 90f},
        };

        float[][] outOld = testNetwork.evaluateInput(inputs);
        float[][] outNew = networkForLoading.evaluateInput(inputs);

        Assert.assertArrayEquals("Old network and reloaded network should evaluateActionDistribution the same.", outOld[0], outNew[0],
                0.0f);

        networkForLoading.close();
    }

    /**
     * Make sure that unit test checkpoint files don't stick around and create garbage.
     * @param fileContains Match for the first part of the filename. TensorFlow appends lots of garbage to the end of
     *                    it that we don't want to deal with.
     * @return Number of matching files flagged for deletion
     */
    private int flagCheckpointForRemoval(String fileContains) {
        File checkpointPath = new File("src/test/resources/");
        Assert.assertTrue(checkpointPath.exists());
        Assert.assertTrue(checkpointPath.isDirectory());
        File[] filesInCheckpointPath = checkpointPath.listFiles();

        // Should find two files: the <name>.data-.... and <name>.index.
        int foundFiles = 0;
        for (File f : Objects.requireNonNull(filesInCheckpointPath)) {
            if (f.getPath().contains(fileContains)) {
                f.deleteOnExit(); // Remove when done.
                foundFiles++;
            }
        }
        return foundFiles;
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

        Assert.assertArrayEquals("Same evaluation should be replicable.", singleOutput[0], multiOutput[1], 0.0f);
    }

    @Test
    public void getNumberOfOperationOutputs() {
        Assert.assertEquals(1, testNetwork.getNumberOfOperationOutputs("output"));
        Assert.assertEquals(1, testNetwork.getNumberOfOperationOutputs("loss"));
        // TODO if I ever use multi-output operations, add another test.
    }

    @Test
    public void getShapeOfOperationOutput() {
        int[] inputShape = testNetwork.getShapeOfOperationOutput("input", 0);
        Assert.assertArrayEquals("Input shape should match expected.", new int[]{-1, 4}, inputShape);

        int[] outputShape = testNetwork.getShapeOfOperationOutput("output", 0);
        Assert.assertArrayEquals("Output shape should match expected.", new int[]{-1, 2}, outputShape);

        int[] fullyConnected0Shape = testNetwork.getShapeOfOperationOutput("fully_connected0/weights/weight", 0);
        Assert.assertArrayEquals("Intermediate weight layer shape should match expected.", new int[]{4, 10},
                fullyConnected0Shape);

    }

    @Test
    public void getLayerSizes() {
        int[] layerSizes = testNetwork.getLayerSizes();
        Assert.assertArrayEquals("Reported layer sizes do not match expected.", new int[]{4, 10, 5, 2}, layerSizes);
    }
}