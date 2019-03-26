package goals.simulator_training;

import evaluators.EvaluationFunction_Distance;
import evaluators.IEvaluationFunction;
import game.GameLearned;
import goals.tree_search.MAIN_Search_Template;
import samplers.ISampler;
import samplers.Sampler_Random;
import samplers.Sampler_UCB;
import savers.DataSaver_SendToTraining;
import tree.Node;
import tree.TreeStage;
import tree.TreeStage_SearchForever;
import tree.TreeWorker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TrainSim_Tree {

    DataSaver_SendToTraining dataSaver;
    GameLearned gameLearned;

    TrainSim_Tree() {
        MAIN_Search_Template.assignAllowableActions(-1); // TODO this is gross. Potential actions should be assigned
        // more systematically.
        // Make the net TODO: add loading
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(128);
        layerSizes.add(72);
        layerSizes.add(72);
//        layerSizes.add(72);
        List<String> opts = new ArrayList<>();
        opts.add("--learnrate");
        opts.add("0.000001");
        try {
//            gameLearned = new GameLearned("simulator_graph", layerSizes, opts);
            gameLearned = new GameLearned(new File("src/main/resources/tflow_models/simulator_graph.pb"));
            gameLearned.loadCheckpoint("simchk");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Make the saver which will collect the game state and action info and send it to the learning game.
        dataSaver = new DataSaver_SendToTraining(gameLearned);

        IEvaluationFunction evaluator = new EvaluationFunction_Distance();
        ISampler sampler = new Sampler_Random(); //new Sampler_UCB(evaluator);
        TreeStage treeStage = new TreeStage_SearchForever(sampler, dataSaver);

        Node rootNode = new Node();
        TreeWorker worker = new TreeWorker();
        List<TreeWorker> workers = new ArrayList<>();
        workers.add(worker); // TODO: make it able to take multiple workers. Right now I think they would collide
        // when training.
        treeStage.initialize(workers, rootNode);
    }

    public static void main(String[] args) {
        new TrainSim_Tree();
    }
}
