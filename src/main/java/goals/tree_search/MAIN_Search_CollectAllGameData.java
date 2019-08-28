package goals.tree_search;

import controllers.Controller_Random;
import game.qwop.GameQWOP;
import game.action.ActionGenerator_FixedSequence;
import game.qwop.CommandQWOP;
import tree.node.NodeQWOPGraphics;
import tree.TreeWorker;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.RolloutPolicyBase;
import tree.sampler.rollout.RolloutPolicy_DeltaScore;
import value.updaters.ValueUpdater_Average;

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

        NodeQWOPGraphics<CommandQWOP> rootNode = new NodeQWOPGraphics<>(
                GameQWOP.getInitialState(),
                ActionGenerator_FixedSequence.makeExtendedGenerator(-1));
        ui.addRootNode(rootNode);

        doFixedGamesToFailureStage(rootNode, "good_and_bad", 1, 1000000);
    }

    @Override
    TreeWorker<CommandQWOP> getTreeWorker() {
        return TreeWorker.makeStandardTreeWorker(new Sampler_UCB<>(
                new EvaluationFunction_Constant<>(0f),
                new RolloutPolicy_DeltaScore<>(
                        new EvaluationFunction_Distance<>(),
                        RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                        new Controller_Random<>()),
                new ValueUpdater_Average<>(),
                5,
                1));
    }
}
