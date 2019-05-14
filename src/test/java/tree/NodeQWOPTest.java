package tree;

import actions.Action;
import game.State;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeQWOPTest {
    // Root node for our test tree.
    private NodeQWOP rootNode;

    private NodeQWOP node1, node2, node3, node1_1, node1_2, node1_3, node1_4, node2_1, node2_2, node3_1, node3_2, node3_3,
            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
            node3_3_4, node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_4;


    private List<NodeQWOP> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;

        // Some sample actions (mocked).
    private Action a1 = mock(Action.class);
    private Action a2 = mock(Action.class);
    private Action a3 = mock(Action.class);
    private Action a4 = mock(Action.class);

    // Some states (mocked).
    private State initialState = mock(State.class);
    private State unfailedState = mock(State.class);
    private State failedState = mock(State.class);


    private void setupTree() {
        // Canned mock Action return values.
        when(a1.getTimestepsTotal()).thenReturn(10);
        when(a2.getTimestepsTotal()).thenReturn(15);
        when(a3.getTimestepsTotal()).thenReturn(12);
        when(a4.getTimestepsTotal()).thenReturn(20);

        when(initialState.isFailed()).thenReturn(false);
        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);

        // Depth 0.
        rootNode = new NodeQWOP(initialState);
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
        node1_4 = node1.addDoublyLinkedChild(a4, unfailedState);

        node2_1 = node2.addDoublyLinkedChild();
        node2_2 = node2.addDoublyLinkedChild();

        node3_1 = node3.addDoublyLinkedChild();
        node3_2 = node3.addDoublyLinkedChild();
        node3_3 = node3.addDoublyLinkedChild();

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
        node1_1_1 = node1_1.addDoublyLinkedChild();
        node1_1_2 = node1_1.addDoublyLinkedChild();

        node1_2_1 = node1_2.addDoublyLinkedChild();

        node2_2_1 = node2_2.addDoublyLinkedChild();
        node2_2_2 = node2_2.addDoublyLinkedChild();
        node2_2_3 = node2_2.addDoublyLinkedChild();

        node3_3_1 = node3_3.addDoublyLinkedChild();
        node3_3_2 = node3_3.addDoublyLinkedChild();
        node3_3_3 = node3_3.addDoublyLinkedChild();
        node3_3_4 = node3_3.addDoublyLinkedChild();

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
        node1_2_1_2 = node1_2_1.addDoublyLinkedChild();
        nodesLvl4 = new ArrayList<>();
        nodesLvl4.add(node1_2_1_2);

        // Depth 5.
        node1_2_1_2_1 = node1_2_1_2.addDoublyLinkedChild();
        node1_2_1_2_2 = node1_2_1_2.addDoublyLinkedChild();
        nodesLvl5 = new ArrayList<>();
        nodesLvl5.add(node1_2_1_2_1);
        nodesLvl5.add(node1_2_1_2_2);

        // Depth 6.
        node1_2_1_2_2_4 = node1_2_1_2_2.addDoublyLinkedChild();
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
    }

    @Test
    public void getAction() {
    }

    @Test
    public void getSequence() {
    }

    @Test
    public void makeNodesFromActionSequences() {
    }

    @Test
    public void makeNodesFromRunInfo() {
    }

    @Test
    public void updateValue() {
    }

    @Test
    public void getValue() {
    }

    @Test
    public void getUpdateCount() {
    }

    @Test
    public void addDoublyLinkedChild() {
    }

    @Test
    public void addBackwardsLinkedChild() {
    }

    @Test
    public void getThis() {
    }
}