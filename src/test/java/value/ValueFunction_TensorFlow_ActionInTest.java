package value;

import actions.Action;
import actions.ActionQueue;
import game.GameSingleThread;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tree.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ValueFunction_TensorFlow_ActionInTest {


    private static GameSingleThread game = GameSingleThread.getInstance();

    private static ValueFunction_TensorFlow_ActionIn valFun;

    private static Node rootNode;

    @BeforeClass
    public static void setUp() {
        // Manually make a dummy tree to run on.
        rootNode = new Node();
        rootNode.setValue(rootNode.getState().body.getY());
        rootNode.visitCount.getAndIncrement();

        // First layer actions.
        Action a1_0 = new Action(5, false, false, false, false),
                a1_1 = new Action(6, false, false, false, false),
                a1_2 = new Action(7, false, false, false, false),
                a1_3 = new Action(8, false, false, false, false);
        List<Action> actionsLayer1 = new ArrayList<>();
        actionsLayer1.add(a1_0);
        actionsLayer1.add(a1_1);
        actionsLayer1.add(a1_2);
        actionsLayer1.add(a1_3);

        // Second layer actions.
        Action a2_0 = new Action(5, false, true, true, false),
                a2_1 = new Action(6, false, true, true, false),
                a2_2 = new Action(7, false, true, true, false),
                a2_3 = new Action(8, false, true, true, false);
        List<Action> actionsLayer2 = new ArrayList<>();
        actionsLayer2.add(a2_0);
        actionsLayer2.add(a2_1);
        actionsLayer2.add(a2_2);
        actionsLayer2.add(a2_3);

        // Third layer actions.
        Action a3_0 = new Action(5, false, false, false, false),
                a3_1 = new Action(6, false, false, false, false),
                a3_2 = new Action(7, false, false, false, false),
                a3_3 = new Action(8, false, false, false, false);
        List<Action> actionsLayer3 = new ArrayList<>();
        actionsLayer3.add(a3_0);
        actionsLayer3.add(a3_1);
        actionsLayer3.add(a3_2);
        actionsLayer3.add(a3_3);

        for (Action action1 : actionsLayer1) {
            for (Action action2 : actionsLayer2) {
                for (Action action3 : actionsLayer3) {
                    Node currentNode = rootNode;
                    currentNode = doNext(currentNode, action1);
                    currentNode = doNext(currentNode, action2);
                    doNext(currentNode, action3);
                    game.makeNewWorld();
                }
            }
        }

        // Do various verifications that we built the tree correctly.
        List<Node> allNodes = new ArrayList<>();
        rootNode.getNodesBelow(allNodes, false);
        Assert.assertEquals(actionsLayer1.size() * actionsLayer2.size() * actionsLayer3.size()
                        + actionsLayer1.size() * actionsLayer2.size()
                        + actionsLayer1.size() + 1,
                allNodes.size());

        for (Node n : allNodes) {
            Assert.assertNotEquals(0f, n.getValue());
            Assert.assertNotEquals(0L, n.visitCount.longValue());
        }

        // Test makeNew here because it must happen first.
        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(128);
        hiddenLayerSizes.add(64);
        try {
            valFun = ValueFunction_TensorFlow_ActionIn.makeNew("valfun_network_unit_test_tmp", hiddenLayerSizes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(valFun.getGraphDefinitionFile().exists());
        valFun.getGraphDefinitionFile().deleteOnExit();
        valFun.verbose = false;
    }

    @Test
    public void getMaximizingAction() {
        Node currentNode = rootNode;
        while (currentNode.getChildCount() > 0) { // Keep going until there are no children to choose from.
            Action bestAction = valFun.getMaximizingAction(currentNode);
            Node[] children = currentNode.getChildren();
            Assert.assertTrue(children.length > 0);

            // Do the comparison for each.
            Node bestChoice = Arrays.stream(children).max(Comparator.comparing(valFun::evaluate)).orElse(null);
            Assert.assertNotNull(bestChoice);

            Assert.assertEquals(bestAction, bestChoice.getAction());
            currentNode = bestChoice;
        }
    }


    @Test
    public void updateAndEvaluateAndSave() {
        valFun.trainingStepsPerBatch = 1000; // Going to run this many training steps on the same bunch of nodes.
        List<Node> allNodes = new ArrayList<>();
        rootNode.getNodesBelow(allNodes, true);
        allNodes.remove(rootNode); // Can't evaluate root under normal circumstances.

        // Evaluate before training.
        float totalAbsErrorBefore = 0f;
        for (Node allNode : allNodes) {
            totalAbsErrorBefore += Math.abs(allNode.getValue() - valFun.evaluate(allNode));
        }

        // Run training.
        valFun.update(allNodes);

        // Evaluate after training.
        float totalAbsErrorAfter = 0f;
        for (Node allNode : allNodes) {
            totalAbsErrorAfter += Math.abs(allNode.getValue() - valFun.evaluate(allNode));
        }
        Assert.assertTrue("Error was not reduced by at least a factor of 2. Before: " + totalAbsErrorBefore +
                " After: " + totalAbsErrorAfter, totalAbsErrorAfter < totalAbsErrorBefore/2f);

        // Save.
        valFun.saveCheckpoint("valfun_test_ckpt");
        File checkpointPath = valFun.getCheckpointPath();
        Assert.assertTrue(checkpointPath.exists());
        Assert.assertTrue(checkpointPath.isDirectory());
        File[] filesInCheckpointPath = checkpointPath.listFiles();

        // Should find two files: the <name>.data-.... and <name>.index.
        int foundFiles = 0;
        for (File f : Objects.requireNonNull(filesInCheckpointPath)) {
            if (f.getName().contains("valfun_test_ckpt")) {
                f.deleteOnExit(); // Remove when done.
                foundFiles++;
            }
        }
        Assert.assertEquals("Saving the checkpoint should result in 2 files.", 2, foundFiles);


        // Make sure loading the checkpoint works correctly.
        // Make another value function.
        ValueFunction_TensorFlow_ActionIn valFunRemade = null;
        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(128);
        hiddenLayerSizes.add(64);
        try {
            valFunRemade = ValueFunction_TensorFlow_ActionIn.makeNew("valfun_network_unit_test_tmp2", hiddenLayerSizes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(Objects.requireNonNull(valFunRemade).getGraphDefinitionFile().exists());
        valFunRemade.getGraphDefinitionFile().deleteOnExit();
        valFunRemade.verbose = false;

        // Evaluate before loading checkpoint.
        float totalAbsErrorBeforeRemade = 0f;
        for (Node allNode : allNodes) {
            totalAbsErrorBeforeRemade += Math.abs(allNode.getValue() - valFunRemade.evaluate(allNode));
        }
        Assert.assertNotEquals(totalAbsErrorAfter, totalAbsErrorBeforeRemade);

        // Load the checkpoint and recalculate
        valFunRemade.loadCheckpoint("valfun_test_ckpt");
        float totalAbsErrorAfterRemade = 0f;
        for (Node allNode : allNodes) {
            totalAbsErrorAfterRemade += Math.abs(allNode.getValue() - valFunRemade.evaluate(allNode));
        }

        Assert.assertEquals("Second value function should behave the same after loading the checkpoint file.",
                totalAbsErrorAfter, totalAbsErrorAfterRemade, 1e-15f);


    }

    /**
     * Just a convenience method for setting up the test tree. Does not make sure that the game is in the proper
     * state! This needs to be done externally. A value is arbitrarily assigned to the new node.
     *
     * @param currentNode Node we're starting at.
     * @param action      Action to add below this node.
     * @return A new child node with state and value assigned.
     */
    private static Node doNext(Node currentNode, Action action) {
        Node[] children = currentNode.getChildren();

        // Check if a node for the desired action already exists.
        boolean alreadyExists = false;
        Node nextNode = null;
        for (Node child : children) {
            if (child.getAction().equals(action)) {
                alreadyExists = true;
                nextNode = child;
                break;
            }
        }

        if (!alreadyExists) {
            nextNode = currentNode.addChild(action);
        }

        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(action);

        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }

        if (!alreadyExists) {
            nextNode.setState(game.getCurrentState());
            nextNode.setValue(nextNode.getState().body.getY());
            nextNode.visitCount.getAndIncrement();
        }
        return nextNode;
    }
}