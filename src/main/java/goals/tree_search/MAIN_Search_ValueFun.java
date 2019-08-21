package goals.tree_search;

import controllers.Controller_Random;
import controllers.Controller_ValueFunction;
import controllers.IController;
import game.GameUnified;
import game.GameUnifiedCaching;
import game.action.Action;
import game.action.ActionGenerator_FixedSequence;
import game.action.CommandQWOP;
import game.action.IActionGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.TreeWorker;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphics;
import tree.node.NodeQWOPGraphicsBase;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.node.evaluator.EvaluationFunction_Velocity;
import tree.node.evaluator.IEvaluationFunction;
import tree.sampler.ISampler;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.*;
import tree.stage.TreeStage_MaxDepth;
import value.ValueFunction_TensorFlow_StateOnly;
import value.updaters.ValueUpdater_Average;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MAIN_Search_ValueFun extends SearchTemplate {

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
    private RolloutControllers rolloutControllerType;
    private RolloutEvaluators rolloutEvaluatorType;

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
    private static final Logger logger = LogManager.getLogger(MAIN_Search_ValueFun.class);

    private int bailAfterXGames;
    private int getToSteadyDepth;
    private int numWorkersToUse;

    private ValueFunction_TensorFlow_StateOnly valueFunction;

    private int prevStates = 0;
    private int delayTs = 1;
//    ValueFunction_TensorFlow_StateOnly vfunCopy = null;
//    {
//        try {
//            List<Integer> layers = new ArrayList<>();
//            layers.add(128);
//            layers.add(64);
//            vfunCopy = new ValueFunction_TensorFlow_StateOnly("small_net", new GameUnified(), layers,
//                    new ArrayList<>());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        vfunCopy.loadCheckpoint("small329");
//    }
    @SuppressWarnings("ConstantConditions")
    public MAIN_Search_ValueFun(File configFile) {
        super(configFile);

        /* Load parameters from config file. */
//        Sampler_UCB.explorationMultiplier = Float.parseFloat(properties.getProperty("UCBExplorationMultiplier", "1"));
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

        rolloutType = Rollouts.valueOf(properties.getProperty("rolloutType", "DECAYING_HORIZON"));
        rolloutControllerType = RolloutControllers.valueOf(properties.getProperty("rolloutController", "RANDOM"));
        rolloutEvaluatorType = RolloutEvaluators.valueOf(properties.getProperty("rolloutEvaluator", "DISTANCE"));

        rolloutHorizonTimesteps = Integer.parseInt(properties.getProperty("rolloutHorizonTimesteps", "200"));
        windowRollout = Boolean.parseBoolean(properties.getProperty("windowRollout", "false"));
        windowSelectionType = RolloutPolicy_Window.Criteria.valueOf(properties.getProperty("windowSelectionType", "AVERAGE"));


        // TODO fix these that were disabled temporarily.
        rolloutWeightedWithValFun = Boolean.parseBoolean(properties.getProperty("rolloutWeightedWithValFun", "false"));
        rolloutValFunWeight = Float.parseFloat(properties.getProperty("rolloutValFunWeight", "0.75"));

        game = (prevStates > 0 && delayTs > 0) ? new GameUnifiedCaching(delayTs, prevStates, GameUnifiedCaching.StateType.HIGHER_DIFFERENCES) :
                new GameUnified();
        makeValueFunction(game);
    }

    @Override
    TreeWorker getTreeWorker() {

        /* Pick rollout configuration. */
        IRolloutPolicy rollout;
        IController rolloutController;
        IEvaluationFunction rolloutEvaluator;

        switch(rolloutControllerType) {
            case RANDOM:
                rolloutController = new Controller_Random();
                break;
            case VALUE_FUNCTION:
//                ValueFunction_TensorFlow_StateOnly hmm = vfunCopy.getCopy();
//                hmm.loadCheckpoint("small329");
                rolloutController = new Controller_ValueFunction(valueFunction.getCopy()); // NOTE: this copy is
                // independent. I don't know if that's good or bad.
                break;
            default:
                throw new IllegalArgumentException("Unknown rollout controller type specified: " + rolloutControllerType.name());
        }

        switch(rolloutEvaluatorType) {
            case DISTANCE:
                rolloutEvaluator = new EvaluationFunction_Distance();
                break;
            case VELOCITY:
                rolloutEvaluator = new EvaluationFunction_Velocity();
                break;
            default:
                throw new IllegalArgumentException("Unknown rollout evaluator type specified: " + rolloutEvaluatorType.name());
        }

        // BASIC ROLLOUT STRATEGY
        switch (rolloutType) {
            case END_SCORE:
                rollout = new RolloutPolicy_EndScore(rolloutEvaluator, rolloutController);
                break;
            case DELTA_SCORE:
                rollout = new RolloutPolicy_DeltaScore(rolloutEvaluator, rolloutController);
                break;
            case DECAYING_HORIZON:
                rollout = new RolloutPolicy_DecayingHorizon(rolloutEvaluator, rolloutController, rolloutHorizonTimesteps);
                break;
            default:
                throw new IllegalArgumentException("Unknown rollout type specified: " + rolloutType.name());
        }

        // ROLLOUT ACTIONS ABOVE AND BELOW ALSO.
        if (windowRollout) {
            RolloutPolicy_Window windowRollout = new RolloutPolicy_Window(rollout);
            windowRollout.selectionCriteria = windowSelectionType;
            rollout = windowRollout;
        }

        ISampler sampler = new Sampler_UCB(new EvaluationFunction_Constant(0f), rollout, new ValueUpdater_Average(), 5
                , 1); // TODO
        // hardcoded.

        return (prevStates > 0 && delayTs > 0) ? TreeWorker.makeCachedStateTreeWorker(sampler, delayTs, prevStates,
                GameUnifiedCaching.StateType.HIGHER_DIFFERENCES) :
                TreeWorker.makeStandardTreeWorker(sampler);
    }

    public static void main(String[] args) {
        MAIN_Search_ValueFun manager = new MAIN_Search_ValueFun(
                new File(configFilePath + configFileName));
        manager.doGames();
    }

    enum Rollouts {
        END_SCORE, DELTA_SCORE, DECAYING_HORIZON
    }

    enum RolloutControllers {
        RANDOM, VALUE_FUNCTION
    }

    enum RolloutEvaluators {
        DISTANCE, VELOCITY
    }

    @SuppressWarnings("Duplicates")
    public void doGames() {
        logger.info("Rollout policy chosen: " + rolloutType.name() + " Rollout controller: " + rolloutControllerType.name() + " Rollout evaluator: " + rolloutEvaluatorType.name() +
                " Weighted with value function? " + rolloutWeightedWithValFun + ". As a window of 3? " + windowRollout + ".");

        // Make new tree root and assign to GUI.
        // Assign default available game.action.
        IActionGenerator actionGenerator = ActionGenerator_FixedSequence.makeExtendedGenerator(-1);// new ActionGenerator_UniformNoRepeats();//

        List<Action[]> alist = new ArrayList<>();
        alist.add(new Action[]{
                new Action(7, CommandQWOP.NONE),
//                new Action(49, Action.Keys.wo),
//                new Action(2, Action.Keys.none),
//                new Action(46, Action.Keys.qp),
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

            doSearchAndUpdate(rootNode, k);

            // Update node labels if graphics are enabled.
            if (!headless) {
                NodeQWOPGraphics graphicsRootNode = (NodeQWOPGraphics) rootNode;

                Runnable updateLabels = () -> graphicsRootNode.recurseDownTreeExclusive(n -> {
                    float percDiff = valueFunction.evaluate(n); // Temp disable percent diff for absolute diff.
//                    float percDiff = Math.abs((valueFunction.evaluateActionDistribution(n) - n.getValue())/n.getValue() * 100f);
                    n.nodeLabel = String.format("%.1f, %.1f", n.getValue(), percDiff);
                    n.setLabelColor(NodeQWOPGraphicsBase.getColorFromScaledValue(-Math.min(Math.abs(percDiff - n.getValue()), 20) + 20
                            , 20,
                            0.9f));
                    n.displayLabel = true;
                });
                labelUpdater.execute(updateLabels);
            }
        }
    }

    private void doSearchAndUpdate(NodeQWOPExplorableBase<?> rootNode, int updateIdx) {
        TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(getToSteadyDepth, bailAfterXGames);

        // Grab some workers from the pool.
        List<TreeWorker> tws = getTreeWorkers(numWorkersToUse);

        // Do stage search
        searchMax.initialize(tws, rootNode);

        // Return the workers.
        tws.forEach(this::removeWorker);

        // Update the value function.
        List<NodeQWOPExplorableBase<?>> nodesBelow = new ArrayList<>();
        rootNode.recurseDownTreeExclusive(n -> {
                    //if (n.getChildCount() > 0) { // TODO TEMP EXCLUDE LEAVES
                        nodesBelow.add(n);
                    //}
                });

        Collections.shuffle(nodesBelow);
        valueFunction.update(nodesBelow);

        // Save a checkpoint of the weights/biases.
        try {
            valueFunction.saveCheckpoint("src/main/resources/tflow_models/checkpoints/" + checkpointNamePrefix + (updateIdx + checkpointIndex + 1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Saved checkpoint as: " + checkpointNamePrefix + (updateIdx + checkpointIndex + 1));
    }


    private void makeValueFunction(GameUnified gameTemplate) {
        /* Make the value function net. */
        List<String> extraNetworkArgs = new ArrayList<>();
        extraNetworkArgs.add("--learnrate");
        extraNetworkArgs.add(learningRate);

        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(networkName, gameTemplate, hiddenLayerSizes,
                    extraNetworkArgs, "", true);
        } catch (IOException e) {
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
            try {
                valueFunction.loadCheckpoint(checkpointNamePrefix + checkpointIndex);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            logger.info("Specified checkpoint index: " + checkpointIndex + ". Not loading.");
        }
        valueFunction.setTrainingBatchSize(trainingBatchSize);
        logger.info("Training batch size set to: " + trainingBatchSize);
    }
}
