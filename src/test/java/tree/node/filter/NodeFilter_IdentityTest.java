package tree.node.filter;

import org.jcodec.common.Assert;
import org.junit.Test;
import org.mockito.Mock;
import tree.node.NodeGameExplorable;

import java.util.Arrays;
import java.util.List;

public class NodeFilter_IdentityTest {

    @Mock
    NodeGameExplorable testNode;

    @Test
    public void filter() {
        List<NodeGameExplorable> listOfNodes = Arrays.asList(testNode, testNode, testNode);

        INodeFilter filter = new NodeFilter_Identity();

        // Single nodes should never be filtered out.
        Assert.assertTrue(filter.filter(testNode));

        // List of nodes should maintain their size.
        int originalSize = listOfNodes.size();
        filter.filter(listOfNodes);
        Assert.assertEquals(originalSize, listOfNodes.size());

        NodeGameExplorable badNode = null;
        Assert.assertTrue(filter.filter((NodeGameExplorable) null)); // Should still allow even null.
    }
}