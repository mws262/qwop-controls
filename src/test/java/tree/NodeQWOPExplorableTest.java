package tree;

import actions.Action;
import actions.ActionGenerator_FixedSequence;
import actions.ActionList;
import actions.IActionGenerator;
import distributions.Distribution_Equal;
import game.IGame;
import game.State;
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
    │   └── node1_4 *
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

    // Some sample actions (mocked).
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

    private IGame game = mock(IGame.class);
    private IActionGenerator generator;

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    private void setupTree() {

        when(initialState.isFailed()).thenReturn(false);
        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);
        when(game.getCurrentState()).thenReturn(unfailedState);

        // Set up action generator.
        ActionList list1 = ActionList.makeActionSet(new int[]{1,2,3}, new boolean[]{true, false, false, false},
                new Distribution_Equal());
        ActionList list2 = ActionList.makeActionSet(new int[]{4,5,6}, new boolean[]{false, true, false, false},
                new Distribution_Equal());

        generator = new ActionGenerator_FixedSequence(new ActionList[]{list1, list2});
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
        node1_2_1_2_1 = node1_2_1_2.addDoublyLinkedChild(a4, failedState);
        node1_2_1_2_2 = node1_2_1_2.addDoublyLinkedChild(a5, unfailedState);
        nodesLvl5 = new ArrayList<>();
        nodesLvl5.add(node1_2_1_2_1);
        nodesLvl5.add(node1_2_1_2_2);

        // Depth 6.
        Assert.assertEquals(a4, node1_2_1_2_2.getUntriedActionByIndex(0));
        node1_2_1_2_2_3 = node1_2_1_2_2.addDoublyLinkedChild(a3, unfailedState);
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

        // Node which has all failed children and no untried actions should be fully-explored.
        Assert.assertEquals(3, node3_3.getChildCount());
        Assert.assertEquals(0, node3_3.getUntriedActionCount());
        Assert.assertTrue(node3_3.isFullyExplored());

        // Other various nodes which still have potential should not be marked fully-explored.
        Assert.assertFalse(rootNode.isFullyExplored());
        Assert.assertFalse(node1_2_1_2.isFullyExplored());
        Assert.assertFalse(node3.isFullyExplored());

        // Add all failed child actions to a node should make it fully explored and cause correct propagation up the
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
    }

    @Test
    public void getUntriedActionByIndex() {
    }

    @Test
    public void getUntriedActionRandom() {
    }

    @Test
    public void getUntriedActionOnDistribution() {
    }

    @Test
    public void clearUntriedActions() {
    }

    @Test
    public void getUntriedActionListCopy() {
    }

    @Test
    public void getActionDistribution() {
    }

    @Test
    public void stripUncheckedActionsExceptOnLeaves() {
    }

    @Test
    public void destroyNodesBelowAndCheckExplored() {
    }

    @Test
    public void destroyNodesBelow() {
    }

    @Test
    public void reserveExpansionRights() {
    }

    @Test
    public void releaseExpansionRights() {
    }

    @Test
    public void propagateLock() {
    }

    @Test
    public void propagateUnlock() {
    }

    @Test
    public void isLocked() {
    }

    @Test
    public void getThis() {
        NodeQWOPExplorable node = new NodeQWOPExplorable(initialState);
        Assert.assertEquals(node, node.getThis());
    }

    @Test
    public void addDoublyLinkedChild() {
    }

    @Test
    public void addBackwardsLinkedChild() {
    }

    @Test
    public void addDoublyLinkedChild1() {
    }

    @Test
    public void addBackwardsLinkedChild1() {
    }
}