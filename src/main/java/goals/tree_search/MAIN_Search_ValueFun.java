package goals.tree_search;

import actions.Action;
import actions.IActionGenerator;
import evaluators.EvaluationFunction_Constant;
import evaluators.EvaluationFunction_Distance;
import game.GameUnified;
import game.GameUnifiedCaching;
import game.IGameInternal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import samplers.Sampler_UCB;
import samplers.rollout.*;
import savers.DataSaver_Null;
import tree.*;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MAIN_Search_ValueFun extends MAIN_Search_Template {

    GameUnified game;

    /**
     * Search configuration parameter file name. Do not need to include the path. TODO move more other parameters to
     * config file.
     */
    private static final String configFileName = "search.config_value";

    /**
     * Name of the neural network to use/create. Do not add the .pb file extension.
     */
    private final String networkName;

    /**
     * Network checkpoint file prefix. Each save will create 2 files which will look like [prefix][number].*
     */
    private final String checkpointNamePrefix;

    /**
     * Starting checkpoint index. If greater than zero, it will attempt to load. If == 0, will just start afresh.
     */
    private final int checkpointIndex;

    /**
     * Network training learning rate. Set in config file. Default 1e-3.
     */
    private final String learningRate; // String because it goes to command line argument.

    /**
     *
     */
    private final int trainingBatchSize;

    private Rollouts rolloutType; // In properties file.

    private int rolloutHorizonTimesteps; // In properties file.

    private boolean windowRollout; // In properties file.

    RolloutPolicy_Window.Criteria windowSelectionType;

    private boolean rolloutWeightedWithValFun;

    private float rolloutValFunWeight;


    /**
     * Network hidden layer sizes. These should not include input and output layers.
     */
    ArrayList<Integer> hiddenLayerSizes = new ArrayList<>();

    /**
     * Handle logging messages.
     */
    private Logger logger;

    private int bailAfterXGames;
    private int getToSteadyDepth;
    private int numWorkersToUse;

    private ValueFunction_TensorFlow_StateOnly valueFunction;

    private int prevStates = 2;
    private int delayTs = 4;

    @SuppressWarnings("ConstantConditions")
    public MAIN_Search_ValueFun(File configFile) {
        super(configFile);
        logger = LogManager.getLogger(MAIN_Search_ValueFun.class);

        /* Load parameters from config file. */
        Sampler_UCB.explorationMultiplier = Float.parseFloat(properties.getProperty("UCBExplorationMultiplier", "1"));
        bailAfterXGames = Integer.parseInt(properties.getProperty("bailAfterXGames", "100000"));
        getToSteadyDepth = Integer.parseInt(properties.getProperty("getToSteadyDepth", "100"));

        float maxWorkerFraction = Float.parseFloat(properties.getProperty("fractionOfMaxWorkers", "1"));
        numWorkersToUse = (int) Math.max(1, maxWorkerFraction * maxWorkers);

        networkName = properties.getProperty("networkName");
        checkpointNamePrefix = properties.getProperty("checkpointNamePrefix", "default_name");
        checkpointIndex = Integer.parseInt(properties.getProperty("checkpointIndex", "1000"));

        learningRate = properties.getProperty("learningRate", "1e-3");
        trainingBatchSize = Integer.parseInt(properties.getProperty("trainingBatchSize", "1000"));

        String[] hiddenLayerSizesString = properties.getProperty("hiddenLayerSizes", "128,64").split(",");
        for (String layerSize : hiddenLayerSizesString) {
            hiddenLayerSizes.add(Integer.parseInt(layerSize));
        }

        if (checkpointIndex < 0)
            logger.warn("Network checkpoint index was: " + checkpointIndex + ". These will not automatically load " +
                    "when reused here.");
        if (networkName.isEmpty())
            logger.error("Network name should not be empty.", new StringIndexOutOfBoundsException());

        if (checkpointNamePrefix.isEmpty()) {
            logger.warn("No checkpoint name prefix given. This will result in saves that only have numbers for names.");
        }

        rolloutType = Rollouts.valueOf(properties.getProperty("rolloutType", "RANDOM_HORIZON"));
        rolloutHorizonTimesteps = Integer.parseInt(properties.getProperty("rolloutHorizonTimesteps", "100"));
        windowRollout = Boolean.parseBoolean(properties.getProperty("windowRollout", "false"));
        windowSelectionType = RolloutPolicy_Window.Criteria.valueOf(properties.getProperty("windowSelectionType", "AVERAGE"));
        rolloutWeightedWithValFun = Boolean.parseBoolean(properties.getProperty("rolloutWeightedWithValFun", "false"));
        rolloutValFunWeight = Float.parseFloat(properties.getProperty("rolloutValFunWeight", "0.75"));

        game = (prevStates > 0) ? new GameUnifiedCaching(delayTs, prevStates) : new GameUnified();
        makeValueFunction(game);
    }

    @Override
    TreeWorker getTreeWorker() {
        return TreeWorker.makeCachedStateTreeWorker(delayTs, prevStates);
    }

    public static void main(String[] args) {
        MAIN_Search_ValueFun manager = new MAIN_Search_ValueFun(
                new File(configFilePath + configFileName));
        manager.doGames();
    }

    enum Rollouts {
        RANDOM, RANDOM_HORIZON, VALUE_HORIZON
    }


    @SuppressWarnings("Duplicates")
    public void doGames() {

        /* Pick rollout configuration. */
        RolloutPolicy rollout;

        // BASIC ROLLOUT STRATEGY
        switch (rolloutType) {
            case RANDOM:
                // Rollout goes randomly among a limited set of actions until failure. Score based on distance
                // travelled from start to end of rollout.
                rollout = new RolloutPolicy_SingleRandom(new EvaluationFunction_Distance());
                break;
            case RANDOM_HORIZON:
                // Rollout goes randomly to a fixed horizon in the future. Future values are weighted less than
                // nearer ones. Based on distances travelled.
                RolloutPolicy_DecayingHorizon decayingHorizonRandom = new RolloutPolicy_RandomDecayingHorizon();
                decayingHorizonRandom.maxTimestepsToSim = rolloutHorizonTimesteps;
                rollout = decayingHorizonRandom;
                break;
            case VALUE_HORIZON:
                // Rollout follows value function controller to a fixed horizon in the future. Future values are
                // weighted less than nearer ones. Based on distance travelled.
                RolloutPolicy_ValueFunctionDecayingHorizon decayingHorizonValue =
                        new RolloutPolicy_ValueFunctionDecayingHorizon(valueFunction);
                decayingHorizonValue.maxTimestepsToSim = rolloutHorizonTimesteps;
                rollout = decayingHorizonValue;
                break;
            default:
                rollout = new RolloutPolicy_SingleRandom(new EvaluationFunction_Distance());
                break;
        }

        // ALSO WEIGHT ROLLOUT RESULTS WITH VALUE FUNCTION?
        if (rolloutWeightedWithValFun) {
            RolloutPolicy_WeightWithValueFunction weightedRollout = new RolloutPolicy_WeightWithValueFunction(rollout
                    , valueFunction); // Does not have to be the same value function as the one used in value
            // function POLICY rollouts.
            weightedRollout.valueFunctionWeight = rolloutValFunWeight;
            rollout = weightedRollout;
        }

        // ROLLOUT ACTIONS ABOVE AND BELOW ALSO.
        if (windowRollout) {
            rollout = new RolloutPolicy_Window(rollout);
        }

        logger.info("Rollout policy chosen: " + rolloutType.name() + ". Weighted with value function? " + rolloutWeightedWithValFun + ". As " +
                "a window of 3? " + windowRollout + ".");


        // Make new tree root and assign to GUI.
        // Assign default available actions.
        IActionGenerator actionGenerator = getExtendedActionGenerator(-1);// new ActionGenerator_Uniform();//

        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(7,false,false,false,false),
                new Action(49, Action.Keys.wo),
                new Action(2, Action.Keys.none),
                new Action(46, Action.Keys.qp),
        });


        NodeQWOPExplorableBase<?> rootNode = headless ?
                new NodeQWOPExplorable(GameUnified.getInitialState(), actionGenerator) :
                new NodeQWOPGraphics(GameUnified.getInitialState(), actionGenerator);

        NodeQWOPExplorable.makeNodesFromActionSequences(alist, rootNode, game);
        NodeQWOPExplorable.stripUncheckedActionsExceptOnLeaves(rootNode, alist.get(0).length - 1);


        ExecutorService labelUpdater = null;
        if (!headless) {
            labelUpdater = Executors.newSingleThreadExecutor();

            NodeQWOPGraphics graphicsRootNode = (NodeQWOPGraphics) rootNode;
            NodeQWOPGraphics.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(graphicsRootNode);
            List<NodeQWOPGraphics> leaf = new ArrayList<>();
            graphicsRootNode.getLeaves(leaf);
            assert leaf.size() == 1;
            leaf.get(0).resetSweepAngle();
        }

        /* Training and exploration loop. */

        for (int k = 0; k < 10000; k++) {

            doSearchAndUpdate(rootNode, rollout, k);

            // Update node labels if graphics are enabled.
            if (!headless) {
                NodeQWOPGraphics graphicsRootNode = (NodeQWOPGraphics) rootNode;

                Runnable updateLabels = () -> graphicsRootNode.recurseDownTreeExclusive(n -> {
                    float percDiff = Math.abs((valueFunction.evaluate(n) - n.getValue())/n.getValue() * 100f);
                    n.nodeLabel = String.format("%.1f, %.0f%%", n.getValue(), percDiff);
                    n.setLabelColor(NodeQWOPGraphicsBase.getColorFromScaledValue(-Math.min(percDiff, 100f) + 100
                            , 100,
                            0.9f));
                    n.displayLabel = true;
                });
                labelUpdater.execute(updateLabels);
            }
        }
    }

    private void doSearchAndUpdate(NodeQWOPExplorableBase<?> rootNode, RolloutPolicy rollout, int updateIdx) {
        Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Constant(0f), rollout.getCopy());
        TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(getToSteadyDepth, ucbSampler, new DataSaver_Null());
        searchMax.terminateAfterXGames = bailAfterXGames;

        // Grab some workers from the pool.
        List<TreeWorker> tws = getTreeWorkers(numWorkersToUse);

        // Do stage search
        searchMax.initialize(tws, rootNode);

        // Return the workers.
        tws.forEach(this::removeWorker);

        // Update the value function.
        List<NodeQWOPExplorableBase<?>> nodesBelow = new ArrayList<>();
        rootNode.recurseDownTreeExclusive(nodesBelow::add);
        Collections.shuffle(nodesBelow);
        valueFunction.update(nodesBelow);

        // Save a checkpoint of the weights/biases.
        valueFunction.saveCheckpoint(checkpointNamePrefix + (updateIdx + checkpointIndex + 1));
        logger.info("Saved checkpoint as: " + checkpointNamePrefix + (updateIdx + checkpointIndex + 1));
    }


    private void makeValueFunction(GameUnified gameTemplate) {
        /* Make the value function net. */
        List<String> extraNetworkArgs = new ArrayList<>();
        extraNetworkArgs.add("--learnrate");
        extraNetworkArgs.add(learningRate);

        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(networkName, gameTemplate, hiddenLayerSizes,
                    extraNetworkArgs);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder hiddenLayerString = new StringBuilder();
        for (Integer i : hiddenLayerSizes) {
            hiddenLayerString.append(i).append(", ");
        }
        logger.info("Using value function with learning rate: " + learningRate + " and hidden layer sizes " + hiddenLayerString.toString());

        // Load checkpoint.
        if (checkpointIndex > 0) {
            logger.info("Specified checkpoint name: " + (checkpointNamePrefix + checkpointIndex) + ". Loading.");
            valueFunction.loadCheckpoint(checkpointNamePrefix + checkpointIndex);
        } else {
            logger.info("Specified checkpoint index: " + checkpointIndex + ". Not loading.");
        }
        valueFunction.setTrainingBatchSize(trainingBatchSize);
        logger.info("Training batch size set to: " + trainingBatchSize);
    }
}
