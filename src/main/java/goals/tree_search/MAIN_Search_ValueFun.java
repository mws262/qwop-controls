package goals.tree_search;

import actions.Action;
import evaluators.EvaluationFunction_Constant;
import evaluators.EvaluationFunction_Distance;
import game.GameUnified;
import samplers.Sampler_UCB;
import samplers.rollout.RolloutPolicy;
import samplers.rollout.RolloutPolicy_ValueFunction;
import savers.DataSaver_StageSelected;
import tree.Node;
import tree.TreeStage_MaxDepth;
import tree.TreeWorker;
import tree.Utility;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MAIN_Search_ValueFun extends MAIN_Search_Template {

    public MAIN_Search_ValueFun(File configFile) {
        super(configFile);
    }

    public static void main(String[] args) {
        MAIN_Search_ValueFun manager = new MAIN_Search_ValueFun(
                new File("src/main/resources/config/" + "search.config_value"));
        manager.doGames();
    }

    @SuppressWarnings("Duplicates")
    public void doGames() {

        // Load parameters from config file.
        Sampler_UCB.explorationMultiplier = Float.parseFloat(properties.getProperty("UCBExplorationMultiplier", "1"));
        float maxWorkerFraction = Float.parseFloat(properties.getProperty("fractionOfMaxWorkers", "1"));
        int bailAfterXGames = Integer.parseInt(properties.getProperty("bailAfterXGames", "100000"));
        int getToSteadyDepth = Integer.parseInt(properties.getProperty("getToSteadyDepth", "100"));
        int netTrainingStepsPerIter = Integer.parseInt(properties.getProperty("netTrainingStepsPerIter", "20"));


        int numWorkersToUse = (int) Math.max(1, maxWorkerFraction * maxWorkers);

        // Make new tree root and assign to GUI.
        // Assign default available actions.
        assignAllowableActionsWider(-1);
        Node rootNode = new Node();
        Node.pointsToDraw.clear();
        ui.clearRootNodes();
        ui.addRootNode(rootNode);

        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(1,false,false,false,false),
                new Action(34,false,true,true,false),
                new Action(19,false,false,false,false),
                new Action(45,true,false,false,true),

                new Action(10,false,false,false,false),
                new Action(27,false,true,true,false),
                new Action(8,false,false,false,false),
                new Action(20,true,false,false,true),
        });

        Node.makeNodesFromActionSequences(alist, rootNode, new GameUnified());
        Node.stripUncheckedActionsExceptOnLeaves(rootNode, 7);

        List<Node> leaf = new ArrayList<>();
        rootNode.getLeaves(leaf);
        assert leaf.size() == 1;
        leaf.get(0).resetSweepAngle();

        DataSaver_StageSelected saver = new DataSaver_StageSelected();
        saver.overrideFilename = "tmp";
        saver.setSavePath("src/main/resources/saved_data/");

        // Make the value function.
        ArrayList<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(128);
        hiddenLayerSizes.add(32);
//        List<String> extraNetworkArgs = new ArrayList<>();
//        extraNetworkArgs.add("--learnrate");
//        extraNetworkArgs.add("1e-4");

//        ValueFunction_TensorFlow_ActionIn valueFunction = null;
//        try {
//            valueFunction = ValueFunction_TensorFlow_ActionIn.makeNew("tmp3",
//                    hiddenLayerSizes);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        valueFunction.setTrainingStepsPerBatch(netTrainingStepsPerIter);
//        valueFunction.setTrainingBatchSize(100);

//        ValueFunction_TensorFlow_StateOnly valueFunction = null;
//        try {
//            valueFunction = new ValueFunction_TensorFlow_StateOnly("state_only_w_roll",
//                    hiddenLayerSizes, new ArrayList<>());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        assert valueFunction != null;
//
//        valueFunction.setTrainingStepsPerBatch(netTrainingStepsPerIter);
//        valueFunction.setTrainingBatchSize(100);

//        ValueFunction_TensorFlow_ActionInMulti valueFunction = null;
//        try {
//            valueFunction = ValueFunction_TensorFlow_ActionInMulti.makeNew(
//                            Node.potentialActionGenerator.getAllPossibleActions(),
//                            "tmpMulti",
//                            hiddenLayerSizes,
//                            new ArrayList<>());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

//        valueFunction.loadCheckpoint("chk1");
        // Load a value function controller.
        ValueFunction_TensorFlow valueFunction = null;
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("chk");

        valueFunction.setTrainingStepsPerBatch(netTrainingStepsPerIter);
        valueFunction.setTrainingBatchSize(100);

        for (int k = 0; k < 1000; k++) {
//            RolloutPolicy_ValueFunction rollout  =
//                    new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueNetwork);
//            rollout.maxRolloutTimesteps = 200;

            RolloutPolicy rollout =
                    new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueFunction);

            Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Constant(0f), rollout);

            TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(getToSteadyDepth, ucbSampler, saver);
            searchMax.terminateAfterXGames = bailAfterXGames;

            // Grab some workers from the pool.
            List<TreeWorker> tws = new ArrayList<>();
            for (int i = 0; i < numWorkersToUse; i++) {
                tws.add(borrowWorker());
            }

            // Do stage search
            searchMax.initialize(tws, rootNode);

            // Return the workers.
            for (TreeWorker w : tws) {
                returnWorker(w);
            }

            // Update the value function.
            List<Node> nodesBelow = new ArrayList<>();
            rootNode.getNodesBelow(nodesBelow, true);
            nodesBelow.remove(rootNode);

            Utility.tic();
            valueFunction.update(nodesBelow);
            Utility.toc();

            for (Node n : nodesBelow) {
                n.nodeLabelAlt = String.format("%.2f", valueFunction.evaluate(n));
            }

//             Save a checkpoint of the weights/biases.
            if (k % 2 == 0) {
                valueFunction.saveCheckpoint("chk2");
                System.out.println("Saved");
            }
        }
    }
}
