package goals.tree_search;

import actions.Action;
import evaluators.EvaluationFunction_Constant;
import evaluators.EvaluationFunction_Distance;
import game.GameUnified;
import samplers.Sampler_UCB;
import samplers.rollout.*;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        ExecutorService labelUpdater = null;
        if (!headless) {
            labelUpdater = Executors.newSingleThreadExecutor();
        }


        // Make new tree root and assign to GUI.
        // Assign default available actions.
        assignAllowableActionsWider(-1);
        Node rootNode = new Node();
        Node.pointsToDraw.clear();
        ui.clearRootNodes();
        ui.addRootNode(rootNode);

        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(7,false,false,false,false),
                new Action(49, Action.Keys.wo),
                new Action(2, Action.Keys.none),
                new Action(25, Action.Keys.qp),

                new Action(34, Action.Keys.none),
//                new Action(11,false,true,true,false),
//                new Action(10,false,false,false,false),
//                new Action(21,true,false,false,true),
//
//                new Action(27,false,false,false,false),
//                new Action(21,false,true,true,false),
//                new Action(30,false,false,false,false),
//                new Action(21,true,false,false,true),
        });

        Node.makeNodesFromActionSequences(alist, rootNode, new GameUnified());
        Node.stripUncheckedActionsExceptOnLeaves(rootNode, alist.get(0).length - 1);

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
        hiddenLayerSizes.add(64);
        List<String> extraNetworkArgs = new ArrayList<>();
        extraNetworkArgs.add("--learnrate");
        extraNetworkArgs.add("1e-3");

        ValueFunction_TensorFlow_StateOnly valueFunction = null;
        try {

            valueFunction = new ValueFunction_TensorFlow_StateOnly("small_net",
                    hiddenLayerSizes, extraNetworkArgs);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }




//        ValueFunction_TensorFlow valueFunction = null;
//        try {
//            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
//                    "/state_only.pb"));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        int chkIdx = 256;
        valueFunction.loadCheckpoint("small" + chkIdx);
        valueFunction.setTrainingStepsPerBatch(netTrainingStepsPerIter);
        valueFunction.setTrainingBatchSize(1000);
        boolean valueFunctionRollout = false;

        for (int k = 0; k < 10000; k++) {
            RolloutPolicy rollout;

            if (valueFunctionRollout) {
                rollout =
                        new RolloutPolicy_WorstCaseWindow(new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueFunction));
            } else {
                //new RolloutPolicy_WorstCaseWindow(
                rollout = new RolloutPolicy_RandomDecayingHorizon();
                        //new RolloutPolicy_SingleRandom(new EvaluationFunction_Distance());
            }

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

            if (!headless) {
                ValueFunction_TensorFlow_StateOnly finalValueFunction = valueFunction;
                Runnable updateLabels = () -> {
                    for (Node n : nodesBelow) {
                        n.nodeLabelAlt = String.format("%.2f", finalValueFunction.evaluate(n));
                    }
                };
                labelUpdater.execute(updateLabels);
            }
//             Save a checkpoint of the weights/biases.
//            if (k % 2 == 0) {
                valueFunction.saveCheckpoint("small" + (k + chkIdx + 1));
                System.out.println("Saved");
//            }
        }
    }
}
