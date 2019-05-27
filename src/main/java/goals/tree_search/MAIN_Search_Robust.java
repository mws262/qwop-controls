package goals.tree_search;

import game.GameUnified;
import tree.NodeQWOPGraphics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MAIN_Search_Robust extends MAIN_Search_Template {


    private MAIN_Search_Robust(File configFile) {
        super(configFile);
    }

    public static void main(String[] args) {
        MAIN_Search_Robust manager =
                new MAIN_Search_Robust(new File("src/main/resources/config/search.config_full"));
        manager.doGames();
    }

    private void doGames() {
        NodeQWOPGraphics rootNode = new NodeQWOPGraphics(GameUnified.getInitialState(), getDefaultActionGenerator(10));
        ui.addRootNode(rootNode);

        doBasicMinDepthStage(rootNode, "testDev0.tmp", 1, 1, 10000);
        List<NodeQWOPGraphics> leaves = new ArrayList<>();
        rootNode.getLeaves(leaves);

        for (NodeQWOPGraphics node : leaves) {
            node.resetSweepAngle();
            node.setBranchZOffset(0.1f);
            float score = evaluateNode(node);
            System.out.println(score);
            node.setBranchZOffset(-0.5f);
            node.destroyNodesBelow();
//            node.postPruneDrawingBelow(0.4f);
        }
    }

    private float evaluateNode(NodeQWOPGraphics node) {

        // Step 1: Expand all children to depth 1.
        doBasicMinDepthStage(node, "testDev1.tmp", 1, 1, 10000);

        // Step 2: Expand all children. If distance travelled after > threshold and # games played < threshold,
        // "recoverable" child node, otherwise "unrecoverable".
        List<NodeQWOPGraphics> leavesToExpand = new ArrayList<>();
        node.getLeaves(leavesToExpand);

        int successfulExpansionDepth = 0;
        for (NodeQWOPGraphics currentLeaf : leavesToExpand) {
            if (!currentLeaf.getState().isFailed()) {
                currentLeaf.setBranchZOffset(0.2f);
                doBasicMaxDepthStage(currentLeaf, "testDev2.tmp", 100, 1f, 15000);
                currentLeaf.setLineBrightnessBelow(0.2f);
                currentLeaf.setBranchZOffset(0.0f);

                successfulExpansionDepth += currentLeaf.getMaxBranchDepth() - currentLeaf.getTreeDepth();
            }
        }

        // Step 3: Result -- each child will have a fraction of recoverable children.
        return (float)((float)successfulExpansionDepth/(1.*leavesToExpand.size()));
    }
}
