package goals.tree_search;

import actions.Action;
import evaluators.EvaluationFunction_Constant;
import evaluators.EvaluationFunction_DeltaDistance;
import evaluators.EvaluationFunction_Distance;
import game.GameThreadSafe;
import samplers.Sampler_UCB;
import samplers.rollout.RolloutPolicy_RandomColdStart;
import samplers.rollout.RolloutPolicy_ValueFunction;
import savers.DataSaver_StageSelected;
import tflowtools.TrainableNetwork;
import tree.Node;
import tree.TreeStage_MaxDepth;
import tree.TreeWorker;
import tree.Utility;

import java.io.File;
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
        Sampler_UCB.explorationMultiplier = Float.valueOf(properties.getProperty("UCBExplorationMultiplier", "1"));
        float maxWorkerFraction = Float.valueOf(properties.getProperty("fractionOfMaxWorkers", "1"));
        int bailAfterXGames = Integer.valueOf(properties.getProperty("bailAfterXGames", "100000"));
        int getToSteadyDepth = Integer.valueOf(properties.getProperty("getToSteadyDepth", "100"));
        int netTrainingStepsPerIter = Integer.valueOf(properties.getProperty("netTrainingStepsPerIter", "20"));


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

        Node.makeNodesFromActionSequences(alist, rootNode, new GameThreadSafe());
        Node.stripUncheckedActionsExceptOnLeaves(rootNode, 7);

        List<Node> leaf = new ArrayList<>();
        rootNode.getLeaves(leaf);
        assert leaf.size() == 1;
        leaf.get(0).resetSweepAngle();




        DataSaver_StageSelected saver = new DataSaver_StageSelected();
        saver.overrideFilename = "tmp";
        saver.setSavePath("src/main/resources/saved_data/");

        // Construct or load the neural network.
        ArrayList<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(73);
        layerSizes.add(128);
        layerSizes.add(64);
        layerSizes.add(32);
//        layerSizes.add();
        layerSizes.add(1);
//        layerSizes.add(totalActionCount);
//        List<String> extraNetworkArgs = new ArrayList<>();
//        extraNetworkArgs.add("--activationsout");
//        extraNetworkArgs.add("softmax");

//        TrainableNetwork valueNetwork = TrainableNetwork.makeNewNetwork(
//                "tmp", layerSizes);
        TrainableNetwork valueNetwork = new TrainableNetwork(new File("src/main/resources/tflow_models/tmp.pb"));
        valueNetwork.loadCheckpoint("chk1");

        for (int k = 0; k < 1000; k++) {
//            RolloutPolicy_ValueFunction rollout  =
//                    new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueNetwork);
//            rollout.maxRolloutTimesteps = 500;

            RolloutPolicy_RandomColdStart rollout = new RolloutPolicy_RandomColdStart(new EvaluationFunction_DeltaDistance());

            // Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Distance(), valueFunction);
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

            for (TreeWorker w : tws) {
                returnWorker(w);
            }

            List<Node> nodesBelow = new ArrayList<>();
            rootNode.getNodesBelow(nodesBelow, true);
            nodesBelow.remove(rootNode);

//            // Get states and actions for training step.
//            float[][] trainingStateArray = new float[nodesBelow.size()][73];
////            float[][] actionDurations = new float[nodesBelow.size()][1];
//            float[][] value = new float[nodesBelow.size()][1];
//
//            for (int i = 0; i < nodesBelow.size(); i++) {
//                float[] st = nodesBelow.get(i).getParent().getState().flattenState();
//                for (int j = 0; j < st.length; j++) {
//                    trainingStateArray[i][j] = st[j];
//                }
//                trainingStateArray[i][st.length] = nodesBelow.get(i).getAction().getTimestepsTotal();
//                value[i][0] = nodesBelow.get(i).getValue()/nodesBelow.get(i).visitCount.floatValue();
//
//                // The fuck is going on here.
//                if (Float.isNaN(value[i][0])) {
//                    System.out.println("WAIT!");
//                    System.out.println(nodesBelow.get(i).getValue());
//                    System.out.println(nodesBelow.get(i).visitCount.floatValue());
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    System.out.println(nodesBelow.get(i).getValue());
//                    System.out.println(nodesBelow.get(i).visitCount.floatValue());
//                    value[i][0] = nodesBelow.get(i).getValue()/nodesBelow.get(i).visitCount.floatValue();
//
//                }
////                actionDurations[i][nodesBelow.get(i).getAction().getTimestepsTotal() - actionLow] = 1; // One hot.
//            }
//
//            float loss = valueNetwork.trainingStep(trainingStateArray, value, netTrainingStepsPerIter);

            // Do as minibatches instead.


            int batchSize = 100;
            int numBatches = nodesBelow.size()/batchSize;
            float[][] trainingStateArray = new float[batchSize][73];
            float[][] value = new float[batchSize][1];

            int currentNodeIdx = 0;
            for (int i = 0; i < numBatches; i++) {
                List<Node> batchNodes = new ArrayList<>();
                for (int j = 0; j < batchSize; j++) {
                    Node currNode = nodesBelow.get(currentNodeIdx);
                    batchNodes.add(currNode);
                    float[] st = currNode.getParent().getState().flattenState();
                    for (int m = 0; m < st.length; m++) {
                        trainingStateArray[j][m] = st[m];
                    }
                    trainingStateArray[j][st.length] = currNode.getAction().getTimestepsTotal();
                    value[j][0] = currNode.getValue()/currNode.visitCount.floatValue();


                    if (Float.isNaN(value[j][0])) {
                        System.out.println("WAIT!");
                        System.out.println(currNode.getValue());
                        System.out.println(currNode.visitCount.floatValue());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println(currNode.getValue());
                        System.out.println(currNode.visitCount.floatValue());
                        value[j][0] = currNode.getValue()/currNode.visitCount.floatValue();
                    }
                    currentNodeIdx++;
                }
                float loss = valueNetwork.trainingStep(trainingStateArray, value, netTrainingStepsPerIter);
                float[][] result = valueNetwork.evaluateInput(trainingStateArray);

                for (int n = 0; n < result.length; n++) {
                    batchNodes.get(n).nodeLabelAlt = String.format("%.2f", result[n][0]);
                }
                System.out.println("Loss: " + loss + " Epoch: " + k + ", Batch: " + i);
            }

            System.out.println("Saved");
            valueNetwork.saveCheckpoint("chk1");


        }
    }
}
