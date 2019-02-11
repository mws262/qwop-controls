package goals.tree_search;

import actions.Action;
import actions.ActionGenerator_FixedSequence;
import actions.ActionSet;
import distributions.Distribution;
import distributions.Distribution_Normal;
import evaluators.EvaluationFunction_Distance;
import samplers.Sampler_UCB;
import samplers.rollout.RolloutPolicy_ValueFunction;
import savers.DataSaver_StageSelected;
import tflowtools.TrainableNetwork;
import tree.Node;
import tree.TreeStage_MaxDepth;
import tree.TreeWorker;
import tree.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

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

//        // For now, going to use the same set of timings for all actions.
//        int actionLow = 5;
//        int actionHigh = 60;
//        int totalActionCount = actionHigh - actionLow;
//
//        /* Repeated action 1 -- no keys pressed. */
//        Distribution<Action> dist1 = new Distribution_Normal(10f, 2f);
//        ActionSet actionSet1 = ActionSet.makeActionSet(IntStream.range(actionLow, actionHigh).toArray(),
//                new boolean[]{false, false, false, false}, dist1);
//
//        /*  Repeated action 2 -- W-O pressed */
//        Distribution<Action> dist2 = new Distribution_Normal(39f, 3f);
//        ActionSet actionSet2 = ActionSet.makeActionSet(IntStream.range(actionLow, actionHigh).toArray(),
//                new boolean[]{false, true, true, false}, dist2);
//
//        /* Repeated action 3 -- W-O pressed */
//        Distribution<Action> dist3 = new Distribution_Normal(10f, 2f);
//        ActionSet actionSet3 = ActionSet.makeActionSet(IntStream.range(actionLow, actionHigh).toArray(),
//                new boolean[]{false, false, false, false}, dist3);
//
//        /*  Repeated action 4 -- Q-P pressed */
//        Distribution<Action> dist4 = new Distribution_Normal(39f, 3f);
//        ActionSet actionSet4 = ActionSet.makeActionSet(IntStream.range(actionLow, actionHigh).toArray(),
//                new boolean[]{true, false, false, true}, dist4);
//
//        ActionSet[] actionSets = new ActionSet[]{actionSet1, actionSet2, actionSet3, actionSet4};
//        Node.potentialActionGenerator = new ActionGenerator_FixedSequence(actionSets, new HashMap<>());

        assignAllowableActions(-1);

        Node rootNode = new Node();
        Node.pointsToDraw.clear();
        ui.clearRootNodes();
        ui.addRootNode(rootNode);

        int desiredDepth = 10000;

        DataSaver_StageSelected saver = new DataSaver_StageSelected();
        saver.overrideFilename = "tmp";
        saver.setSavePath("src/main/resources/saved_data/");


        ArrayList<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(73);
        layerSizes.add(128);
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
            Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Distance(),
                    new RolloutPolicy_ValueFunction(new EvaluationFunction_Distance(), valueNetwork));
            ucbSampler.c = 0.1f;
            TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(desiredDepth, ucbSampler, saver);
            searchMax.terminateAfterXGames = 1000;
            // Grab some workers from the pool.
            List<TreeWorker> tws1 = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tws1.add(borrowWorker());
            }

            // Do stage search
            searchMax.initialize(tws1, rootNode);

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
                value[i][0] = (float) (nodesBelow.get(i).getValue()/nodesBelow.get(i).visitCount.doubleValue());

//                actionDurations[i][nodesBelow.get(i).getAction().getTimestepsTotal() - actionLow] = 1; // One hot.
            }

            valueNetwork.trainingStep(trainingStateArray, value, 20);
            valueNetwork.saveCheckpoint("chk1");
            System.out.println("Training complete. " + k);
            for (TreeWorker w : tws1) {
                returnWorker(w);
            }
        }



    }
}
