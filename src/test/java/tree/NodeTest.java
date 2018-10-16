package tree;

import game.GameLoader;
import game.State;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import actions.Action;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeTest {
    /* Demo tree.
    Tree structure: 27 nodes. Max depth 6 (7 layers, including 0th).

    Root Node
    ├── node1
    │   ├── node1_1
    │   │    ├── node1_1_1
    │   │    └── node1_1_2
    │   ├── node1_2
    │   │    └── node1_2_1
    │   │        └── node1_2_1_2
    │   │            │── node1_2_1_2_1
    │   │            └── node1_2_1_2_2
    │   │                └── node1_2_1_2_2_4
    │   ├── node1_3
    │   └── node1_4
    ├── node2
    │   ├── node2_1
    │   └── node2_2
    │       ├── node2_2_1
    │       ├── node2_2_2
    │       └── node2_2_3
    └── node3
        ├── node3_1
        ├── node3_2
        └── node3_3
            ├── node3_3_1
            ├── node3_3_2
            ├── node3_3_3
            └── node3_3_4
     */

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    // Root node for our test tree.
    private Node rootNode;

    private Node node1, node2, node3, node1_1, node1_2, node1_3, node1_4, node2_1, node2_2, node3_1, node3_2, node3_3,
            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
            node3_3_4, node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_4;


    private List<Node> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;

    // Some sample actions (mocked).
    private Action a1 = mock(Action.class);
    private Action a2 = mock(Action.class);
    private Action a3 = mock(Action.class);
    private Action a4 = mock(Action.class);

    // Some states (mocked).
    private State unfailedState = mock(State.class);
    private State failedState = mock(State.class);


    private void setupTree() {
        // Canned mock Action return values.
        when(a1.getTimestepsTotal()).thenReturn(10);
        when(a2.getTimestepsTotal()).thenReturn(15);
        when(a3.getTimestepsTotal()).thenReturn(12);
        when(a4.getTimestepsTotal()).thenReturn(20);

        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);

        // Depth 0.
        rootNode = new Node();
        nodesLvl0 = new ArrayList<>();
        nodesLvl0.add(rootNode);

        // Depth 1.
        node1 = rootNode.addChild(a1); // Depth 1, node 1.
        node2 = rootNode.addChild(a2); // Depth 1, node 2.
        node3 = rootNode.addChild(a3); // Depth 1, node 3.
        nodesLvl1 = new ArrayList<>();
        nodesLvl1.add(node1);
        nodesLvl1.add(node2);
        nodesLvl1.add(node3);

        // Depth 2.
        node1_1 = node1.addChild(a1);
        node1_2 = node1.addChild(a2);
        node1_3 = node1.addChild(a3);
        node1_4 = node1.addChild(a4);

        node2_1 = node2.addChild(a1);
        node2_2 = node2.addChild(a2);

        node3_1 = node3.addChild(a1);
        node3_2 = node3.addChild(a2);
        node3_3 = node3.addChild(a3);

        nodesLvl2 = new ArrayList<>();
        nodesLvl2.add(node1_1);
        nodesLvl2.add(node1_2);
        nodesLvl2.add(node1_3);
        nodesLvl2.add(node1_4);
        nodesLvl2.add(node2_1);
        nodesLvl2.add(node2_2);
        nodesLvl2.add(node3_1);
        nodesLvl2.add(node3_2);
        nodesLvl2.add(node3_3);

        // Depth 3.
        node1_1_1 = node1_1.addChild(a1);
        node1_1_2 = node1_1.addChild(a2);

        node1_2_1 = node1_2.addChild(a1);

        node2_2_1 = node2_2.addChild(a1);
        node2_2_2 = node2_2.addChild(a2);
        node2_2_3 = node2_2.addChild(a3);

        node3_3_1 = node3_3.addChild(a1);
        node3_3_2 = node3_3.addChild(a2);
        node3_3_3 = node3_3.addChild(a3);
        node3_3_4 = node3_3.addChild(a4);

        nodesLvl3 = new ArrayList<>();
        nodesLvl3.add(node1_1_1);
        nodesLvl3.add(node1_1_2);
        nodesLvl3.add(node1_2_1);
        nodesLvl3.add(node2_2_1);
        nodesLvl3.add(node2_2_2);
        nodesLvl3.add(node2_2_3);
        nodesLvl3.add(node3_3_1);
        nodesLvl3.add(node3_3_2);
        nodesLvl3.add(node3_3_3);
        nodesLvl3.add(node3_3_4);

        // Depth 4.
        node1_2_1_2 = node1_2_1.addChild(a2);
        nodesLvl4 = new ArrayList<>();
        nodesLvl4.add(node1_2_1_2);

        // Depth 5.
        node1_2_1_2_1 = node1_2_1_2.addChild(a1);
        node1_2_1_2_2 = node1_2_1_2.addChild(a2);
        nodesLvl5 = new ArrayList<>();
        nodesLvl5.add(node1_2_1_2_1);
        nodesLvl5.add(node1_2_1_2_2);

        // Depth 6.
        node1_2_1_2_2_4 = node1_2_1_2_2.addChild(a4);
        nodesLvl6 = new ArrayList<>();
        nodesLvl6.add(node1_2_1_2_2_4);

        allNodes = new ArrayList<>();
        allNodes.addAll(nodesLvl0);
        allNodes.addAll(nodesLvl1);
        allNodes.addAll(nodesLvl2);
        allNodes.addAll(nodesLvl3);
        allNodes.addAll(nodesLvl4);
        allNodes.addAll(nodesLvl5);
        allNodes.addAll(nodesLvl6);
    }

    @Test
    public void addChild() {

    }

    @Test
    public void reserveExpansionRights() {
    }

    @Test
    public void releaseExpansionRights() {
    }

    @Test
    public void getLockStatus() {
    }

    @Test
    public void getValue() {
        setupTree();

        for (Node node : allNodes) { // Should all be zero initially. Will test other functionality in the setValue
            // and addToValue unit tests.
            Assert.assertEquals(0., node.getValue(), 1e-12);
        }
    }

    @Test
    public void setValue() {
        setupTree();

        for (Node node : allNodes) { // Should all be zero initially. Will test other functionality in the setValue
            // and addToValue unit tests.
            node.setValue(1.87f);
            Assert.assertEquals(1.87f, node.getValue(), 1e-12);
        }
    }

    @Test
    public void addToValue() {
        setupTree();

        for (Node node : allNodes) { // Should all be zero initially. Will test other functionality in the setValue
            // and addToValue unit tests.
            node.setValue(1.87f);
            node.addToValue(1.f);
            Assert.assertEquals(2.87f, node.getValue(), 1e-12);
        }
    }

    @Test
    public void getParent() {
        setupTree();

        Assert.assertNull(rootNode.getParent()); // Root has no parent.

        Assert.assertEquals(node1.getParent(), rootNode);
        Assert.assertEquals(node2.getParent(), rootNode);
        Assert.assertEquals(node3.getParent(), rootNode);
        Assert.assertEquals(node1_3.getParent(), node1);
        Assert.assertEquals(node1_2_1.getParent(), node1_2);
        Assert.assertEquals(node1_2_1_2_2_4.getParent(), node1_2_1_2_2);
        Assert.assertEquals(node1_2_1_2_1.getParent(), node1_2_1_2);
        Assert.assertEquals(node1_2_1_2.getParent(), node1_2_1);
        Assert.assertEquals(node3_3_2.getParent(), node3_3);
        Assert.assertEquals(node3_1.getParent(), node3);
    }

    @Test
    public void getRandomChild() {
        setupTree();

        List<Node> childNodes = new ArrayList<>();
        childNodes.add(node3_3_1);
        childNodes.add(node3_3_2);
        childNodes.add(node3_3_3);
        childNodes.add(node3_3_4);

        List<Node> childNodesCheckOff = new ArrayList<>(childNodes);
        boolean success = false;
        for (int i = 0; i < 10000; i++) {
            Node sampledNode = node3_3.getRandomChild();

            Assert.assertTrue(childNodes.contains(sampledNode));

            if (childNodesCheckOff.contains(sampledNode)) {
                childNodesCheckOff.remove(sampledNode);
                if (childNodesCheckOff.isEmpty()){
                    success = true;
                    break;
                }
            }
        }

        Assert.assertTrue("Were not able to randomly sample all children.", success);

        // Try a node which has no children.
        exception.expect(IndexOutOfBoundsException.class);
        node3_3_4.getRandomChild();
    }

    @Test
    public void getNodesBelow() {
    }

    @Test
    public void getLeaves() {
    }

    @Test
    public void getRoot() {
        setupTree();
        for (Node node : allNodes) { // Should return root regardless of the node getRoot() is called from.
            Assert.assertEquals(node.getRoot(), rootNode);
        }
    }

    @Test
    public void countDescendants() {
        setupTree();

        Assert.assertEquals(26, rootNode.countDescendants());
        Assert.assertEquals(11, node1.countDescendants());
        Assert.assertEquals(5, node2.countDescendants());
        Assert.assertEquals(7, node3.countDescendants());

        Assert.assertEquals(2, node1_1.countDescendants());
        Assert.assertEquals(5, node1_2.countDescendants());
        Assert.assertEquals(0, node1_3.countDescendants());
        Assert.assertEquals(0, node1_4.countDescendants());
        Assert.assertEquals(0, node2_1.countDescendants());
        Assert.assertEquals(3, node2_2.countDescendants());
        Assert.assertEquals(0, node3_1.countDescendants());
        Assert.assertEquals(0, node3_2.countDescendants());
        Assert.assertEquals(4, node3_3.countDescendants());

        Assert.assertEquals(0, node1_1_1.countDescendants());
        Assert.assertEquals(0, node1_1_2.countDescendants());
        Assert.assertEquals(4, node1_2_1.countDescendants());
        Assert.assertEquals(0, node2_2_1.countDescendants());
        Assert.assertEquals(0, node2_2_2.countDescendants());
        Assert.assertEquals(0, node2_2_3.countDescendants());
        Assert.assertEquals(0, node3_3_1.countDescendants());
        Assert.assertEquals(0, node3_3_2.countDescendants());
        Assert.assertEquals(0, node3_3_3.countDescendants());
        Assert.assertEquals(0, node3_3_4.countDescendants());

        Assert.assertEquals(3, node1_2_1_2.countDescendants());

        Assert.assertEquals(0, node1_2_1_2_1.countDescendants());
        Assert.assertEquals(1, node1_2_1_2_2.countDescendants());

        Assert.assertEquals(0, node1_2_1_2_2_4.countDescendants());
    }

    @Test
    public void isOtherNodeAncestor() {
        setupTree();

        Assert.assertTrue(node1_2_1_2_2_4.isOtherNodeAncestor(node1_2_1_2_2));
        Assert.assertTrue(node1_2_1_2_2_4.isOtherNodeAncestor(node1_2_1));
        Assert.assertTrue(node1_2_1_2_2_4.isOtherNodeAncestor(node1_2));
        Assert.assertTrue(node1_2_1_2_2_4.isOtherNodeAncestor(node1));
        Assert.assertTrue(node1_2_1_2_2_4.isOtherNodeAncestor(rootNode));
        Assert.assertFalse(node1_2_1_2_2_4.isOtherNodeAncestor(node1_2_1_2_1));
        Assert.assertFalse(node1_2_1_2_2_4.isOtherNodeAncestor(node2));

        Assert.assertTrue(node2.isOtherNodeAncestor(rootNode));
        Assert.assertFalse(node2.isOtherNodeAncestor(node1)); // Not a sibling.
        Assert.assertFalse(node2.isOtherNodeAncestor(node2_1)); // Not a child.

        Assert.assertTrue(node3_3_4.isOtherNodeAncestor(node3_3));
        Assert.assertTrue(node3_3_4.isOtherNodeAncestor(node3));
        Assert.assertTrue(node3_3_4.isOtherNodeAncestor(rootNode));
        Assert.assertFalse(node3_3_4.isOtherNodeAncestor(node3_3_3));
        Assert.assertFalse(node3_3_4.isOtherNodeAncestor(node2_2_3));
    }

    @Test
    public void destroyAllNodesBelowAndCheckExplored() {}

    @Test
    public void getTotalNodeCount() {} // TODO

    @Test
    public void getImportedNodeCount() {} // TODO

    @Test
    public void getCreatedNodeCount() {
//        // Reset the static field containing the node creation count. Hacky, but hey, it's a unit test.
//        try {
//            Field nodeCounter = Node.class.getDeclaredField("nodesCreated");
//            nodeCounter.setAccessible(true);
//            LongAdder.class.getMethod("reset").invoke(nodeCounter.get(null));
//        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
//            e.printStackTrace();
//        }
//        setupTree();
//
//        Assert.assertEquals(27, Node.getTotalNodeCount());
//        rootNode.addChild(a4);
//
//        Assert.assertEquals(28, Node.getTotalNodeCount());
    }

    @Test
    public void getImportedGameCount() {
    }

    @Test
    public void isFailed() {
    }

    @Test
    public void setFailed() {
    }

    @Test
    public void setState() {
        setupTree();

        Assert.assertTrue(node1_1_1.isStateUnassigned());
        node1_1_1.setState(unfailedState);
        Assert.assertEquals(unfailedState, node1_1_1.getState());

        Assert.assertTrue(node3_3_4.isStateUnassigned());
        node3_3_4.setState(unfailedState);
        Assert.assertEquals(unfailedState, node3_3_4.getState());

        exception.expect(IllegalStateException.class);
        rootNode.setState(unfailedState);
    }

    @Test
    public void getAction() {
        setupTree();

        Assert.assertEquals(a1, node1.getAction());
        Assert.assertEquals(a2, node2.getAction());
        Assert.assertEquals(a3, node3.getAction());

        Assert.assertEquals(a1, node1_1.getAction());
        Assert.assertEquals(a2, node1_2.getAction());
        Assert.assertEquals(a3, node1_3.getAction());
        Assert.assertEquals(a4, node1_4.getAction());

        Assert.assertEquals(a1, node1_1_1.getAction());
        Assert.assertEquals(a2, node1_1_2.getAction());

        Assert.assertEquals(a1, node1_2_1.getAction());
        Assert.assertEquals(a2, node1_2_1_2.getAction());

        Assert.assertEquals(a1, node1_2_1_2_1.getAction());
        Assert.assertEquals(a2, node1_2_1_2_2.getAction());
        Assert.assertEquals(a4, node1_2_1_2_2_4.getAction());

        Assert.assertEquals(a1, node2_1.getAction());
        Assert.assertEquals(a2, node2_2.getAction());

        Assert.assertEquals(a1, node2_2_1.getAction());
        Assert.assertEquals(a2, node2_2_2.getAction());
        Assert.assertEquals(a3, node2_2_3.getAction());

        Assert.assertEquals(a1, node3_1.getAction());
        Assert.assertEquals(a2, node3_2.getAction());
        Assert.assertEquals(a3, node3_3.getAction());

        Assert.assertEquals(a1, node3_3_1.getAction());
        Assert.assertEquals(a2, node3_3_2.getAction());
        Assert.assertEquals(a3, node3_3_3.getAction());
        Assert.assertEquals(a4, node3_3_4.getAction());

        exception.expect(NullPointerException.class);
        rootNode.getAction();
    }

    @Test
    public void getSequence() {
    }

    @Test
    public void makeNodesFromRunInfo() {
    }

    @Test
    public void makeNodesFromActionSequences() {
    }

    @Test
    public void calcNodePosBelow() {
    }

    @Test
    public void drawLines_below() {
    }

    @Test
    public void drawNodes_below() {
    }

    @Test
    public void turnOffBranchDisplay() {
    }

    @Test
    public void highlightSingleRunToThisNode() {
    }

    @Test
    public void resetLineBrightness_below() {
    }

    @Test
    public void getColorFromTreeDepth() {
    }

    @Test
    public void getColorFromScaledValue() {
    }

    @Test
    public void setBranchColor() {
    }

    @Test
    public void setBackwardsBranchColor() {
    }

    @Test
    public void clearBranchColor() {
    }

    @Test
    public void clearBackwardsBranchColor() {
    }

    @Test
    public void clearNodeOverrideColor() {
    }

    @Test
    public void setBranchZOffset() {
    }

    @Test
    public void setBackwardsBranchZOffset() {
    }

    @Test
    public void clearBackwardsBranchZOffset() {
    }

    @Test
    public void clearBranchZOffset() {
    }

    @Test
    public void getChildren() {
    }

    @Test
    public void getIndexAccordingToParent() {
        setupTree();

        Assert.assertEquals(0, node1.getIndexAccordingToParent());
        Assert.assertEquals(1, node2.getIndexAccordingToParent());
        Assert.assertEquals(2, node3.getIndexAccordingToParent());

        Assert.assertEquals(0, node1_1.getIndexAccordingToParent());
        Assert.assertEquals(1, node1_2.getIndexAccordingToParent());
        Assert.assertEquals(2, node1_3.getIndexAccordingToParent());
        Assert.assertEquals(3, node1_4.getIndexAccordingToParent());

        Assert.assertEquals(0, node1_2_1_2_2_4.getIndexAccordingToParent());

        Assert.assertEquals(0, node2_1.getIndexAccordingToParent());
        Assert.assertEquals(1, node2_2.getIndexAccordingToParent());

        exception.expect(IndexOutOfBoundsException.class);
        rootNode.getIndexAccordingToParent(); // Root should throw an exception.
    }

    @Test
    public void getSiblingCount() {
        setupTree();

        Assert.assertEquals(0, rootNode.getSiblingCount());

        Assert.assertEquals(2, node1.getSiblingCount());
        Assert.assertEquals(2, node2.getSiblingCount());
        Assert.assertEquals(2, node3.getSiblingCount());

        Assert.assertEquals(3, node3_3_1.getSiblingCount());
        Assert.assertEquals(3, node3_3_2.getSiblingCount());
        Assert.assertEquals(3, node3_3_3.getSiblingCount());
        Assert.assertEquals(3, node3_3_4.getSiblingCount());

        Assert.assertEquals(0, node1_2_1_2_2_4.getSiblingCount());

        Assert.assertEquals(1, node2_1.getSiblingCount());
        Assert.assertEquals(1, node2_2.getSiblingCount());
    }

    @Test
    public void getChildCount() {
        setupTree();

        Assert.assertEquals(3, rootNode.getChildCount());
        Assert.assertEquals(4, node1.getChildCount());
        Assert.assertEquals(2, node2.getChildCount());
        Assert.assertEquals(3, node3.getChildCount());
        Assert.assertEquals(2, node1_1.getChildCount());
        Assert.assertEquals(1, node1_2.getChildCount());
        Assert.assertEquals(0, node1_3.getChildCount());
        Assert.assertEquals(0, node1_4.getChildCount());
        Assert.assertEquals(0, node2_1.getChildCount());
        Assert.assertEquals(3, node2_2.getChildCount());
        Assert.assertEquals(0, node3_1.getChildCount());
        Assert.assertEquals(0, node3_2.getChildCount());
        Assert.assertEquals(4, node3_3.getChildCount());
        Assert.assertEquals(0, node1_1_1.getChildCount());
        Assert.assertEquals(0, node1_1_2.getChildCount());
        Assert.assertEquals(1, node1_2_1.getChildCount());
        Assert.assertEquals(0, node2_2_1.getChildCount());
        Assert.assertEquals(0, node2_2_2.getChildCount());
        Assert.assertEquals(0, node2_2_3.getChildCount());
        Assert.assertEquals(0, node3_3_1.getChildCount());
        Assert.assertEquals(0, node3_3_2.getChildCount());
        Assert.assertEquals(0, node3_3_3.getChildCount());
        Assert.assertEquals(0, node3_3_4.getChildCount());
        Assert.assertEquals(2, node1_2_1_2.getChildCount());
        Assert.assertEquals(0, node1_2_1_2_1.getChildCount());
        Assert.assertEquals(1, node1_2_1_2_2.getChildCount());
        Assert.assertEquals(0, node1_2_1_2_2_4.getChildCount());

        // Add one and make sure the count goes up.
        rootNode.addChild(a4);
        Assert.assertEquals(4, rootNode.getChildCount());
    }

    @Test
    public void getChildByIndex() {
        setupTree();

        Assert.assertEquals(rootNode.getChildByIndex(0), node1);
        Assert.assertEquals(rootNode.getChildByIndex(1), node2);
        Assert.assertEquals(rootNode.getChildByIndex(2), node3);

        Assert.assertEquals(node1_1.getChildByIndex(0), node1_1_1);
        Assert.assertEquals(node1_1.getChildByIndex(1), node1_1_2);

        Assert.assertEquals(node1_2.getChildByIndex(0), node1_2_1);
        Assert.assertEquals(node1_2_1.getChildByIndex(0), node1_2_1_2);
        Assert.assertEquals(node1_2_1_2.getChildByIndex(0), node1_2_1_2_1);
        Assert.assertEquals(node1_2_1_2.getChildByIndex(1), node1_2_1_2_2);
        Assert.assertEquals(node1_2_1_2_2.getChildByIndex(0), node1_2_1_2_2_4);

        Assert.assertEquals(node2.getChildByIndex(0), node2_1);
        Assert.assertEquals(node2.getChildByIndex(1), node2_2);

        Assert.assertEquals(node2_2.getChildByIndex(0), node2_2_1);
        Assert.assertEquals(node2_2.getChildByIndex(1), node2_2_2);
        Assert.assertEquals(node2_2.getChildByIndex(2), node2_2_3);

        Assert.assertEquals(node3.getChildByIndex(0), node3_1);
        Assert.assertEquals(node3.getChildByIndex(1), node3_2);
        Assert.assertEquals(node3.getChildByIndex(2), node3_3);

        Assert.assertEquals(node3_3.getChildByIndex(0), node3_3_1);
        Assert.assertEquals(node3_3.getChildByIndex(1), node3_3_2);
        Assert.assertEquals(node3_3.getChildByIndex(2), node3_3_3);
        Assert.assertEquals(node3_3.getChildByIndex(3), node3_3_4);
    }

    @Test
    public void propagateFullyExploredStatus_lite() {
    }

    @Test
    public void getState() {
        setupTree();

        Assert.assertEquals(rootNode.getState(), GameLoader.getInitialState());

        exception.expect(NullPointerException.class);
        for (Node n : allNodes) {
            if (n.getTreeDepth() == 0) continue; // Root already has state assigned.
            n.getState();
            n.setState(unfailedState);
            Assert.assertEquals(n.getState(), unfailedState);
        }
    }

    @Test
    public void isStateUnassigned() {
        setupTree();
        for (Node n : allNodes) {
            if (n.getTreeDepth() != 0) {
                Assert.assertTrue(n.isStateUnassigned());
                n.setState(unfailedState);
                Assert.assertFalse(n.isStateUnassigned());
            }else{ // Root already gets assigned initial state by default.
                Assert.assertFalse(n.isStateUnassigned());
            }
        }
    }

    @Test
    public void getTreeDepth() {
        setupTree();

        for (Node n : nodesLvl0) {
            Assert.assertEquals(0, n.getTreeDepth());
        }
        for (Node n : nodesLvl1) {
            Assert.assertEquals(1, n.getTreeDepth());
        }
        for (Node n : nodesLvl2) {
            Assert.assertEquals(2, n.getTreeDepth());
        }
        for (Node n : nodesLvl3) {
            Assert.assertEquals(3, n.getTreeDepth());
        }
        for (Node n : nodesLvl4) {
            Assert.assertEquals(4, n.getTreeDepth());
        }
        for (Node n : nodesLvl5) {
            Assert.assertEquals(5, n.getTreeDepth());
        }
        for (Node n : nodesLvl6) {
            Assert.assertEquals(6, n.getTreeDepth());
        }
    }
}