package tree.node;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

public class NodeGenericTest {
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
    private NodeGeneric rootNode;

    private NodeGeneric node1, node2, node3, node1_1, node1_2, node1_3, node1_4, node2_1, node2_2, node3_1, node3_2, node3_3,
            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
            node3_3_4, node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_4;


    private List<NodeGeneric> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;


    private void setupTree() {

        // Depth 0.
        rootNode = new NodeGeneric();
        nodesLvl0 = new ArrayList<>();
        nodesLvl0.add(rootNode);

        // Depth 1.
        node1 = rootNode.addDoublyLinkedChild(); // Depth 1, node 1.
        node2 = rootNode.addDoublyLinkedChild(); // Depth 1, node 2.
        node3 = rootNode.addDoublyLinkedChild(); // Depth 1, node 3.
        nodesLvl1 = new ArrayList<>();
        nodesLvl1.add(node1);
        nodesLvl1.add(node2);
        nodesLvl1.add(node3);

        // Depth 2.
        node1_1 = node1.addDoublyLinkedChild();
        node1_2 = node1.addDoublyLinkedChild();
        node1_3 = node1.addDoublyLinkedChild();
        node1_4 = node1.addDoublyLinkedChild();

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
    public void addChild() {
        setupTree();

        NodeGeneric node1_1_1_3 = node1_1_1.addDoublyLinkedChild();
        Assert.assertEquals(node1_1_1_3, node1_1_1.getChildByIndex(0));
        Assert.assertEquals(1, node1_1_1.getChildCount());

        NodeGeneric node2_2_4 = node2_2.addDoublyLinkedChild();
        Assert.assertEquals(node2_2_4, node2_2.getChildByIndex(3));
        Assert.assertEquals(4, node2_2.getChildCount());
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

        List<NodeGeneric> childNodes = new ArrayList<>();
        childNodes.add(node3_3_1);
        childNodes.add(node3_3_2);
        childNodes.add(node3_3_3);
        childNodes.add(node3_3_4);

        List<NodeGeneric> childNodesCheckOff = new ArrayList<>(childNodes);
        boolean success = false;
        for (int i = 0; i < 10000; i++) {
            NodeGeneric sampledNode = node3_3.getRandomChild();

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
        List<NodeGeneric> nlist = new ArrayList<>();
        rootNode.getNodesBelowInclusive(nlist);
        Assert.assertEquals(allNodes.size(), nlist.size());
        for (NodeGeneric n : allNodes) {
            Assert.assertTrue(nlist.contains(n));
        }
        nlist.clear();
        rootNode.getNodesBelowInclusive(nlist);
        Assert.assertEquals(27, nlist.size());

        // From another node.
        nlist.clear();
        node2.getNodesBelowInclusive(nlist);
        Assert.assertEquals(6, nlist.size());
        Assert.assertTrue(nlist.contains(node2));
        Assert.assertTrue(nlist.contains(node2_1));
        Assert.assertTrue(nlist.contains(node2_2));
        Assert.assertTrue(nlist.contains(node2_2_1));
        Assert.assertTrue(nlist.contains(node2_2_2));
        Assert.assertTrue(nlist.contains(node2_2_3));
        nlist.clear();

        // From an end node.
        nlist.clear();
        node3_3_3.getNodesBelowInclusive(nlist);
        Assert.assertEquals(1, nlist.size());
        Assert.assertTrue(nlist.contains(node3_3_3));
    }

    @Test
    public void getLeaves() {
        setupTree();
        List<NodeGeneric> nlist = new ArrayList<>();

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
        for (NodeGeneric node : allNodes) { // Should return root regardless of the node getRoot() is called from.
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
    public void getChildren() {
        setupTree();

        List<NodeGeneric> children = rootNode.getChildren();
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
        rootNode.addDoublyLinkedChild();
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
    public void getTreeDepth() {
        setupTree();

        for (NodeGeneric n : nodesLvl0) {
            Assert.assertEquals(0, n.getTreeDepth());
        }
        for (NodeGeneric n : nodesLvl1) {
            Assert.assertEquals(1, n.getTreeDepth());
        }
        for (NodeGeneric n : nodesLvl2) {
            Assert.assertEquals(2, n.getTreeDepth());
        }
        for (NodeGeneric n : nodesLvl3) {
            Assert.assertEquals(3, n.getTreeDepth());
        }
        for (NodeGeneric n : nodesLvl4) {
            Assert.assertEquals(4, n.getTreeDepth());
        }
        for (NodeGeneric n : nodesLvl5) {
            Assert.assertEquals(5, n.getTreeDepth());
        }
        for (NodeGeneric n : nodesLvl6) {
            Assert.assertEquals(6, n.getTreeDepth());
        }
    }

    @Test
    public void getMaxBranchDepth() {
        setupTree();

        Assert.assertEquals(6, rootNode.getMaxBranchDepth()); // Root node.
        Assert.assertEquals(3, node3_3_4.getMaxBranchDepth()); // Leaf node.
        Assert.assertEquals(3, node2_2.getMaxBranchDepth()); // Some middle node.
    }

    @Test
    public void recurseDownTreeInclusive() {
        setupTree();
        List<NodeGeneric> nodes = new ArrayList<>();

        // Root node.
        rootNode.recurseDownTreeInclusive(nodes::add);
        Assert.assertEquals(27, nodes.size());

        // Middle node.
        nodes.clear();
        node1.recurseDownTreeInclusive(nodes::add);
        Assert.assertEquals(12, nodes.size());

        // Another middle node
        nodes.clear();
        node3_3.recurseDownTreeInclusive(nodes::add);
        Assert.assertEquals(5, nodes.size());

        // Leaf node.
        nodes.clear();
        node2_1.recurseDownTreeInclusive(nodes::add);
        Assert.assertEquals(1, nodes.size());
    }

    @Test
    public void recurseDownTreeExclusive() {
        setupTree();
        List<NodeGeneric> nodes = new ArrayList<>();

        // Root node.
        rootNode.recurseDownTreeExclusive(nodes::add);
        Assert.assertEquals(26, nodes.size());

        // Middle node.
        nodes.clear();
        node1.recurseDownTreeExclusive(nodes::add);
        Assert.assertEquals(11, nodes.size());

        // Another middle node
        nodes.clear();
        node3_3.recurseDownTreeExclusive(nodes::add);
        Assert.assertEquals(4, nodes.size());

        // Leaf node.
        nodes.clear();
        node2_1.recurseDownTreeExclusive(nodes::add);
        Assert.assertEquals(0, nodes.size());
    }

    @Test
    public void recurseUpTreeInclusive() {
        setupTree();
        List<NodeGeneric> nodes = new ArrayList<>();

        // Root node.
        rootNode.recurseUpTreeInclusive(nodes::add);
        Assert.assertEquals(1, nodes.size());
        Assert.assertTrue(nodes.contains(rootNode));

        // Middle node.
        nodes.clear();
        node1.recurseUpTreeInclusive(nodes::add);
        Assert.assertEquals(2, nodes.size());
        Assert.assertTrue(nodes.contains(rootNode));
        Assert.assertTrue(nodes.contains(node1));

        // Another middle node
        nodes.clear();
        node3_3.recurseUpTreeInclusive(nodes::add);
        Assert.assertEquals(3, nodes.size());
        Assert.assertTrue(nodes.contains(rootNode));
        Assert.assertTrue(nodes.contains(node3));
        Assert.assertTrue(nodes.contains(node3_3));

        // Leaf node.
        nodes.clear();
        node2_1.recurseUpTreeInclusive(nodes::add);
        Assert.assertEquals(3, nodes.size());
        Assert.assertTrue(nodes.contains(rootNode));
        Assert.assertTrue(nodes.contains(node2));
        Assert.assertTrue(nodes.contains(node2_1));
    }

    @Test
    public void recurseUpTreeInclusiveNoRoot() {
        setupTree();
        List<NodeGeneric> nodes = new ArrayList<>();

        // Root node.
        rootNode.recurseUpTreeInclusiveNoRoot(nodes::add);
        Assert.assertEquals(0, nodes.size());

        // Middle node.
        nodes.clear();
        node1.recurseUpTreeInclusiveNoRoot(nodes::add);
        Assert.assertEquals(1, nodes.size());
        Assert.assertFalse(nodes.contains(rootNode));
        Assert.assertTrue(nodes.contains(node1));

        // Another middle node
        nodes.clear();
        node3_3.recurseUpTreeInclusiveNoRoot(nodes::add);
        Assert.assertEquals(2, nodes.size());
        Assert.assertFalse(nodes.contains(rootNode));
        Assert.assertTrue(nodes.contains(node3));
        Assert.assertTrue(nodes.contains(node3_3));

        // Leaf node.
        nodes.clear();
        node2_1.recurseUpTreeInclusiveNoRoot(nodes::add);
        Assert.assertEquals(2, nodes.size());
        Assert.assertFalse(nodes.contains(rootNode));
        Assert.assertTrue(nodes.contains(node2));
        Assert.assertTrue(nodes.contains(node2_1));
    }

    @Test
    public void applyToLeaves() {
        setupTree();
        List<NodeGeneric> nodes = new ArrayList<>();

        // From root
        rootNode.applyToLeavesBelow(nodes::add);
        Assert.assertEquals(16, nodes.size());

        // From leaf.
        nodes.clear();
        node2_1.applyToLeavesBelow(nodes::add);
        Assert.assertEquals(1, nodes.size());

        // From middle.
        nodes.clear();
        node1_2.applyToLeavesBelow(nodes::add);
        Assert.assertEquals(2, nodes.size());
    }

    @Test
    public void destroyNodesBelow() {
        setupTree();

        node3.destroyNodesBelow();
        Assert.assertEquals(0, node3.getChildCount());
        Assert.assertEquals(3, rootNode.getChildCount());

        List<NodeGeneric> nodes = new ArrayList<>();
        rootNode.getNodesBelowInclusive(nodes);
        Assert.assertEquals(20, nodes.size());

        node2_2_3.destroyNodesBelow(); // It's a leaf, so it shouldn't affect anything.
        nodes.clear();
        rootNode.getNodesBelowInclusive(nodes);
        Assert.assertEquals(20, nodes.size());

        rootNode.destroyNodesBelow();
        Assert.assertEquals(0, rootNode.getChildCount());
        nodes.clear();
        rootNode.getNodesBelowInclusive(nodes);
        Assert.assertEquals(1, nodes.size());
    }

    @Test
    public void removeFromChildren() {
        setupTree();

        node2.removeFromChildren(node2_1);
        Assert.assertEquals(1, node2.getChildCount());
        Assert.assertFalse(node2.getChildren().contains(node2_1));

        int node1ChildCount = node1.getChildCount();
        node1.removeFromChildren(rootNode);
        Assert.assertEquals(node1ChildCount, node1.getChildCount());
    }

    @Test
    public void addDoublyAndBackwardsLinkedNodes() {
        NodeGeneric root = new NodeGeneric();
        NodeGeneric dChild1 = root.addDoublyLinkedChild();
        NodeGeneric bChild2 = root.addBackwardsLinkedChild();
        NodeGeneric bChild1_1 = dChild1.addBackwardsLinkedChild();
        NodeGeneric dChild2_1 = bChild2.addDoublyLinkedChild();
        NodeGeneric dChild3 = root.addDoublyLinkedChild();
        NodeGeneric dChild3_1 = dChild3.addDoublyLinkedChild();
        NodeGeneric dChild3_2 = dChild3.addDoublyLinkedChild();
        NodeGeneric dChild3_1_1 = dChild3_1.addBackwardsLinkedChild();

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
}