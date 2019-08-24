package tree.node;

import game.action.*;
import distributions.Distribution_Equal;
import game.IGameInternal;
import game.state.State;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NodeQWOPGraphicsTest {
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
    private NodeQWOPGraphics<CommandQWOP> rootNode;

    private NodeQWOPGraphics<CommandQWOP> node1, node2, node3, node1_1, node1_2, node1_3, node2_1, node2_2, node3_1, node3_2, node3_3,
            node1_1_1, node1_1_2, node1_2_1, node2_2_1, node2_2_2, node2_2_3, node3_3_1, node3_3_2, node3_3_3,
            node1_2_1_2, node1_2_1_2_1, node1_2_1_2_2, node1_2_1_2_2_3;


    private List<NodeQWOPGraphics<CommandQWOP>> allNodes, nodesLvl0, nodesLvl1, nodesLvl2, nodesLvl3, nodesLvl4, nodesLvl5, nodesLvl6;

    // Some sample game.action (mocked).
    private Action<CommandQWOP> a1, a2, a3, a4, a5, a6;

    // Some states (mocked).
    private State
            initialState = mock(State.class),
            unfailedState = mock(State.class),
            failedState = mock(State.class);

    private IGameInternal game = mock(IGameInternal.class);
    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    private void setupTree() {
        when(initialState.isFailed()).thenReturn(false);
        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);
        when(game.getCurrentState()).thenReturn(unfailedState);

        // Set up action generator.
        ActionList<CommandQWOP> list1 = ActionList.makeActionList(new int[]{1,2,3}, CommandQWOP.Q,
                new Distribution_Equal<>());
        ActionList<CommandQWOP> list2 = ActionList.makeActionList(new int[]{4,5,6}, CommandQWOP.W,
                new Distribution_Equal<>());

        IActionGenerator<CommandQWOP> generator = new ActionGenerator_FixedSequence<>(new ActionList[]{list1, list2});
        a1 = list1.get(0);
        a2 = list1.get(1);
        a3 = list1.get(2);

        a4 = list2.get(0);
        a5 = list2.get(1);
        a6 = list2.get(2);

        // Depth 0.
        rootNode = new NodeQWOPGraphics<>(initialState, generator);
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
    public void drawLine() {
        // Unfortunately, Mockito can't mock GL2. It's too big. We're stuck doing this, which just tests to make sure
        // errors don't occur.
//        setupTree();
//
//        rootNode.drawLine(gl);
//        node1.drawLine(gl);
//        node1_2_1_2_2_3.drawLine(gl);
    }

    @Test
    public void drawPoint() {
        // Unfortunately, Mockito can't mock GL2. It's too big. We're stuck doing this, which just tests to make sure
        // errors don't occur.
//        setupTree();
//
//        rootNode.drawPoint(gl);
//        rootNode.displayPoint = true;
//        rootNode.drawPoint(gl);
//
//        node1.drawPoint(gl);
//        node1.displayPoint = true;
//        node1.drawPoint(gl);
//
//        node1_2_1_2_2_3.drawPoint(gl);
//        node1_2_1_2_2_3.displayPoint = true;
//        node1_2_1_2_2_3.drawPoint(gl);
    }

    @Test
    public void drawLinesBelow() {
        // Unfortunately, Mockito can't mock GL2. It's too big. We're stuck doing this, which just tests to make sure
        // errors don't occur.

//        rootNode.drawLinesBelow(gl);
//        node1.drawLinesBelow(gl);
//        node1_2_1_2_2_3.drawLinesBelow(gl);
//
//        rootNode.recurseDownTreeInclusive(n -> n.displayLine = false);
//        rootNode.drawLinesBelow(gl);
//        node1.drawLinesBelow(gl);
//        node1_2_1_2_2_3.drawLinesBelow(gl);
    }

    @Test
    public void drawNodesBelow() {
        // Unfortunately, Mockito can't mock GL2. It's too big. We're stuck doing this, which just tests to make sure
        // errors don't occur.

//        rootNode.drawPointsBelow(gl);
//        node1.drawPointsBelow(gl);
//        node1_2_1_2_2_3.drawPointsBelow(gl);
//
//        rootNode.recurseDownTreeInclusive(n -> n.displayPoint = false);
//        rootNode.drawPointsBelow(gl);
//        node1.drawPointsBelow(gl);
//        node1_2_1_2_2_3.drawPointsBelow(gl);
    }

    @Test
    public void getColorFromTreeDepth() {
        // Should succeed for basically any value, even those which are not normally intended.
        Color c = NodeQWOPGraphics.getColorFromTreeDepth(0, 0.6f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromTreeDepth(-1, 1f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromTreeDepth(10, 10f);
        Assert.assertNotNull(c);
    }

    @Test
    public void getColorFromScaledValue() {
        // Should succeed for basically any value, even those which are not normally intended.
        Color c = NodeQWOPGraphics.getColorFromScaledValue(0, 10, 0.8f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromScaledValue(0, 0, 0.8f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromScaledValue(1, 0, 0.8f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromScaledValue(-10, -1, 0.8f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromScaledValue(1, 6, 10f);
        Assert.assertNotNull(c);
        c = NodeQWOPGraphics.getColorFromScaledValue(1,1,0);
        Assert.assertNotNull(c);
    }

    @Test
    public void setLineColor() {
        setupTree();

        // Pick a color to change some lines to.
        float[] pink = Color.PINK.getColorComponents(null);
        Assert.assertEquals(3, pink.length);

        // Get colors before the change.
        float[] node1LineColor = getLineColorFloats(node1);
        Assert.assertEquals(3, node1LineColor.length);
        float[] rootNodeLineColor = getLineColorFloats(rootNode);
        float[] node1_1LineColor = getLineColorFloats(node1_1);

        // Check that the colors are different beforehand.
        Assert.assertFalse(Arrays.equals(pink, node1LineColor));
        node1.setLineColor(Color.PINK);
        // Check that they are the same after.
        node1LineColor = getLineColorFloats(node1);
        Assert.assertArrayEquals(pink, node1LineColor, 0.0f);

        // Make sure that the parent and child of the changed node are not affected.
        Assert.assertArrayEquals(rootNodeLineColor, getLineColorFloats(rootNode), 0.0f);
        Assert.assertArrayEquals(node1_1LineColor, getLineColorFloats(node1_1), 0.0f);

        // Try root now.
        rootNode.setLineColor(Color.PINK);
        Assert.assertArrayEquals(pink, getLineColorFloats(rootNode), 0.0f);

        // Try the other child now.
        node1_1.setLineColor(Color.PINK);
        Assert.assertArrayEquals(pink, getLineColorFloats(node1_1), 0.0f);
    }

    @Test
    public void setOverrideLineColor() {
        setupTree();
        // Pick a color to change some lines to.
        float[] pink = Color.PINK.getColorComponents(null);
        Assert.assertEquals(3, pink.length);

        // Get the override colors before the change.
        float[] node1LineColorOverride = getOverrideLineColorFloats(node1);
        Assert.assertNull(node1LineColorOverride); // Null until set.
        float[] rootNodeLineColorOverride = getOverrideLineColorFloats(rootNode);
        Assert.assertNull(rootNodeLineColorOverride);
        float[] node1_1LineColorOverride = getOverrideLineColorFloats(node1_1);
        Assert.assertNull(node1_1LineColorOverride);

        // Get the normal colors before the change.
        float[] node1LineColor = getLineColorFloats(node1);
        Assert.assertEquals(3, node1LineColor.length);
        float[] rootNodeLineColor = getLineColorFloats(rootNode);
        float[] node1_1LineColor = getLineColorFloats(node1_1);

        // Change override color.
        node1.setOverrideLineColor(Color.PINK);
        // Check that they are the same after.
        node1LineColorOverride = getOverrideLineColorFloats(node1);
        Assert.assertArrayEquals(pink, node1LineColorOverride, 0.0f);

        // Override colors for parent and child should remain null.
        Assert.assertNull(rootNodeLineColorOverride);
        Assert.assertNull(node1_1LineColorOverride);

        // Make sure that the parent and child of the changed node are not affected.
        Assert.assertArrayEquals(rootNodeLineColor, getLineColorFloats(rootNode), 0.0f);
        Assert.assertArrayEquals(node1_1LineColor, getLineColorFloats(node1_1), 0.0f);

        // Try root now.
        rootNode.setOverrideLineColor(Color.PINK);
        Assert.assertArrayEquals(pink, getOverrideLineColorFloats(rootNode), 0.0f);

        // Try the other child now.
        node1_1.setOverrideLineColor(Color.PINK);
        Assert.assertArrayEquals(pink, getOverrideLineColorFloats(node1_1), 0.0f);
    }

    @Test
    public void setOverridePointColor() {
        setupTree();

        for (NodeQWOPGraphics node : allNodes) {
            Color color = Color.ORANGE;
            float[] colorComponents = color.getColorComponents(null);
            node.setOverridePointColor(color);

            Assert.assertArrayEquals(colorComponents, getOverridePointColorFloats(node), 1e-8f);
        }
    }

    @Test
    public void setLineBrightness() {
    }

    @Test
    public void turnOffBranchDisplay() {
    }

    @Test
    public void highlightSingleRunToThisNode() {
    }

    @Test
    public void setLineBrightnessBelow() {
    }

    @Test
    public void resetSweepAngle() {
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

    /**
     * Use reflection to get this private field.
     * @param node Node to fetch from.
     * @return 3-element float array representing the line color.
     */
    private static float[] getLineColorFloats(NodeQWOPGraphicsBase<?, CommandQWOP> node) {
        return (float[])getPrivateField(NodeQWOPGraphicsBase.class, node, "lineColorFloats");
    }

    private static float[] getOverrideLineColorFloats(NodeQWOPGraphicsBase<?, CommandQWOP> node) {
        return (float[])getPrivateField(NodeQWOPGraphicsBase.class, node, "overrideLineColorFloats");
    }

    private static float[] getPointColorFloats(NodeQWOPGraphicsBase<?, CommandQWOP> node) {
        return (float[])getPrivateField(NodeQWOPGraphicsBase.class, node, "pointColorFloats");
    }

    private static float[] getOverridePointColorFloats(NodeQWOPGraphicsBase<?, CommandQWOP> node) {
        return (float[])getPrivateField(NodeQWOPGraphicsBase.class, node, "overridePointColorFloats");
    }

    private static Object getPrivateField(Class clazz, Object object, String fieldName) {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}