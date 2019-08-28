package tree.node;

import game.action.Action;
import game.IGameInternal;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import value.updaters.IValueUpdater;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeGameTest {
    /* Demo tree.
    Tree structure: 27 nodes. Max depth 6 (7 layers, including 0th).

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
    │   │                └── node1_2_1_2_2_4
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
            └── node3_3_4
 */

    // Root node for our test tree.
    private NodeGame<CommandQWOP> rootNode;

    private NodeGame<CommandQWOP> node1, node2, node3, node1_1, node1_2, node1_3, node1_4, node2_1, node2_2, node3_1, node3_2, node3_3,
            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
            node3_3_4, node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_4;


    private List<NodeGame> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;

        // Some sample game.command (mocked).
    private Action<CommandQWOP>
                a1 = new Action<>(10, CommandQWOP.Q),
                a2 = new Action<>(15, CommandQWOP.W),
                a3 = new Action<>(12, CommandQWOP.O),
                a4 = new Action<>(20, CommandQWOP.P);

    // Some states (mocked).
    private StateQWOP initialState = mock(StateQWOP.class);
    private StateQWOP unfailedState = mock(StateQWOP.class);
    private StateQWOP failedState = mock(StateQWOP.class);

    private IGameInternal<CommandQWOP> game = mock(IGameInternal.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    private void setupTree() {

        when(initialState.isFailed()).thenReturn(false);
        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);

        when(game.getCurrentState()).thenReturn(unfailedState);

        // Depth 0.
        rootNode = new NodeGame<>(initialState);
        nodesLvl0 = new ArrayList<>();
        nodesLvl0.add(rootNode);

        // Depth 1.
        node1 = rootNode.addDoublyLinkedChild(a1, unfailedState); // Depth 1, node 1.
        node2 = rootNode.addDoublyLinkedChild(a2, unfailedState); // Depth 1, node 2.
        node3 = rootNode.addDoublyLinkedChild(a3, unfailedState); // Depth 1, node 3.
        nodesLvl1 = new ArrayList<>();
        nodesLvl1.add(node1);
        nodesLvl1.add(node2);
        nodesLvl1.add(node3);

        // Depth 2.
        node1_1 = node1.addDoublyLinkedChild(a1, unfailedState);
        node1_2 = node1.addDoublyLinkedChild(a2, unfailedState);
        node1_3 = node1.addDoublyLinkedChild(a3, unfailedState);
        node1_4 = node1.addDoublyLinkedChild(a4, failedState);

        node2_1 = node2.addDoublyLinkedChild(a1, unfailedState);
        node2_2 = node2.addDoublyLinkedChild(a2, unfailedState);

        node3_1 = node3.addDoublyLinkedChild(a1, unfailedState);
        node3_2 = node3.addDoublyLinkedChild(a2, failedState);
        node3_3 = node3.addDoublyLinkedChild(a3, unfailedState);

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
        node1_1_1 = node1_1.addDoublyLinkedChild(a1, unfailedState);
        node1_1_2 = node1_1.addDoublyLinkedChild(a2, failedState);

        node1_2_1 = node1_2.addDoublyLinkedChild(a1, unfailedState);

        node2_2_1 = node2_2.addDoublyLinkedChild(a1, unfailedState);
        node2_2_2 = node2_2.addDoublyLinkedChild(a2, failedState);
        node2_2_3 = node2_2.addDoublyLinkedChild(a3, failedState);

        node3_3_1 = node3_3.addDoublyLinkedChild(a1, failedState);
        node3_3_2 = node3_3.addDoublyLinkedChild(a2, failedState);
        node3_3_3 = node3_3.addDoublyLinkedChild(a3, failedState);
        node3_3_4 = node3_3.addDoublyLinkedChild(a4, unfailedState);

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
        node1_2_1_2 = node1_2_1.addDoublyLinkedChild(a2, unfailedState);
        nodesLvl4 = new ArrayList<>();
        nodesLvl4.add(node1_2_1_2);

        // Depth 5.
        node1_2_1_2_1 = node1_2_1_2.addDoublyLinkedChild(a1, failedState);
        node1_2_1_2_2 = node1_2_1_2.addDoublyLinkedChild(a2, unfailedState);
        nodesLvl5 = new ArrayList<>();
        nodesLvl5.add(node1_2_1_2_1);
        nodesLvl5.add(node1_2_1_2_2);

        // Depth 6.
        node1_2_1_2_2_4 = node1_2_1_2_2.addDoublyLinkedChild(a4, unfailedState);
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
    public void getState() {
        setupTree();

        Assert.assertEquals(initialState, rootNode.getState());

        Assert.assertEquals(unfailedState, node1.getState());
        Assert.assertEquals(unfailedState, node2.getState());
        Assert.assertEquals(unfailedState, node3.getState());
        Assert.assertEquals(unfailedState, node1_1.getState());
        Assert.assertEquals(unfailedState, node1_2.getState());
        Assert.assertEquals(unfailedState, node1_3.getState());
        Assert.assertEquals(unfailedState, node2_1.getState());
        Assert.assertEquals(unfailedState, node2_2.getState());
        Assert.assertEquals(unfailedState, node3_1.getState());
        Assert.assertEquals(unfailedState, node3_3.getState());
        Assert.assertEquals(unfailedState, node1_1_1.getState());
        Assert.assertEquals(unfailedState, node1_2_1.getState());
        Assert.assertEquals(unfailedState, node2_2_1.getState());
        Assert.assertEquals(unfailedState, node3_3_4.getState());
        Assert.assertEquals(unfailedState, node1_2_1_2.getState());
        Assert.assertEquals(unfailedState, node1_2_1_2_2.getState());
        Assert.assertEquals(unfailedState, node1_2_1_2_2_4.getState());

        Assert.assertEquals(failedState, node1_1_2.getState());
        Assert.assertEquals(failedState, node1_2_1_2_1.getState());
        Assert.assertEquals(failedState, node1_4.getState());
        Assert.assertEquals(failedState, node2_2_2.getState());
        Assert.assertEquals(failedState, node2_2_3.getState());
        Assert.assertEquals(failedState, node3_2.getState());
        Assert.assertEquals(failedState, node3_3_1.getState());
        Assert.assertEquals(failedState, node3_3_2.getState());
        Assert.assertEquals(failedState, node3_3_3.getState());
    }

    @Test
    public void getAction() {
        setupTree();
        Assert.assertNull(rootNode.getAction());

        Assert.assertEquals(a1, node1.getAction());
        Assert.assertEquals(a2, node2.getAction());
        Assert.assertEquals(a3, node3.getAction());
        Assert.assertEquals(a1, node1_1.getAction());
        Assert.assertEquals(a2, node1_2.getAction());
        Assert.assertEquals(a3, node1_3.getAction());
        Assert.assertEquals(a1, node2_1.getAction());
        Assert.assertEquals(a2, node2_2.getAction());
        Assert.assertEquals(a1, node3_1.getAction());
        Assert.assertEquals(a3, node3_3.getAction());
        Assert.assertEquals(a1, node1_1_1.getAction());
        Assert.assertEquals(a1, node1_2_1.getAction());
        Assert.assertEquals(a1, node2_2_1.getAction());
        Assert.assertEquals(a4, node3_3_4.getAction());
        Assert.assertEquals(a2, node1_2_1_2.getAction());
        Assert.assertEquals(a2, node1_2_1_2_2.getAction());
        Assert.assertEquals(a4, node1_2_1_2_2_4.getAction());

        Assert.assertEquals(a2, node1_1_2.getAction());
        Assert.assertEquals(a1, node1_2_1_2_1.getAction());
        Assert.assertEquals(a4, node1_4.getAction());
        Assert.assertEquals(a2, node2_2_2.getAction());
        Assert.assertEquals(a3, node2_2_3.getAction());
        Assert.assertEquals(a2, node3_2.getAction());
        Assert.assertEquals(a1, node3_3_1.getAction());
        Assert.assertEquals(a2, node3_3_2.getAction());
        Assert.assertEquals(a3, node3_3_3.getAction());
    }

    @Test
    public void getSequence() {
        setupTree();
        List<Action<CommandQWOP>> actionList = new ArrayList<>();

        node1_2_1_2_2_4.getSequence(actionList);
        Assert.assertEquals(6, actionList.size());
        Assert.assertEquals(a1, actionList.get(0));
        Assert.assertEquals(a2, actionList.get(1));
        Assert.assertEquals(a1, actionList.get(2));
        Assert.assertEquals(a2, actionList.get(3));
        Assert.assertEquals(a2, actionList.get(4));
        Assert.assertEquals(a4, actionList.get(5));
        actionList.clear();

        node2_2_1.getSequence(actionList);
        Assert.assertEquals(3, actionList.size());
        Assert.assertEquals(a2, actionList.get(0));
        Assert.assertEquals(a2, actionList.get(1));
        Assert.assertEquals(a1, actionList.get(2));
        actionList.clear();

        node3.getSequence(actionList);
        Assert.assertEquals(1, actionList.size());
        Assert.assertEquals(a3, actionList.get(0));

        exception.expect(IndexOutOfBoundsException.class);
        rootNode.getSequence(actionList);
    }

    @Test
    public void makeNodesFromActionSequences() {
        NodeGame<CommandQWOP> root = new NodeGame<>(initialState);
        List<Action<CommandQWOP>[]> sequences = new ArrayList<>();
        sequences.add(new Action[]{a1,a1,a1,a1}); // 4 new nodes.
        sequences.add(new Action[]{a2,a1}); // 2 new nodes
        sequences.add(new Action[]{a2,a1,a4}); // 1 new node.
        sequences.add(new Action[]{a4,a2,a3,a1}); // 4 new nodes.
        sequences.add(new Action[]{a2,a1}); // 0 new nodes.

        NodeGame.makeNodesFromActionSequences(sequences, root, game);
        Assert.assertEquals(11, root.countDescendants());
        Assert.assertEquals(3, root.getChildCount());
        Assert.assertEquals(4, root.getMaxBranchDepth());
    }

    @Test
    public void makeNodesFromRunInfo() {
    // TODO
    //        makeNodesFromRunInfo(Collection< SavableSingleGame > runs,
    //                N existingRootToAddTo)
    }

    @Test
    public void updateValue() {
        IValueUpdater<CommandQWOP> updater = mock(IValueUpdater.class);
        when(updater.update(any(Float.class), any(NodeGame.class))).thenReturn(10f);

        NodeGame<CommandQWOP> root = new NodeGame<>(initialState);
        Assert.assertEquals(0f, root.getValue(), 1e-12f);
        Assert.assertEquals(0, root.getUpdateCount());
        root.updateValue(0f, updater);
        Assert.assertEquals(10f, root.getValue(), 1e-12f);
        Assert.assertEquals(1, root.getUpdateCount());
        root.updateValue(0f, updater);
        Assert.assertEquals(10f, root.getValue(), 1e-12f);
        Assert.assertEquals(2, root.getUpdateCount());
    }

    @Test
    public void addDoublyAndBackwardsLinkedNodes() {
        NodeGame<CommandQWOP> root = new NodeGame<>(initialState),
                dChild1 = root.addDoublyLinkedChild(a1, unfailedState),
                bChild2 = root.addBackwardsLinkedChild(a2, unfailedState),
                bChild1_1 = dChild1.addBackwardsLinkedChild(a1, unfailedState),
                dChild2_1 = bChild2.addDoublyLinkedChild(a1, unfailedState),
                dChild3 = root.addDoublyLinkedChild(a3, unfailedState),
                dChild3_1 = dChild3.addDoublyLinkedChild(a1, unfailedState),
                dChild3_2 = dChild3.addDoublyLinkedChild(a2, unfailedState),
                dChild3_1_1 = dChild3_1.addBackwardsLinkedChild(a1, unfailedState);

        Assert.assertEquals(4, root.countDescendants());
        Assert.assertEquals(0, dChild1.countDescendants());
        Assert.assertEquals(1, bChild2.countDescendants());
        Assert.assertEquals(0, bChild1_1.countDescendants());
        Assert.assertEquals(0, dChild2_1.countDescendants());

        Assert.assertEquals(2, dChild3.countDescendants());
        Assert.assertEquals(0, dChild3_1.countDescendants());
        Assert.assertEquals(0, dChild3_2.countDescendants());
        Assert.assertEquals(0, dChild3_1_1.countDescendants());
    }

    @Test
    public void getThis() {
        NodeGame<CommandQWOP> node = new NodeGame<>(initialState);
        Assert.assertEquals(node, node.getThis());
    }
}