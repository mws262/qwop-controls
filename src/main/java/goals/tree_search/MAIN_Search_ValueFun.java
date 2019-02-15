package goals.tree_search;

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

        // Assign default available actions.
        assignAllowableActions(-1);

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
        layerSizes.add(73);
        layerSizes.add(128);
        layerSizes.add(50);
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
            RolloutPolicy_ValueFunction valueFunction  =
                    new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueNetwork);

            valueFunction.maxRolloutTimesteps = 200;
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
            nodesBelow.remove(rootNode);

            // Get states and actions for training step.
            float[][] trainingStateArray = new float[nodesBelow.size()][73];
//            float[][] actionDurations = new float[nodesBelow.size()][1];
            float[][] value = new float[nodesBelow.size()][1];

            for (int i = 0; i < nodesBelow.size(); i++) {
                float[] st = nodesBelow.get(i).getParent().getState().flattenState();
                for (int j = 0; j < st.length; j++) {
                    trainingStateArray[i][j] = st[j];
                }
                trainingStateArray[i][st.length] = nodesBelow.get(i).getAction().getTimestepsTotal();
                value[i][0] = nodesBelow.get(i).getValue()/nodesBelow.get(i).visitCount.floatValue();

                // The fuck is going on here.
                if (Float.isNaN(value[i][0])) {
                    System.out.println("WAIT!");
                    System.out.println(nodesBelow.get(i).getValue());
                    System.out.println(nodesBelow.get(i).visitCount.floatValue());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(nodesBelow.get(i).getValue());
                    System.out.println(nodesBelow.get(i).visitCount.floatValue());
                    value[i][0] = nodesBelow.get(i).getValue()/nodesBelow.get(i).visitCount.floatValue();

                }
//                actionDurations[i][nodesBelow.get(i).getAction().getTimestepsTotal() - actionLow] = 1; // One hot.
            }

            float loss = valueNetwork.trainingStep(trainingStateArray, value, netTrainingStepsPerIter);
            valueNetwork.saveCheckpoint("chk1");
            System.out.println("Training complete. Loss: " + loss + " Iteration: " + k);

        }
    }
}
