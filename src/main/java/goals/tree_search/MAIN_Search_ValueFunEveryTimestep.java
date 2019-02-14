package goals.tree_search;

import actions.Action;
import actions.ActionGenerator_FixedActions;
import actions.ActionSet;
import distributions.Distribution_Equal;
import evaluators.EvaluationFunction_Constant;
import evaluators.EvaluationFunction_Distance;
import samplers.Sampler_UCB;
import samplers.rollout.RolloutPolicy_ValueFunction;
import savers.DataSaver_StageSelected;
import tflowtools.TrainableNetwork;
import tree.Node;
import tree.TreeStage_MaxDepth;
import tree.TreeWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MAIN_Search_ValueFunEveryTimestep extends MAIN_Search_Template {

    public MAIN_Search_ValueFunEveryTimestep(File configFile) {
        super(configFile);
    }

    public static void main(String[] args) {
        MAIN_Search_ValueFunEveryTimestep manager = new MAIN_Search_ValueFunEveryTimestep(
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

        // Only 3 actions ever. 1 timestep nothing, 1 timestep QP, 1 timestep WO
        ActionSet allActions = new ActionSet(new Distribution_Equal());
        allActions.add(new Action(1, false, false, false, false));
        allActions.add(new Action(1, false, true, true, false));
        allActions.add(new Action(1, true, false, false, true));
        Node.potentialActionGenerator = new ActionGenerator_FixedActions(allActions);


        // Make new tree root and assign to GUI.
        Node rootNode = new Node();
        Node.pointsToDraw.clear();
        ui.clearRootNodes();
        ui.addRootNode(rootNode);

        DataSaver_StageSelected saver = new DataSaver_StageSelected();
        saver.overrideFilename = "tmp";
        saver.setSavePath("src/main/resources/saved_data/");

        // Construct or load the neural network.
        ArrayList<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(72);
        layerSizes.add(128);
//        layerSizes.add(1);
        layerSizes.add(allActions.size());
        List<String> extraNetworkArgs = new ArrayList<>();
//        extraNetworkArgs.add("--activationsout");
//        extraNetworkArgs.add("softmax");

//        TrainableNetwork valueNetwork = TrainableNetwork.makeNewNetwork(
//                "tmp", layerSizes, extraNetworkArgs);
        TrainableNetwork valueNetwork = new TrainableNetwork(new File("src/main/resources/tflow_models/tmp.pb"));
        valueNetwork.loadCheckpoint("chk1");

        for (int k = 0; k < 1000; k++) {
            RolloutPolicy_ValueFunction valueFunction  =
                    new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueNetwork);

            valueFunction.maxRolloutTimesteps = 1000;
//            Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Distance(), valueFunction);
            Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Constant(0f), valueFunction);


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

            // Remove all nodes which do not have all 3 child actions added yet.
            Iterator<Node> iter = nodesBelow.iterator();
            while(iter.hasNext()) {
                Node n = iter.next();
                if (n.getChildCount() < allActions.size()) {
                    iter.remove();
                }
            }

            // Get states and actions for training step.
            float[][] trainingStateArray = new float[nodesBelow.size()][72];
            float[][] value = new float[nodesBelow.size()][3];

            for (int i = 0; i < nodesBelow.size(); i++) {
                Node n = nodesBelow.get(i);
                float[] st = n.getState().flattenState();
                for (int j = 0; j < st.length; j++) {
                    trainingStateArray[i][j] = st[j];
                }

                // Get values for each.
                float minVal = Float.MAX_VALUE;
                for (Node cn : n.getChildren()) {
                    // Temp hax
                    while (0 == cn.visitCount.floatValue()) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (Arrays.equals(cn.getAction().peek(), allActions.get(0).peek())) {
                        value[i][0] = cn.getValue() / cn.visitCount.floatValue();
                        if (value[i][0] < minVal) minVal = value[i][0];
                    } else if (Arrays.equals(cn.getAction().peek(), allActions.get(1).peek())) {
                        value[i][1] = cn.getValue() / cn.visitCount.floatValue();
                        if (value[i][1] < minVal) minVal = value[i][1];
                    } else if (Arrays.equals(cn.getAction().peek(), allActions.get(2).peek())) {
                        value[i][2] = cn.getValue() / cn.visitCount.floatValue();
                        if (value[i][2] < minVal) minVal = value[i][2];
                    } else {
                        throw new RuntimeException("bad bad bad");
                    }
                }

//                // Offset by min to clear negatives.
//                value[i][0] -= minVal;
//                value[i][1] -= minVal;
//                value[i][2] -= minVal;
//
//                float avg = (value[i][0] + value[i][1] + value[i][2]) / 3f;
//                value[i][0] /= avg;
//                value[i][1] /= avg;
//                value[i][2] /= avg;




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
//                actionDurations[i][nodesBelow.get(i).getAction().getTimestepsTotal() - actionLow] = 1; // One hot.
            }

            float loss = valueNetwork.trainingStep(trainingStateArray, value, netTrainingStepsPerIter);
            valueNetwork.saveCheckpoint("chk1");
            System.out.println("Training complete. Loss: " + loss + " Iteration: " + k);

        }
    }
}
