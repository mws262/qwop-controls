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

    rootNode
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
        setupTree();

        Node node1_1_1_3 = node1_1_1.addChild(a3);
        Assert.assertEquals(node1_1_1_3, node1_1_1.getChildByIndex(0));
        Assert.assertEquals(1, node1_1_1.getChildCount());
        Assert.assertEquals(node1_1_1_3.getAction(), a3);

        Node node2_2_4 = node2_2.addChild(a4);
        Assert.assertEquals(node2_2_4, node2_2.getChildByIndex(3));
        Assert.assertEquals(4, node2_2.getChildCount());
        Assert.assertEquals(node2_2_4.getAction(), a4);

        // Trying to add a duplicate action child.
        exception.expect(IllegalArgumentException.class);
        node1_1.addChild(a1);

    }

    @Test
    public void reserveExpansionRights() {// TODO
    }

    @Test
    public void releaseExpansionRights() {// TODO
    }

    @Test
    public void getLockStatus() {// TODO
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
                if (childNodesCheckOff.isEmpty()) {
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
        setupTree();

        // From root.
        List<Node> nlist = new ArrayList<>();
        rootNode.getNodesBelow(nlist, false);
        Assert.assertEquals(allNodes.size(), nlist.size());
        for (Node n : allNodes) {
            Assert.assertTrue(nlist.contains(n));
        }
        nlist.clear();
        rootNode.getNodesBelow(nlist, true);
        Assert.assertEquals(1, nlist.size()); // Root is the only one with state assigned.

        // From another node.
        nlist.clear();
        node2.getNodesBelow(nlist, false);
        Assert.assertEquals(6, nlist.size());
        Assert.assertTrue(nlist.contains(node2));
        Assert.assertTrue(nlist.contains(node2_1));
        Assert.assertTrue(nlist.contains(node2_2));
        Assert.assertTrue(nlist.contains(node2_2_1));
        Assert.assertTrue(nlist.contains(node2_2_2));
        Assert.assertTrue(nlist.contains(node2_2_3));
        nlist.clear();
        node2.getNodesBelow(nlist, true);
        Assert.assertEquals(0, nlist.size());

        // From an end node.
        nlist.clear();
        node3_3_3.getNodesBelow(nlist, false);
        Assert.assertEquals(1, nlist.size());
        Assert.assertTrue(nlist.contains(node3_3_3));
        nlist.clear();
        node3_3_3.getNodesBelow(nlist, true);
        Assert.assertEquals(0, nlist.size());

        // With some states assigned.
        nlist.clear();
        node1_2.setState(unfailedState);
        node3_3_2.setState(failedState);
        node2.setState(unfailedState);
        node1_4.setState(failedState);

        rootNode.getNodesBelow(nlist, true);
        Assert.assertEquals(5, nlist.size()); // 4 above plus root node.
        nlist.clear();
        node1.getNodesBelow(nlist, true);
        Assert.assertEquals(2, nlist.size());
        Assert.assertTrue(nlist.contains(node1_2));
        Assert.assertTrue(nlist.contains(node1_4));
        nlist.clear();
        node3_3_2.getNodesBelow(nlist, true);
        Assert.assertEquals(1, nlist.size());
        Assert.assertTrue(nlist.contains(node3_3_2));
    }

    @Test
    public void getLeaves() {
        setupTree();
        List<Node> nlist = new ArrayList<>();

        // From a leaf.
        node1_2_1_2_2_4.getLeaves(nlist);
        Assert.assertEquals(1, nlist.size());
        Assert.assertEquals(node1_2_1_2_2_4, nlist.get(0));

        // With a leaves right below.
        nlist.clear();
        node1_1.getLeaves(nlist);
        Assert.assertEquals(2, nlist.size());
        Assert.assertTrue(nlist.contains(node1_1_1));
        Assert.assertTrue(nlist.contains(node1_1_2));

        // Several layers of leaves below.
        nlist.clear();
        node1_2.getLeaves(nlist);
        Assert.assertEquals(2, nlist.size());
        Assert.assertTrue(nlist.contains(node1_2_1_2_1));
        Assert.assertTrue(nlist.contains(node1_2_1_2_2_4));

        // From root.
        nlist.clear();
        rootNode.getLeaves(nlist);
        Assert.assertEquals(16, nlist.size());
        Assert.assertTrue(nlist.contains(node1_1_1));
        Assert.assertTrue(nlist.contains(node1_1_2));
        Assert.assertTrue(nlist.contains(node1_2_1_2_1));
        Assert.assertTrue(nlist.contains(node1_2_1_2_2_4));
        Assert.assertTrue(nlist.contains(node1_3));
        Assert.assertTrue(nlist.contains(node1_4));
        Assert.assertTrue(nlist.contains(node2_1));
        Assert.assertTrue(nlist.contains(node2_2_1));
        Assert.assertTrue(nlist.contains(node2_2_2));
        Assert.assertTrue(nlist.contains(node2_2_3));
        Assert.assertTrue(nlist.contains(node3_1));
        Assert.assertTrue(nlist.contains(node3_2));
        Assert.assertTrue(nlist.contains(node3_3_1));
        Assert.assertTrue(nlist.contains(node3_3_2));
        Assert.assertTrue(nlist.contains(node3_3_3));
        Assert.assertTrue(nlist.contains(node3_3_4));
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
    public void destroyAllNodesBelowAndCheckExplored() {
    } // TODO

    @Test
    public void getTotalNodeCount() {
    } // TODO

    @Test
    public void getImportedNodeCount() {
    } // TODO

    @Test
    public void getCreatedNodeCount() {
        //TODO change from static counter. Too hard to test.
    }

    @Test
    public void getImportedGameCount() {
    } // TODO

    @Test
    public void isFailed() {
        setupTree();

        // Try some unfailed states.
        node1_1_2.setState(unfailedState);
        Assert.assertFalse(node1_1_2.isFailed());
        node3.setState(unfailedState);
        Assert.assertFalse(node3.isFailed());

        // Try some failed states.
        node2_2_3.setState(failedState);
        Assert.assertTrue(node2_2_3.isFailed());
        node2.setState(failedState);
        Assert.assertTrue(node2.isFailed());

        // Check root node.
        Assert.assertFalse(rootNode.isFailed());
    }

    @Test
    public void setFailed() {
        setupTree();

        // Can override any failure flag.
        // No state assigned.
        Assert.assertFalse(node2.isFailed());
        node2.setFailed(true);
        Assert.assertTrue(node2.isFailed());

        // Unfailed state.
        node1_2_1_2_1.setState(unfailedState);
        Assert.assertFalse(node1_2_1_2_1.isFailed());
        node1_2_1_2_1.setFailed(true);
        Assert.assertTrue(node1_2_1_2_1.isFailed());
        node1_2_1_2_1.setFailed(false);
        Assert.assertFalse(node1_2_1_2_1.isFailed());

        // Failed state.
        Assert.assertFalse(node1_1.isFailed());
        node1_1.setState(failedState);
        Assert.assertTrue(node1_1.isFailed());
        node1_1.setFailed(false);
        Assert.assertFalse(node1_1.isFailed());
        node1_1.setFailed(true);
        Assert.assertTrue(node1_1.isFailed());

        Assert.assertFalse(rootNode.isFailed());
        rootNode.setFailed(true);
        Assert.assertTrue(rootNode.isFailed());
        rootNode.setFailed(false);
        Assert.assertFalse(rootNode.isFailed());
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
        setupTree();

        Action[] alist = node1_2_1_2_2_4.getSequence();
        Assert.assertEquals(6, alist.length);
        Assert.assertEquals(a1, alist[0]);
        Assert.assertEquals(a2, alist[1]);
        Assert.assertEquals(a1, alist[2]);
        Assert.assertEquals(a2, alist[3]);
        Assert.assertEquals(a2, alist[4]);
        Assert.assertEquals(a4, alist[5]);

        alist = node3_3_2.getSequence();
        Assert.assertEquals(3, alist.length);
        Assert.assertEquals(a3, alist[0]);
        Assert.assertEquals(a3, alist[1]);
        Assert.assertEquals(a2, alist[2]);

        alist = node1_4.getSequence();
        Assert.assertEquals(2, alist.length);
        Assert.assertEquals(a1, alist[0]);
        Assert.assertEquals(a4, alist[1]);

        alist = node3.getSequence();
        Assert.assertEquals(1, alist.length);
        Assert.assertEquals(a3, alist[0]);

        alist = node1_2_1_2_1.getSequence();
        Assert.assertEquals(5, alist.length);
        Assert.assertEquals(a1, alist[0]);
        Assert.assertEquals(a2, alist[1]);
        Assert.assertEquals(a1, alist[2]);
        Assert.assertEquals(a2, alist[3]);
        Assert.assertEquals(a1, alist[4]);

        // Should throw when called on root.
        exception.expect(IndexOutOfBoundsException.class);
        rootNode.getSequence();
    }

    @Test
    public void makeNodesFromRunInfo() {// TODO
    }

    @Test
    public void makeNodesFromActionSequences() {// TODO
    }

    @Test
    public void calcNodePosBelow() {// TODO
    }

    @Test
    public void drawLines_below() {// TODO
    }

    @Test
    public void drawNodes_below() {// TODO
    }

    @Test
    public void turnOffBranchDisplay() {// TODO
    }

    @Test
    public void highlightSingleRunToThisNode() {// TODO
    }

    @Test
    public void resetLineBrightness_below() {// TODO
    }

    @Test
    public void getColorFromTreeDepth() {// TODO
    }

    @Test
    public void getColorFromScaledValue() {// TODO
    }

    @Test
    public void setBranchColor() {// TODO
    }

    @Test
    public void setBackwardsBranchColor() {// TODO
    }

    @Test
    public void clearBranchColor() {// TODO
    }

    @Test
    public void clearBackwardsBranchColor() {// TODO
    }

    @Test
    public void clearNodeOverrideColor() {// TODO
    }

    @Test
    public void setBranchZOffset() {
        setupTree();

        // Should be at 0 initially.
        for (Node n : allNodes) {
            Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
        }

        // Calling on leaf node should only change that one node.
        Node testNode = node2_2_3;
        testNode.setBranchZOffset(1);
        for (Node n : allNodes) {
            if (testNode == n) {
                Assert.assertEquals(1f, testNode.nodeLocationZOffset, 1e-10);
            } else {
                Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
            }
        }
        testNode.setBranchZOffset(0f); // Reset it.

        // Try some node in the middle.
        testNode = node2;
        testNode.setBranchZOffset(5);
        Assert.assertEquals(5, node2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node2_2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node2_2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node2_2_3.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node1_4.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, rootNode.nodeLocationZOffset, 1e-10);


        // Changing from root should change all nodes.
        rootNode.setBranchZOffset(-1f);
        for (Node n : allNodes) {
            Assert.assertEquals(-1, n.nodeLocationZOffset, 1e-10);
        }


    }

    @Test
    public void setBackwardsBranchZOffset() {
        setupTree();

        // Should be at 0 initially.
        for (Node n : allNodes) {
            Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
        }

        // Calling on root should only change root.
        rootNode.setBackwardsBranchZOffset(-4);
        for (Node n : allNodes) {
            if (n == rootNode) {
                Assert.assertEquals(-4f, n.nodeLocationZOffset, 1e-10);
            } else {
                Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
            }
        }

        // Somewhere in the middle.
        node1_2_1.setBackwardsBranchZOffset(3);
        Assert.assertEquals(0f, node1_2_1_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_2_1_2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_2_1_2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_2_1_2_2_4.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_2_1_2_2_4.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_3.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node2.nodeLocationZOffset, 1e-10);

        Assert.assertEquals(3f, node1_2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(3f, node1_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(3f, node1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(3f, rootNode.nodeLocationZOffset, 1e-10);

        // Reset all.
        for (Node n : allNodes) {
            n.nodeLocationZOffset = 0;
        }

        // Another middle spot.
        node3_3.setBackwardsBranchZOffset(7);
        Assert.assertEquals(0f, node3_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node3_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node2_2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node3_3_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node3_3_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node3_3_3.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node3_3_4.nodeLocationZOffset, 1e-10);

        Assert.assertEquals(7f, node3_3.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(7f, node3.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(7f, rootNode.nodeLocationZOffset, 1e-10);

        // Reset all.
        for (Node n : allNodes) {
            n.nodeLocationZOffset = 0;
        }

        // Some end node.
        node2_2_2.setBackwardsBranchZOffset(4f);
        Assert.assertEquals(0f, node2_2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node2_2_3.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0f, node1_1_2.nodeLocationZOffset, 1e-10);

        Assert.assertEquals(4f, node2_2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(4f, node2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(4f, node2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(4f, rootNode.nodeLocationZOffset, 1e-10);

    }

    @Test
    public void clearBackwardsBranchZOffset() {// TODO
    }

    @Test
    public void clearBranchZOffset() {
        setupTree();

        // From single node.
        node1_2_1.setBranchZOffset(5);
        node1_2_1.clearBranchZOffset();
        for (Node n : allNodes) {
            Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
        }

        // From root node.
        rootNode.setBranchZOffset(5);
        rootNode.clearBranchZOffset();
        for (Node n : allNodes) {
            Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
        }

        // Set from deeper node than reset -> No nodes offset after.
        node2.setBranchZOffset(5);
        rootNode.clearBranchZOffset();
        for (Node n : allNodes) {
            Assert.assertEquals(0f, n.nodeLocationZOffset, 1e-10);
        }

        // Set from shallower node than reset -> Some nodes remain offset.
        node1.setBranchZOffset(5);
        node1_2.clearBranchZOffset();

        Assert.assertEquals(5, node1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node1_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node1_1_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(5, node1_1_2.nodeLocationZOffset, 1e-10);

        Assert.assertEquals(0, node1_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node1_2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node1_2_1_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node1_2_1_2_1.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node1_2_1_2_2.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node1_2_1_2_2_4.nodeLocationZOffset, 1e-10);

        Assert.assertEquals(0, rootNode.nodeLocationZOffset, 1e-10);
        Assert.assertEquals(0, node2.nodeLocationZOffset, 1e-10);
    }

    @Test
    public void getChildren() {
        setupTree();

        List<Node> children = rootNode.getChildren();
        Assert.assertEquals(3, children.size());
        Assert.assertTrue(children.contains(node1));
        Assert.assertTrue(children.contains(node2));
        Assert.assertTrue(children.contains(node3));

        children = node1.getChildren();
        Assert.assertEquals(4, children.size());
        Assert.assertTrue(children.contains(node1_1));
        Assert.assertTrue(children.contains(node1_2));
        Assert.assertTrue(children.contains(node1_3));
        Assert.assertTrue(children.contains(node1_4));

        children = node2_2.getChildren();
        Assert.assertEquals(3, children.size());
        Assert.assertTrue(children.contains(node2_2_1));
        Assert.assertTrue(children.contains(node2_2_2));
        Assert.assertTrue(children.contains(node2_2_3));

        children = node1_2_1_2_2.getChildren();
        Assert.assertEquals(1, children.size());
        Assert.assertTrue(children.contains(node1_2_1_2_2_4));

        children = node2_2_3.getChildren();
        Assert.assertTrue(children.isEmpty());

        children = node3_3_3.getChildren();
        Assert.assertTrue(children.isEmpty());
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
    public void propagateFullyExploredStatus_lite() { //TODO
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
            } else { // Root already gets assigned initial state by default.
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