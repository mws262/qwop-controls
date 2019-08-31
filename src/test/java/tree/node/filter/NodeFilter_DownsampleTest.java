package tree.node.filter;

import game.action.Command;
import game.state.IState;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeGameExplorable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeFilter_DownsampleTest {

    private static class DummyCommand extends Command<int[]> {
        public DummyCommand(int[] commandData) {
            super(commandData);
        }
    }

    private static class DummyState implements IState {
        @Override
        public float[] flattenState() {
            return new float[0];
        }
        @Override
        public float getCenterX() {
            return 0;
        }
        @Override
        public boolean isFailed() {
            return false;
        }
        @Override
        public int getStateSize() {
            return 0;
        }
    }

    @Test
    public void filterEvenlySpaced() {
        List<NodeGameExplorable<DummyCommand, DummyState>> nodeList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            @SuppressWarnings("unchecked") NodeGameExplorable<DummyCommand, DummyState> node = mock(NodeGameExplorable.class);
            nodeList.add(node);
            when(node.getTreeDepth()).thenReturn(i); // Each mocked node will return a number in treeDepth of 0-19.
        }

        // Odd number filtered. Not a factor.
        int filterNum = 11;
        INodeFilter<DummyCommand, DummyState> filter = new NodeFilter_Downsample<>(filterNum);
        List<NodeGameExplorable<DummyCommand, DummyState>> testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));

        // Even number filtered. Not a factor.
        filterNum = 12;
        filter = new NodeFilter_Downsample<>(filterNum, NodeFilter_Downsample.Strategy.EVENLY_SPACED);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));

        // Odd number filtered. A factor.
        filterNum = 4;
        filter = new NodeFilter_Downsample<>(filterNum);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        // Should also be evenly spaced if filterNum <= greatest factor of the size of the nodeList.
//        Assert.assertTrue("Should be evenly spaced.", checkEvenSpacing(testList));


        // Even number filtered. A factor.
        filterNum = 10;
        filter = new NodeFilter_Downsample<>(filterNum);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        // Should also be evenly spaced if filterNum <= greatest factor of the size of the nodeList.
//        Assert.assertTrue("Should be evenly spaced.", checkEvenSpacing(testList));

        // Only keep 1.
        filterNum = 1;
        filter = new NodeFilter_Downsample<>(filterNum);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        // Should also be evenly spaced if filterNum <= greatest factor of the size of the nodeList.
//        Assert.assertTrue("Should be evenly spaced.", checkEvenSpacing(testList));

        // Keep all.
        filterNum = 30;
        filter = new NodeFilter_Downsample<>(filterNum);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        // Should also be evenly spaced if filterNum <= greatest factor of the size of the nodeList.
//        Assert.assertTrue("Should be evenly spaced.", checkEvenSpacing(testList));


        // Keep up to much higher.
        filterNum = 100;
        filter = new NodeFilter_Downsample<>(filterNum);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(30, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        // Should also be evenly spaced if filterNum <= greatest factor of the size of the nodeList.
//        Assert.assertTrue("Should be evenly spaced.", checkEvenSpacing(testList));

        // Keep nothing.
        filterNum = 0;
        filter = new NodeFilter_Downsample<>(filterNum);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertTrue("Ordering should be preserved.", checkMonotonic(testList));
        // Should also be evenly spaced if filterNum <= greatest factor of the size of the nodeList.
//        Assert.assertTrue("Should be evenly spaced.", checkEvenSpacing(testList));

    }

    @Test
    public void filterRandom() {
        List<NodeGameExplorable<DummyCommand, DummyState>> nodeList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            @SuppressWarnings("unchecked") NodeGameExplorable<DummyCommand, DummyState> node = mock(NodeGameExplorable.class);
            nodeList.add(node);
            when(node.getTreeDepth()).thenReturn(i); // Each mocked node will return a number in treeDepth of 0-19.
        }

        // Odd number.
        int filterNum = 11;
        INodeFilter<DummyCommand, DummyState> filter = new NodeFilter_Downsample<>(filterNum,
                NodeFilter_Downsample.Strategy.RANDOM);
        List<NodeGameExplorable<DummyCommand, DummyState>> testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));

        // Even number.
        filterNum = 16;
        filter = new NodeFilter_Downsample<>(filterNum, NodeFilter_Downsample.Strategy.RANDOM);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));

        // All
        filterNum = 30;
        filter = new NodeFilter_Downsample<>(filterNum, NodeFilter_Downsample.Strategy.RANDOM);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));

        // Only 1
        filterNum = 1;
        filter = new NodeFilter_Downsample<>(filterNum, NodeFilter_Downsample.Strategy.RANDOM);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(filterNum, testList.size());
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));

        // More than enough.
        filterNum = 500;
        filter = new NodeFilter_Downsample<>(filterNum, NodeFilter_Downsample.Strategy.RANDOM);
        testList = new ArrayList<>(nodeList);
        filter.filter(testList);
        Assert.assertEquals(30, testList.size());
        Assert.assertFalse("No duplicates in filtered list.", checkForDuplicates(testList));
    }

    /**
     * If true, then duplicates.
     */
    private boolean checkForDuplicates(List<NodeGameExplorable<DummyCommand, DummyState>> nodeList) {
        Set<NodeGameExplorable> set = new HashSet<>(nodeList);

        return set.size() < nodeList.size();
    }

    private boolean checkMonotonic(List<NodeGameExplorable<DummyCommand, DummyState>> nodeList) {
        if (nodeList.size() < 2) return true;
        NodeGameExplorable prevNode = nodeList.get(0);
        for (int i = 1; i < nodeList.size(); i++) {
            if (prevNode.getTreeDepth() > nodeList.get(i).getTreeDepth()) return false;
            prevNode = nodeList.get(i);
        }
        return true;
    }

    // TODO doesn't really test the right thing.
//    private boolean checkEvenSpacing(List<NodeGameExplorable<DummyCommand, DummyState>> nodeList) {
//        if (nodeList.size() < 3) return true;
//
//        int firstSpacing = nodeList.get(1).getTreeDepth() - nodeList.get(0).getTreeDepth();
//
//        for (int i = 1; i < nodeList.size() - 1; i++) {
//
//            if (nodeList.get(i + 1).getTreeDepth() - nodeList.get(i).getTreeDepth() != firstSpacing) {
//                return false;
//            }
//        }
//        return true;
//    }
}