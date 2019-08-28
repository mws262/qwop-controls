package goals.tree_search;

import controllers.Controller_Random;
import game.qwop.GameQWOP;
import game.action.ActionGenerator_FixedSequence;
import game.qwop.CommandQWOP;
import tree.TreeWorker;
import tree.node.NodeGameGraphics;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.RolloutPolicyBase;
import tree.sampler.rollout.RolloutPolicy_DeltaScore;
import value.updaters.ValueUpdater_Average;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MAIN_Search_Robust extends SearchTemplate {


    private MAIN_Search_Robust(File configFile) {
        super(configFile);
    }

    public static void main(String[] args) {
        MAIN_Search_Robust manager =
                new MAIN_Search_Robust(new File("src/main/resources/config/search.config_full"));
        manager.doGames();
    }

    @Override
    TreeWorker<CommandQWOP> getTreeWorker() {
        return TreeWorker.makeStandardTreeWorker(new Sampler_UCB<>(
                new EvaluationFunction_Constant<>(0f),
                new RolloutPolicy_DeltaScore<>(
                        new EvaluationFunction_Distance<>(),
                        RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                        new Controller_Random<>()), new ValueUpdater_Average<>(), 5, 1)); // TODO hardcoded.
    }

    private void doGames() {
        NodeGameGraphics<CommandQWOP> rootNode = new NodeGameGraphics<>(GameQWOP.getInitialState(),
                ActionGenerator_FixedSequence.makeDefaultGenerator(10));
        ui.addRootNode(rootNode);

        doBasicMinDepthStage(rootNode, "testDev0.tmp", 1, 1, 10000);
        List<NodeGameGraphics<CommandQWOP>> leaves = new ArrayList<>();
        rootNode.getLeaves(leaves);

        for (NodeGameGraphics<CommandQWOP> node : leaves) {
            node.resetSweepAngle();
            node.setBranchZOffset(0.1f);
            float score = evaluateNode(node);
            node.setBranchZOffset(-0.5f);
            node.destroyNodesBelow();
//            node.postPruneDrawingBelow(0.4f);
        }
    }

    private float evaluateNode(NodeGameGraphics<CommandQWOP> node) {

        // Step 1: Expand all children to depth 1.
        doBasicMinDepthStage(node, "testDev1.tmp", 1, 1, 10000);

        // Step 2: Expand all children. If distance travelled after > threshold and # games played < threshold,
        // "recoverable" child node, otherwise "unrecoverable".
        List<NodeGameGraphics<CommandQWOP>> leavesToExpand = new ArrayList<>();
        node.getLeaves(leavesToExpand);

        int successfulExpansionDepth = 0;
        for (NodeGameGraphics<CommandQWOP> currentLeaf : leavesToExpand) {
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
