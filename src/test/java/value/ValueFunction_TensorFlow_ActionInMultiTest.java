package value;

import actions.Action;
import actions.ActionGenerator_FixedSequence;
import actions.ActionQueue;
import actions.ActionSet;
import distributions.Distribution_Equal;
import game.GameSingleThread;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tree.Node;

import java.util.stream.IntStream;

public class ValueFunction_TensorFlow_ActionInMultiTest {

    private static Node rootNode;

    @BeforeClass
    public static void setUp() {

        // Manually make a small tree to test on. This has three key combinations, repeated. The tree is fairly
        // shallow, so we get root->1->2->3->1. One repeated so we can test whether that works.
//
//        // None.
//        ActionSet actionSet1 = ActionSet.makeActionSet(IntStream.range(1, 4).toArray(), new boolean[]{false, false,
//                false, false}, new Distribution_Equal());
//        // WO
//        ActionSet actionSet2 = ActionSet.makeActionSet(IntStream.range(4, 8).toArray(), new boolean[]{false, true,
//                true, false}, new Distribution_Equal());
//        // QP
//        ActionSet actionSet3 = ActionSet.makeActionSet(IntStream.range(8, 12).toArray(), new boolean[]{true, false,
//                false, true}, new Distribution_Equal());
//
//        ActionSet[] repeatedActions = new ActionSet[]{actionSet1, actionSet2, actionSet3};
//
//        //Node.potentialActionGenerator = new ActionGenerator_FixedSequence(repeatedActions);
//
//        GameSingleThread game = GameSingleThread.getInstance();
//        ActionQueue queue = new ActionQueue();
//
//        rootNode = new Node();
//
//        while (!rootNode.fullyExplored.get()) {
//            Node currentNode = rootNode;
//
//            // Do some runs to the edge of the horizon we care about for these tests.
//            while (currentNode.getTreeDepth() < 4) {
//                Action nextAction = null;
//                boolean nodeAlreadyExists = true;
//                if (currentNode.uncheckedActions.isEmpty()) {
//                    for (Node child : currentNode.getChildren()) {
//                        if (!child.fullyExplored.get()) {
//                            nextAction = child.getAction();
//                            currentNode = child;
//                            break;
//                        }
//                    }
//                } else {
//                    nextAction = currentNode.uncheckedActions.getRandom();
//                    currentNode = currentNode.addChild(nextAction);
//                    nodeAlreadyExists = false;
//                }
//                queue.addAction(nextAction);
//                while (!queue.isEmpty()) {
//                    game.step(queue.pollCommand());
//                }
//                if (!nodeAlreadyExists) {
//                    currentNode.setState(game.getCurrentState());
//                    currentNode.setValue(currentNode.getState().body.getY()); // Arbitrary choice of value.
//                    currentNode.visitCount.getAndIncrement();
//                }
//            }
//            currentNode.fullyExplored.set(true);
//            currentNode.uncheckedActions.clear();
//            currentNode.propagateFullyExploredStatus_lite();
//            game.makeNewWorld();
//        }
//
//        Assert.assertEquals(actionSet1.size() * actionSet2.size() * actionSet3.size() * actionSet1.size()
//                + actionSet1.size() * actionSet2.size() * actionSet3.size()
//                + actionSet1.size() * actionSet2.size()
//                + actionSet1.size(),
//                rootNode.countDescendants());
//
//        game.releaseGame();
    }

    @Test
    public void getMaximizingAction() {
    }

    @Test
    public void evaluate() {
    }

    @Test
    public void update() {
    }

    @Test
    public void loadCheckpoints() {
    }

    @Test
    public void saveCheckpoints() {
    }

    @Test
    public void loadExisting() {
    }

    @Test
    public void makeNew() {
    }
}