//package filters;
//
//import actions.Action;
//import game.State;
//import org.junit.Assert;
//import org.junit.Test;
//import tree.Node;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class NodeFilter_MissingInfoTest {
//    /* Demo tree.
//    Tree structure: 27 nodes. Max depth 6 (7 layers, including 0th).
//
//    rootNode
//    ├── node1
//    │   ├── node1_1
//    │   │    ├── node1_1_1
//    │   │    └── node1_1_2
//    │   ├── node1_2
//    │   │    └── node1_2_1
//    │   │        └── node1_2_1_2
//    │   │            │── node1_2_1_2_1
//    │   │            └── node1_2_1_2_2
//    │   │                └── node1_2_1_2_2_4
//    │   ├── node1_3
//    │   └── node1_4
//    ├── node2
//    │   ├── node2_1
//    │   └── node2_2
//    │       ├── node2_2_1
//    │       ├── node2_2_2
//    │       └── node2_2_3
//    └── node3
//        ├── node3_1
//        ├── node3_2
//        └── node3_3
//            ├── node3_3_1
//            ├── node3_3_2
//            ├── node3_3_3
//            └── node3_3_4
//     */
//
//    // Root node for our test tree.
//    private Node rootNode;
//
//    private Node node1, node2, node3, node1_1, node1_2, node1_3, node1_4, node2_1, node2_2, node3_1, node3_2, node3_3,
//            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
//            node3_3_4, node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_4;
//
//
//    private List<Node> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;
//
//    // Some sample actions (mocked).
//    private Action a1 = mock(Action.class);
//    private Action a2 = mock(Action.class);
//    private Action a3 = mock(Action.class);
//    private Action a4 = mock(Action.class);
//
//    // Some states (mocked).
//    private State unfailedState = mock(State.class);
//    private State failedState = mock(State.class);
//
//
//    private void setupTree() {
//        // Canned mock Action return values.
//        when(a1.getTimestepsTotal()).thenReturn(10);
//        when(a2.getTimestepsTotal()).thenReturn(15);
//        when(a3.getTimestepsTotal()).thenReturn(12);
//        when(a4.getTimestepsTotal()).thenReturn(20);
//
//        when(unfailedState.isFailed()).thenReturn(false);
//        when(failedState.isFailed()).thenReturn(true);
//
//        // Depth 0.
//        rootNode = new Node();
//        nodesLvl0 = new ArrayList<>();
//        nodesLvl0.add(rootNode);
//
//        // Depth 1.
//        node1 = rootNode.addChild(a1); // Depth 1, node 1.
//        node2 = rootNode.addChild(a2); // Depth 1, node 2.
//        node3 = rootNode.addChild(a3); // Depth 1, node 3.
//        nodesLvl1 = new ArrayList<>();
//        nodesLvl1.add(node1);
//        nodesLvl1.add(node2);
//        nodesLvl1.add(node3);
//
//        // Depth 2.
//        node1_1 = node1.addChild(a1);
//        node1_2 = node1.addChild(a2);
//        node1_3 = node1.addChild(a3);
//        node1_4 = node1.addChild(a4);
//
//        node2_1 = node2.addChild(a1);
//        node2_2 = node2.addChild(a2);
//
//        node3_1 = node3.addChild(a1);
//        node3_2 = node3.addChild(a2);
//        node3_3 = node3.addChild(a3);
//
//        nodesLvl2 = new ArrayList<>();
//        nodesLvl2.add(node1_1);
//        nodesLvl2.add(node1_2);
//        nodesLvl2.add(node1_3);
//        nodesLvl2.add(node1_4);
//        nodesLvl2.add(node2_1);
//        nodesLvl2.add(node2_2);
//        nodesLvl2.add(node3_1);
//        nodesLvl2.add(node3_2);
//        nodesLvl2.add(node3_3);
//
//        // Depth 3.
//        node1_1_1 = node1_1.addChild(a1);
//        node1_1_2 = node1_1.addChild(a2);
//
//        node1_2_1 = node1_2.addChild(a1);
//
//        node2_2_1 = node2_2.addChild(a1);
//        node2_2_2 = node2_2.addChild(a2);
//        node2_2_3 = node2_2.addChild(a3);
//
//        node3_3_1 = node3_3.addChild(a1);
//        node3_3_2 = node3_3.addChild(a2);
//        node3_3_3 = node3_3.addChild(a3);
//        node3_3_4 = node3_3.addChild(a4);
//
//        nodesLvl3 = new ArrayList<>();
//        nodesLvl3.add(node1_1_1);
//        nodesLvl3.add(node1_1_2);
//        nodesLvl3.add(node1_2_1);
//        nodesLvl3.add(node2_2_1);
//        nodesLvl3.add(node2_2_2);
//        nodesLvl3.add(node2_2_3);
//        nodesLvl3.add(node3_3_1);
//        nodesLvl3.add(node3_3_2);
//        nodesLvl3.add(node3_3_3);
//        nodesLvl3.add(node3_3_4);
//
//        // Depth 4.
//        node1_2_1_2 = node1_2_1.addChild(a2);
//        nodesLvl4 = new ArrayList<>();
//        nodesLvl4.add(node1_2_1_2);
//
//        // Depth 5.
//        node1_2_1_2_1 = node1_2_1_2.addChild(a1);
//        node1_2_1_2_2 = node1_2_1_2.addChild(a2);
//        nodesLvl5 = new ArrayList<>();
//        nodesLvl5.add(node1_2_1_2_1);
//        nodesLvl5.add(node1_2_1_2_2);
//
//        // Depth 6.
//        node1_2_1_2_2_4 = node1_2_1_2_2.addChild(a4);
//        nodesLvl6 = new ArrayList<>();
//        nodesLvl6.add(node1_2_1_2_2_4);
//
//        allNodes = new ArrayList<>();
//        allNodes.addAll(nodesLvl0);
//        allNodes.addAll(nodesLvl1);
//        allNodes.addAll(nodesLvl2);
//        allNodes.addAll(nodesLvl3);
//        allNodes.addAll(nodesLvl4);
//        allNodes.addAll(nodesLvl5);
//        allNodes.addAll(nodesLvl6);
//    }
//    @Test
//    public void filter() {
//        setupTree();
//
//        List<Node> nodesWithStates = new ArrayList<>();
//        for (int i = 0; i < allNodes.size(); i++) {
//            if (i % 2 == 1) {
//                allNodes.get(i).setState(unfailedState);
//                nodesWithStates.add(allNodes.get(i));
//            }
//        }
//
//        INodeFilter nodeFilter = new NodeFilter_MissingInfo();
//
//        nodeFilter.filter(allNodes);
//
//        Assert.assertEquals(nodesWithStates.size(), allNodes.size());
//        for (Node n : nodesWithStates) {
//            Assert.assertTrue(allNodes.contains(n));
//        }
//
//        Assert.assertFalse(nodeFilter.filter((Node) null)); // Null nodes should return false.
//
//        nodesWithStates.clear();
//        nodeFilter.filter(nodesWithStates); // Should tolerate an empty list.
//        Assert.assertEquals(0, nodesWithStates.size());
//    }
//}