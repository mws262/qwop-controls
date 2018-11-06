package goals.tree_search;

import tree.Node;

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
        assignAllowableActions(10);

        Node rootNode = new Node();
        ui.addRootNode(rootNode);
        Node.includeZOffsetInDrawFiltering = true;

        doBasicMinDepthStage(rootNode, "testDev0.tmp", 1, 1, 10000);
        List<Node> leaves = new ArrayList<>();
        rootNode.getLeaves(leaves);

        for (Node node : leaves) {
            node.resetSweepAngle();
            node.setBranchZOffset(0.1f);
            System.out.println(evaluateNode(node));
            node.setBranchZOffset(-0.5f);
            node.destroyNodesBelow();
//            node.postPruneDrawingBelow(0.4f);
        }
    }

    private float evaluateNode(Node node) {

        // Step 1: Expand all children to depth 1.
        doBasicMinDepthStage(node, "testDev1.tmp", 1, 1, 10000);

        // Step 2: Expand all children. If distance travelled after > threshold and # games played < threshold,
        // "recoverable" child node, otherwise "unrecoverable".
        List<Node> leavesToExpand = new ArrayList<>();
        node.getLeaves(leavesToExpand);

        int successfulExpansionDepth = 0;
        for (Node currentLeaf : leavesToExpand) {
            if (!currentLeaf.isFailed()) {
                currentLeaf.setBranchZOffset(0.2f);
                doBasicMaxDepthStage(currentLeaf, "testDev2.tmp", 100, 1f, 15000);
                currentLeaf.setLineBrightness_below(0.2f);
                currentLeaf.setBranchZOffset(0.0f);

                successfulExpansionDepth += currentLeaf.maxBranchDepth.get() - currentLeaf.getTreeDepth();
            }
        }



        // Step 3: Result -- each child will have a fraction of recoverable children.

        return (float)((float)successfulExpansionDepth/(1.*leavesToExpand.size()));
    }
}
