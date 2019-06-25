package goals.tree_search;

import controllers.Controller_Random;
import game.GameUnified;
import game.action.ActionGenerator_FixedSequence;
import tree.node.NodeQWOPGraphics;
import tree.TreeWorker;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.RolloutPolicy_DeltaScore;

import java.io.File;

public class MAIN_Search_CollectAllGameData extends SearchTemplate {


    public MAIN_Search_CollectAllGameData() {
        super(new File("src/main/resources/config/" + "search.config_long"));
    }

    public static void main(String[] args) {
        MAIN_Search_CollectAllGameData manager = new MAIN_Search_CollectAllGameData();
        manager.doGames();
    }

    private void doGames() {

        NodeQWOPGraphics rootNode = new NodeQWOPGraphics(GameUnified.getInitialState(),
                ActionGenerator_FixedSequence.makeExtendedGenerator(-1));
        ui.addRootNode(rootNode);

        doFixedGamesToFailureStage(rootNode, "good_and_bad", 1, 1000000);
    }

    @Override
    TreeWorker getTreeWorker() {
        return TreeWorker.makeStandardTreeWorker(new Sampler_UCB(
                new EvaluationFunction_Constant(0f),
                new RolloutPolicy_DeltaScore(
                        new EvaluationFunction_Distance(),
                        new Controller_Random())));
    }
}
