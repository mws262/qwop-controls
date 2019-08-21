package tree.node;

import game.action.*;
import distributions.Distribution_Equal;
import game.IGameInternal;
import game.state.State;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeQWOPExplorableTest {
 /* Demo tree.
    Tree structure: 25 nodes. Max depth 6 (7 layers, including 0th).

        * means failed

    rootNode
    ├── node1
    │   ├── node1_1
    │   │    ├── node1_1_1
    │   │    └── node1_1_2 *
    │   ├── node1_2
    │   │    └── node1_2_1
    │   │        └── node1_2_1_2
    │   │            │── node1_2_1_2_1 *
    │   │            └── node1_2_1_2_2
    │   │                └── node1_2_1_2_2_3
    │   ├── node1_3
    │
    ├── node2
    │   ├── node2_1
    │   └── node2_2
    │       ├── node2_2_1
    │       ├── node2_2_2 *
    │       └── node2_2_3 *
    └── node3
        ├── node3_1
        ├── node3_2 *
        └── node3_3
            ├── node3_3_1 *
            ├── node3_3_2 *
            ├── node3_3_3 *
 */

    // Root node for our test tree.
    private NodeQWOPExplorable rootNode;

    private NodeQWOPExplorable node1, node2, node3, node1_1, node1_2, node1_3, node2_1, node2_2, node3_1, node3_2, node3_3,
            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
            node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_3;


    private List<NodeQWOPExplorable> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;

    // Some sample game.action (mocked).
    private Action a1;
    private Action a2;
    private Action a3;
    private Action a4;
    private Action a5;
    private Action a6;

    // Some states (mocked).
    private State initialState = mock(State.class);
    private State unfailedState = mock(State.class);
    private State failedState = mock(State.class);

    private IGameInternal game = mock(IGameInternal.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    private void setupTree() {

        when(initialState.isFailed()).thenReturn(false);
        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);
        when(game.getCurrentState()).thenReturn(unfailedState);

        // Set up action generator.
        ActionList list1 = ActionList.makeActionList(new int[]{1,2,3}, CommandQWOP.Q, new Distribution_Equal());
        ActionList list2 = ActionList.makeActionList(new int[]{4,5,6}, CommandQWOP.W, new Distribution_Equal());

        IActionGenerator generator = new ActionGenerator_FixedSequence(new ActionList[]{list1, list2});
        a1 = list1.get(0);
        a2 = list1.get(1);
        a3 = list1.get(2);

        a4 = list2.get(0);
        a5 = list2.get(1);
        a6 = list2.get(2);

        // Depth 0.
        rootNode = new NodeQWOPExplorable(initialState, generator);
        nodesLvl0 = new ArrayList<>();
        nodesLvl0.add(rootNode);

        // Depth 1.
        node1 = rootNode.addDoublyLinkedChild(rootNode.getUntriedActionByIndex(0), unfailedState); // Depth 1, node 1.
        node2 = rootNode.addDoublyLinkedChild(rootNode.getUntriedActionByIndex(0), unfailedState); // Depth 1, node 2.
        node3 = rootNode.addDoublyLinkedChild(rootNode.getUntriedActionByIndex(0), unfailedState); // Depth 1, node 3.
        Assert.assertEquals(0, rootNode.getUntriedActionCount());
        Assert.assertEquals(a1, node1.getAction());

        nodesLvl1 = new ArrayList<>();
        nodesLvl1.add(node1);
        nodesLvl1.add(node2);
        nodesLvl1.add(node3);

        // Depth 2.
        Assert.assertEquals(a4, node1.getUntriedActionByIndex(0));
        node1_1 = node1.addDoublyLinkedChild(a4, unfailedState);
        node1_2 = node1.addDoublyLinkedChild(a5, unfailedState);
        node1_3 = node1.addDoublyLinkedChild(a6, unfailedState);

        node2_1 = node2.addDoublyLinkedChild(a4, unfailedState);
        node2_2 = node2.addDoublyLinkedChild(a5, unfailedState);

        node3_1 = node3.addDoublyLinkedChild(a4, unfailedState);
        node3_2 = node3.addDoublyLinkedChild(a5, failedState);
        node3_3 = node3.addDoublyLinkedChild(a6, unfailedState);

        nodesLvl2 = new ArrayList<>();
        nodesLvl2.add(node1_1);
        nodesLvl2.add(node1_2);
        nodesLvl2.add(node1_3);
        nodesLvl2.add(node2_1);
        nodesLvl2.add(node2_2);
        nodesLvl2.add(node3_1);
        nodesLvl2.add(node3_2);
        nodesLvl2.add(node3_3);

        // Depth 3.
        Assert.assertEquals(a1, node1_1.getUntriedActionByIndex(0));
        node1_1_1 = node1_1.addDoublyLinkedChild(a1, unfailedState);
        node1_1_2 = node1_1.addDoublyLinkedChild(a2, failedState);

        node1_2_1 = node1_2.addDoublyLinkedChild(a1, unfailedState);

        node2_2_1 = node2_2.addDoublyLinkedChild(a1, unfailedState);
        node2_2_2 = node2_2.addDoublyLinkedChild(a2, failedState);
        node2_2_3 = node2_2.addDoublyLinkedChild(a3, failedState);

        node3_3_1 = node3_3.addDoublyLinkedChild(a1, failedState);
        node3_3_2 = node3_3.addDoublyLinkedChild(a2, failedState);
        node3_3_3 = node3_3.addDoublyLinkedChild(a3, failedState);

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

        // Depth 4.
        Assert.assertEquals(a4, node1_2_1.getUntriedActionByIndex(0));
        node1_2_1_2 = node1_2_1.addDoublyLinkedChild(a5, unfailedState);
        nodesLvl4 = new ArrayList<>();
        nodesLvl4.add(node1_2_1_2);

        // Depth 5.
        Assert.assertEquals(a1, node1_2_1_2.getUntriedActionByIndex(0));
        node1_2_1_2_1 = node1_2_1_2.addDoublyLinkedChild(a1, failedState);
        node1_2_1_2_2 = node1_2_1_2.addDoublyLinkedChild(a2, unfailedState);
        nodesLvl5 = new ArrayList<>();
        nodesLvl5.add(node1_2_1_2_1);
        nodesLvl5.add(node1_2_1_2_2);

        // Depth 6.
        Assert.assertEquals(a4, node1_2_1_2_2.getUntriedActionByIndex(0));
        node1_2_1_2_2_3 = node1_2_1_2_2.addDoublyLinkedChild(a6, unfailedState);
        nodesLvl6 = new ArrayList<>();
        nodesLvl6.add(node1_2_1_2_2_3);

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
    public void isFullyExplored() {
        setupTree();

        // Failed nodes should be fully explored.
        Assert.assertTrue(node1_1_2.isFullyExplored());
        Assert.assertTrue(node1_2_1_2_1.isFullyExplored());
        Assert.assertTrue(node2_2_2.isFullyExplored());
        Assert.assertTrue(node2_2_3.isFullyExplored());
        Assert.assertTrue(node3_2.isFullyExplored());
        Assert.assertTrue(node3_3_1.isFullyExplored());
        Assert.assertTrue(node3_3_2.isFullyExplored());
        Assert.assertTrue(node3_3_3.isFullyExplored());

        // Node which has all failed children and no untried game.action should be fully-explored.
        Assert.assertEquals(3, node3_3.getChildCount());
        Assert.assertEquals(0, node3_3.getUntriedActionCount());
        Assert.assertTrue(node3_3.isFullyExplored());

        // Other various nodes which still have potential should not be marked fully-explored.
        Assert.assertFalse(rootNode.isFullyExplored());
        Assert.assertFalse(node1_2_1_2.isFullyExplored());
        Assert.assertFalse(node3.isFullyExplored());

        // Add all failed child game.action to a node should make it fully explored and cause correct propagation up the
        // tree.
        Assert.assertFalse(node2_2.isFullyExplored());
        Assert.assertFalse(node2_2_1.isFullyExplored());
        node2_2_1.addDoublyLinkedChild(node2_2_1.getUntriedActionRandom(), failedState);
        node2_2_1.addDoublyLinkedChild(node2_2_1.getUntriedActionRandom(), failedState);
        node2_2_1.addDoublyLinkedChild(node2_2_1.getUntriedActionRandom(), failedState);
        Assert.assertEquals(0, node2_2_1.getUntriedActionCount());
        Assert.assertTrue(node2_2_1.isFullyExplored());
        Assert.assertTrue(node2_2.isFullyExplored());

        Assert.assertFalse(node2.isFullyExplored());
        node2.addDoublyLinkedChild(a6, failedState);
        Assert.assertEquals(0, node2.getUntriedActionCount());
        node2_1.addDoublyLinkedChild(node2_1.getUntriedActionRandom(), failedState);
        node2_1.addDoublyLinkedChild(node2_1.getUntriedActionRandom(), failedState);
        node2_1.addDoublyLinkedChild(node2_1.getUntriedActionRandom(), failedState);

        Assert.assertTrue(node2.isFullyExplored());
    }

    @Test
    public void setFullyExploredStatus() {


    }

    @Test
    public void getUntriedActionCount() {
        setupTree();

        Assert.assertEquals(0, rootNode.getUntriedActionCount());
        Assert.assertEquals(0, node1.getUntriedActionCount());
        Assert.assertEquals(1, node2.getUntriedActionCount());
        Assert.assertEquals(0, node3.getUntriedActionCount());
        Assert.assertEquals(1, node1_1.getUntriedActionCount());
        Assert.assertEquals(2, node1_2.getUntriedActionCount());
        Assert.assertEquals(3, node1_3.getUntriedActionCount());
        Assert.assertEquals(3, node2_1.getUntriedActionCount());
        Assert.assertEquals(0, node2_2.getUntriedActionCount());
        Assert.assertEquals(3, node3_1.getUntriedActionCount());
        Assert.assertEquals(0, node3_2.getUntriedActionCount());
        Assert.assertEquals(0, node3_3.getUntriedActionCount());
        Assert.assertEquals(3, node1_1_1.getUntriedActionCount());
        Assert.assertEquals(0, node1_1_2.getUntriedActionCount());
        Assert.assertEquals(2, node1_2_1.getUntriedActionCount());
        Assert.assertEquals(3, node2_2_1.getUntriedActionCount());
        Assert.assertEquals(0, node2_2_2.getUntriedActionCount());
        Assert.assertEquals(0, node2_2_3.getUntriedActionCount());
        Assert.assertEquals(0, node3_3_1.getUntriedActionCount());
        Assert.assertEquals(0, node3_3_2.getUntriedActionCount());
        Assert.assertEquals(0, node3_3_3.getUntriedActionCount());
        Assert.assertEquals(1, node1_2_1_2.getUntriedActionCount());
        Assert.assertEquals(0, node1_2_1_2_1.getUntriedActionCount());
        Assert.assertEquals(2, node1_2_1_2_2.getUntriedActionCount());
        Assert.assertEquals(3, node1_2_1_2_2_3.getUntriedActionCount());
    }

    @Test
    public void getUntriedActionByIndex() {
        setupTree();

        Assert.assertEquals(a6, node2.getUntriedActionByIndex(0));
        Assert.assertEquals(a3, node1_1.getUntriedActionByIndex(0));
        Assert.assertEquals(a4, node1_2_1_2_2.getUntriedActionByIndex(0));
        Assert.assertEquals(a5, node1_2_1_2_2.getUntriedActionByIndex(1));

        exception.expect(IndexOutOfBoundsException.class);
        node1.getUntriedActionByIndex(0);
    }

    @Test
    public void getUntriedActionRandom() {
        setupTree();
        boolean a4found = false;
        boolean a6found = false;
        final int timeout = 10000;
        int counter = 0;
        while (counter++ < timeout) {
            Action a = node1_2_1.getUntriedActionRandom();
            if (a == a4) {
                a4found = true;
            } else if (a == a6) {
                a6found = true;
            }

            if (a4found && a6found) {
                break;
            }
        }

        Assert.assertTrue("Random action selection failed to come up with all possible game.action within " + timeout +
                "tries. This is incredibly unlikely to be random chance.", counter < 9999);
    }

    @Test
    public void getUntriedActionOnDistribution() {
    }

    @Test
    public void clearUntriedActions() {
        // This really shouldn't be used outside the class anyway.

        setupTree();

        node1_2_1_2_2_3.clearUntriedActions();
        Assert.assertTrue(node1_2_1_2_2_3.isFullyExplored());
        Assert.assertFalse(node1_2_1_2_2.isFullyExplored());

        node2_2_1.clearUntriedActions();
        Assert.assertTrue(node2_2_1.isFullyExplored());
        Assert.assertTrue(node2_2.isFullyExplored());
    }

    @Test
    public void getUntriedActionListCopy() {
        setupTree();

        List<Action> actions = node1_2_1_2_2.getUntriedActionListCopy();
        Assert.assertEquals(node1_2_1_2_2.getUntriedActionCount(), actions.size());
        for (int i = 0; i < node1_2_1_2_2.getUntriedActionCount(); i++) {
            Assert.assertEquals(node1_2_1_2_2.getUntriedActionByIndex(i), actions.get(i));
        }

        actions.remove(0);
        Assert.assertEquals(node1_2_1_2_2.getUntriedActionCount() - 1, actions.size());
        for (int i = 0; i < actions.size(); i++) {
            Assert.assertEquals(node1_2_1_2_2.getUntriedActionByIndex(i + 1), actions.get(i));
        }
    }

    @Test
    public void getActionDistribution() {
        setupTree();
        Assert.assertEquals(node1.getActionDistribution().getClass(), Distribution_Equal.class);
        Assert.assertEquals(rootNode.getActionDistribution().getClass(), Distribution_Equal.class);
    }

    @Test
    public void stripUncheckedActionsExceptOnLeaves() {
        setupTree();
        // Clear to way beyond. Only leaves should have unchecked game.action now.
        NodeQWOPExplorableBase.stripUncheckedActionsExceptOnLeaves(node1_2_1_2, 10);
        Assert.assertEquals(0, node1_2_1_2.getUntriedActionCount());
        Assert.assertEquals(0, node1_2_1_2_1.getUntriedActionCount()); // Already failed.
        Assert.assertEquals(0, node1_2_1_2_2.getUntriedActionCount());
        Assert.assertEquals(3, node1_2_1_2_2_3.getUntriedActionCount());

        Assert.assertFalse(node1_2_1_2_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2_2_3.isFullyExplored());
        Assert.assertTrue(node1_2_1_2_1.isFullyExplored());

        // Clear just to the node we're calling it on. Only it should be affected.
        setupTree();
        NodeQWOPExplorableBase.stripUncheckedActionsExceptOnLeaves(node1_2_1_2, 4);
        Assert.assertEquals(0, node1_2_1_2.getUntriedActionCount());
        Assert.assertEquals(0, node1_2_1_2_1.getUntriedActionCount()); // Already failed.
        Assert.assertEquals(2, node1_2_1_2_2.getUntriedActionCount());
        Assert.assertEquals(3, node1_2_1_2_2_3.getUntriedActionCount());

        Assert.assertFalse(node1_2_1_2_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2_2_3.isFullyExplored());
        Assert.assertTrue(node1_2_1_2_1.isFullyExplored());

        // Call with 1 less depth than the node we're calling it on. Nothing should be affected!
        setupTree();
        NodeQWOPExplorableBase.stripUncheckedActionsExceptOnLeaves(node1_2_1_2, 3);
        Assert.assertEquals(1, node1_2_1_2.getUntriedActionCount());
        Assert.assertEquals(0, node1_2_1_2_1.getUntriedActionCount()); // Already failed.
        Assert.assertEquals(2, node1_2_1_2_2.getUntriedActionCount());
        Assert.assertEquals(3, node1_2_1_2_2_3.getUntriedActionCount());

        Assert.assertFalse(node1_2_1_2_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2_2_3.isFullyExplored());
        Assert.assertTrue(node1_2_1_2_1.isFullyExplored());

        // Call with a depth in between so that some, but not all get cleared.
        setupTree();
        NodeQWOPExplorableBase.stripUncheckedActionsExceptOnLeaves(node1_2_1, 4);
        Assert.assertEquals(0, node1_2_1_2.getUntriedActionCount());
        Assert.assertEquals(0, node1_2_1_2_1.getUntriedActionCount()); // Already failed.
        Assert.assertEquals(2, node1_2_1_2_2.getUntriedActionCount());
        Assert.assertEquals(3, node1_2_1_2_2_3.getUntriedActionCount());
        Assert.assertEquals(0, node1_2_1.getUntriedActionCount());

        Assert.assertFalse(node1_2_1.isFullyExplored());
        Assert.assertFalse(node1_2_1_2_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2.isFullyExplored());
        Assert.assertFalse(node1_2_1_2_2_3.isFullyExplored());
        Assert.assertTrue(node1_2_1_2_1.isFullyExplored());
    }

    @Test
    public void destroyNodesBelowAndCheckExplored() {
        setupTree();

        // Node from which nothing should become fully-explored.
        node1_2_1.destroyNodesBelowAndCheckExplored();
        Assert.assertFalse(node1_2_1.isFullyExplored());
        Assert.assertEquals(0, node1_2_1.getChildCount());
        Assert.assertEquals(2, node1_2_1.getUntriedActionCount());

        // Node which becomes fully-explored.
        node2_2.destroyNodesBelowAndCheckExplored();
        Assert.assertTrue(node2_2.isFullyExplored());
        Assert.assertEquals(0, node2_2.getChildCount());
        Assert.assertEquals(0, node2_2.getUntriedActionCount());

        // Node which causes fully-explored status to propagate back beyond the original layer.
        while(node3_1.getUntriedActionCount() > 0) { // Make it so this node only has children and nothing untried.
            node3_1.addDoublyLinkedChild(node3_1.getUntriedActionRandom(), unfailedState);
        }
        node3_1.destroyNodesBelowAndCheckExplored();
        Assert.assertTrue(node3_1.isFullyExplored());
        Assert.assertTrue(node3.isFullyExplored());

        // Keep destroying stuff until the root node is fully-explored.
        node1.destroyNodesBelowAndCheckExplored();
        Assert.assertFalse(rootNode.isFullyExplored()); // Node 2 can still do things.
        node2.addDoublyLinkedChild(node2.getUntriedActionRandom(), unfailedState);
        Assert.assertEquals(0, node2.getUntriedActionCount());
        node2.destroyNodesBelowAndCheckExplored();
        Assert.assertTrue(rootNode.isFullyExplored());
    }

    @Test
    public void reserveExpansionRights() {
        // TODO some multi-threaded tests would be good.
        setupTree();

        Assert.assertTrue(node2_2.reserveExpansionRights());
        Assert.assertFalse(node2_2.reserveExpansionRights()); // Can't re-reserve.
        Assert.assertFalse(node2.isLocked());

        Assert.assertTrue(node2_1.reserveExpansionRights());
        Assert.assertFalse(node2.isLocked());

        Assert.assertFalse(node3.isLocked());
        Assert.assertTrue(node3_1.reserveExpansionRights());
        Assert.assertTrue(node3.isLocked());
        Assert.assertFalse(node3.reserveExpansionRights());
        Assert.assertFalse(node3_3.isLocked());
        Assert.assertFalse(node3_3_1.isLocked());

        node3_1.releaseExpansionRights();
        Assert.assertFalse(node3_1.isLocked());
        Assert.assertFalse(node3.isLocked());

        node2_2.releaseExpansionRights();
        Assert.assertFalse(node2_2.isLocked());
        Assert.assertFalse(node2.isLocked());
        Assert.assertTrue(node2_1.isLocked());
        node2_1.releaseExpansionRights();
        Assert.assertFalse(node2_1.isLocked());
    }

    @Test
    public void getThis() {
        NodeQWOPExplorable node = new NodeQWOPExplorable(initialState);
        Assert.assertEquals(node, node.getThis());
    }
    @Test
    public void addBackwardsLinkedChild() {
        setupTree();

        // Add a node not in the untried action list.
        NodeQWOPExplorable unexpectedNode = node2.addBackwardsLinkedChild(new Action(50, CommandQWOP.Keys.wp), failedState);
        Assert.assertFalse(node2.isFullyExplored()); // Shouldn't be affected by this new node.
        Assert.assertEquals(2, node2.getChildCount()); // Shouldn't affect the child count.
        Assert.assertTrue(unexpectedNode.isFullyExplored());

        // Add a node from within the untried action list.
        NodeQWOPExplorable phantomNode = node2.addBackwardsLinkedChild(node2.getUntriedActionRandom(), failedState);
        Assert.assertFalse(node2.isFullyExplored()); // Shouldn't be affected by this new node.
        Assert.assertEquals(2, node2.getChildCount()); // Shouldn't affect the child count.
        Assert.assertTrue(phantomNode.isFullyExplored());
        Assert.assertEquals(1, node2.getUntriedActionCount());

        // Make sure a real node can be added later which has the same action as the phantom node.
        NodeQWOPExplorable realNode = node2.addDoublyLinkedChild(node2.getUntriedActionRandom(), failedState);
        Assert.assertEquals(0, node2.getUntriedActionCount());
        Assert.assertEquals(3, node2.getChildCount()); // Shouldn't affect the child count.
        
        exception.expect(IllegalArgumentException.class);
        node2.addBackwardsLinkedChild(a4, unfailedState); // Still shouldn't be able to add duplicate game.action, even
        // if it is only backwards linked.
    }
}