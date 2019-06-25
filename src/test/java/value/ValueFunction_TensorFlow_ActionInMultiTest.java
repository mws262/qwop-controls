//package value;
//
//import game.action.Action;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import tree.Node;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//
//public class ValueFunction_TensorFlow_ActionInMultiTest {
//
//    private static Node rootNode;
//
//    private static ValueFunction_TensorFlow_ActionInMulti valFun;
//
//    private static List<Integer> hiddenLayerSizes = new ArrayList<>();
//    private static List<Action> allActions = new ArrayList<>();
//
//    @BeforeClass
//    public static void setUp() {
//
//        // We are setting up game.action:
//        rootNode = ValueFunction_TensorFlow_ActionInTest.makeDemoTree();
//        hiddenLayerSizes.add(128);
//        hiddenLayerSizes.add(64);
//
//        allActions.addAll(ValueFunction_TensorFlow_ActionInTest.actionsLayer1);
//        allActions.addAll(ValueFunction_TensorFlow_ActionInTest.actionsLayer2);
//        allActions.addAll(ValueFunction_TensorFlow_ActionInTest.actionsLayer3);
//
//        try {
//            valFun = ValueFunction_TensorFlow_ActionInMulti.makeNew(allActions, "valfun_network_unit_test_multi_tmp",
//                    hiddenLayerSizes, new ArrayList<>());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        // Make sure that the graph files were created and that they will be cleaned up on exit.
//        File[] graphDefinitionFiles = valFun.getGraphDefinitionFiles();
//        for (File file : graphDefinitionFiles) {
//            Assert.assertTrue(file.exists());
//            file.deleteOnExit();
//        }
//    }
//
//    @Test
//    public void orderCheck() {
//        // Make sure at the various levels of the tree that we have the key combination we expect returned.
//        Action actionLayer1 = valFun.getMaximizingAction(rootNode);
//        Assert.assertArrayEquals(ValueFunction_TensorFlow_ActionInTest.actionsLayer1.get(0).peek(),
//                actionLayer1.peek());
//
//        Action actionLayer2 = valFun.getMaximizingAction(rootNode.getChildren()[0]);
//        Assert.assertArrayEquals(ValueFunction_TensorFlow_ActionInTest.actionsLayer2.get(0).peek(),
//                actionLayer2.peek());
//
//        Action actionLayer3 = valFun.getMaximizingAction(rootNode.getChildren()[0].getChildren()[0]);
//        Assert.assertArrayEquals(ValueFunction_TensorFlow_ActionInTest.actionsLayer3.get(0).peek(),
//                actionLayer3.peek());
//
//        Action actionLayer4 = valFun.getMaximizingAction(rootNode.getChildren()[0].getChildren()[0].getChildren()[0]);
//        Assert.assertArrayEquals(ValueFunction_TensorFlow_ActionInTest.actionsLayer1.get(0).peek(),
//                actionLayer4.peek());
//    }
//
//    @Test
//    public void updateEvaluateSaveLoad() {
//        String checkpointFilePrefix = "valfun_multi_test_ckpt";
//        String valueFunctionRemadeName = "valfun_network__multi_remade_tmp";
//
//        List<Node> nodes = new ArrayList<>();
//        rootNode.getNodesBelowInclusive(nodes, false);
//        valFun.setTrainingStepsPerBatch(1000);
//        valFun.setVerbose(false);
//        nodes.remove(rootNode);
//
//        // Evaluate before training.
//        float totalAbsErrorBefore = 0f;
//        for (Node node : nodes) {
//            totalAbsErrorBefore += Math.abs(node.getValue() - valFun.evaluate(node));
//        }
//
//        valFun.update(nodes);
//
//        // Evaluate after training.
//        float totalAbsErrorAfter = 0f;
//        for (Node node : nodes) {
//            totalAbsErrorAfter += Math.abs(node.getValue() - valFun.evaluate(node));
//        }
//        Assert.assertTrue("Error was not reduced by at least a factor of 2. Before: " + totalAbsErrorBefore +
//                " After: " + totalAbsErrorAfter, totalAbsErrorAfter < totalAbsErrorBefore/2f);
//
//        // Save.
//        List<File> checkpointFiles = valFun.saveCheckpoints(checkpointFilePrefix);
//        Assert.assertEquals(6, checkpointFiles.size());
//        checkpointFiles.forEach(File::deleteOnExit);
//
//        // Make sure loading the checkpoint works correctly.
//        // Make another value function.
//        ValueFunction_TensorFlow_ActionInMulti valFunRemade = null;
//
//        try {
//            valFunRemade = ValueFunction_TensorFlow_ActionInMulti.makeNew(allActions,
//                    valueFunctionRemadeName, hiddenLayerSizes);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        File[] newGraphFiles = Objects.requireNonNull(valFunRemade).getGraphDefinitionFiles();
//        for (File file : newGraphFiles) {
//            Assert.assertTrue(file.exists());
//            file.deleteOnExit();
//        }
//        valFunRemade.setVerbose(false);
//
//        // Evaluate before loading checkpoint.
//        float totalAbsErrorBeforeRemade = 0f;
//        for (Node node : nodes) {
//            totalAbsErrorBeforeRemade += Math.abs(node.getValue() - valFunRemade.evaluate(node));
//        }
//        Assert.assertNotEquals(totalAbsErrorAfter, totalAbsErrorBeforeRemade);
//
//        // Load the checkpoint and recalculate
//        valFunRemade.loadCheckpoints(checkpointFilePrefix);
//        float totalAbsErrorAfterRemade = 0f;
//        for (Node node : nodes) {
//            totalAbsErrorAfterRemade += Math.abs(node.getValue() - valFunRemade.evaluate(node));
//        }
//
//        Assert.assertEquals("Second value function should behave the same after loading the checkpoint file.",
//                totalAbsErrorAfter, totalAbsErrorAfterRemade, 1e-15f);
//    }
//
//    @Test
//    public void getMaximizingAction() {
//    }
//
//    @Test
//    public void evaluate() {
//    }
//
//    @Test
//    public void update() {
//    }
//
//    @Test
//    public void loadCheckpoints() {
//    }
//
//    @Test
//    public void saveCheckpoints() {
//    }
//
//    @Test
//    public void loadExisting() {
//    }
//
//    @Test
//    public void makeNew() {
//    }
//
//
//}