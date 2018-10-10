package main;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NodeTest {

    // Root node for our test tree.
    Node rootNode = new Node();

    // Some sample actions.
    Action a1 = new Action(10, true, false, false, true);
    Action a2 = new Action(15, false, true, true, false);
    Action a3 = new Action(12, false, false, false, false);
    Action a4 = new Action(20, false, false, false, false);

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void addChild() {
    }

    @Test
    public void reserveExpansionRights() {
    }

    @Test
    public void releaseExpansionRights() {
    }

    @Test
    public void getLockStatus() {
    }

    @Test
    public void getValue() {
    }

    @Test
    public void setValue() {
    }

    @Test
    public void addToValue() {
    }

    @Test
    public void getParent() {
    }

    @Test
    public void getRandomChild() {
    }

    @Test
    public void getNodesBelow() {
    }

    @Test
    public void getLeaves() {
    }

    @Test
    public void getRoot() {
    }

    @Test
    public void countDescendants() {
    }

    @Test
    public void isOtherNodeAncestor() {
    }

    @Test
    public void destroyAllNodesBelowAndCheckExplored() {
    }

    @Test
    public void getTotalNodeCount() {
    }

    @Test
    public void getImportedNodeCount() {
    }

    @Test
    public void getCreatedNodeCount() {
    }

    @Test
    public void getImportedGameCount() {
    }

    @Test
    public void getCreatedGameCount() {
    }

    @Test
    public void markTerminal() {
    }

    @Test
    public void isFailed() {
    }

    @Test
    public void setFailed() {
    }

    @Test
    public void setState() {
    }

    @Test
    public void getAction() {
    }

    @Test
    public void getSequence() {
    }

    @Test
    public void makeNodesFromRunInfo() {
    }

    @Test
    public void makeNodesFromActionSequences() {
    }

    @Test
    public void calcNodePosBelow() {
    }

    @Test
    public void drawLines_below() {
    }

    @Test
    public void drawNodes_below() {
    }

    @Test
    public void turnOffBranchDisplay() {
    }

    @Test
    public void highlightSingleRunToThisNode() {
    }

    @Test
    public void resetLineBrightness_below() {
    }

    @Test
    public void getColorFromTreeDepth() {
    }

    @Test
    public void getColorFromScaledValue() {
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

    @Test
    public void clearNodeOverrideColor() {
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
}