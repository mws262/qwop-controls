package goals.tree_search;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

import samplers.Sampler_Greedy;
import savers.DataSaver_DenseTFRecord;
import tree.*;
import actions.Action;
import actions.ActionGenerator_FixedSequence;
import actions.ActionSet;
import distributions.Distribution;
import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;

import distributions.Distribution_Normal;
import evaluators.EvaluationFunction_Distance;
import filters.NodeFilter_SurvivalHorizon;
import samplers.Sampler_FixedDepth;
import samplers.Sampler_UCB;
import savers.DataSaver_StageSelected;
import transformations.Transform_Autoencoder;
import transformations.Transform_PCA;
import ui.*;

public abstract class MAIN_Search_Template {

    /**
     * Settings loaded from the config file. Should AT LEAST contain:
     * boolean headless
     * string saveLocation
     * workersFractionOfCores
     */
    protected Properties properties;

    /**
     * Whether or not to run without the UI.
     */
    protected final boolean headless;

    protected final IUserInterface ui;

    /**
     * Where should data be saved?
     */
    private File saveLoc;

    /**
     * Information put in a running log to be saved at shutdown.
     */
    protected String endLog = "";

    /**
     * Pool of {@link TreeWorker} to be shared for building the tree.
     */
    private final GenericObjectPool<TreeWorker> workerPool;

    /**
     * Keep a list of the checked out workers so they can be added to the monitor panel if it exists.
     */
    private List<TreeWorker> activeWorkers = new ArrayList<>();

    /**
     * A tabbed panel for displaying how many games per second each worker is running.
     */
    private PanelTimeSeries_WorkerLoad workerMonitorPanel;

    /**
     * Maximum number of workers any stage can recruit.
     */
    final int maxWorkers;

    public MAIN_Search_Template(File configFile) {
        // Load the configuration file.
        properties = Utility.loadConfigFile(configFile);

        String logPrefix = "MAIN: ";

        // Create the data save directory.
        saveLoc = new File("src/main/resources/saved_data/" + properties.getProperty("saveLocation", "./"));
        if (!saveLoc.exists()) {
            boolean success = saveLoc.mkdirs();
            if (!success) throw new RuntimeException("Could not make save directory.");
        }
        // WORKER CORES:
        // Worker threads to run. Each worker independently explores the tree and has its own loaded copy of the
        // Box2D libraries.
        float workersFractionOfCores = Float.parseFloat(properties.getProperty("workersFractionOfCores", "0.8"));
        int cores = Runtime.getRuntime().availableProcessors();
        maxWorkers = (int) (workersFractionOfCores * cores); // Basing of number of cores including hyperthreading.
        // May want to optimize this a tad.
        System.out.println("Detected " + cores + " physical cores. Making a max of " + maxWorkers + " workers.");

        workerPool = new GenericObjectPool<>(new WorkerFactory());
        workerPool.setMaxTotal(maxWorkers);
        workerPool.setMaxIdle(-1); // No limit to idle. Would have defaulted to 8, meaning all others would get
        // culled between stages.

        // UI CONFIG:
        headless = Boolean.valueOf(properties.getProperty("headless", "false")); // Default to using fullUI
        appendSummaryLog(logPrefix + "Full UI is " + (headless ? "not" : "") + "on.");
        ui = (headless) ? new UI_Headless() : setupFullUI(); // Make the UI.

        Thread uiThread = new Thread(ui);
        uiThread.start();

        // Copy the config file into the save directory.
        File configSave = new File(saveLoc.toString() + "/config_" + Utility.getTimestamp() + ".config");
        try {
            FileUtils.copyFile(configFile, configSave);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // Write machine details to log.
        appendSummaryLog(logPrefix + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        String hostname = "Unknown";
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Hostname can not be resolved");
        }

        appendSummaryLog(logPrefix + "Name: " + hostname);
        appendSummaryLog(logPrefix + "Save directory: " + saveLoc.getAbsolutePath());


        // Save a progress log before shutting down.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            appendSummaryLog(logPrefix + "Finalizing log.");
            try {
                Utility.stringToLogFile(endLog, saveLoc.toString() + "/" + "summary_" + Utility.getTimestamp() +
						".log");
            } catch (IOException e) {
                e.printStackTrace();
            }
            workerPool.close();
        }));
    }

    /**
     * Borrow a {@link TreeWorker} from the pool. Be sure to return it later!
     */
    TreeWorker borrowWorker() {
        TreeWorker worker = null;
        try {
            worker = workerPool.borrowObject();
            activeWorkers.add(worker);
            if (workerMonitorPanel != null) workerMonitorPanel.setWorkers(activeWorkers);

        } catch (Exception e) {
            System.out.println("Failed to borrow a worker.");
            e.printStackTrace();
        }
        return worker;
    }

    /**
     * Give the worker back to the pool to be reused later.
     */
    void returnWorker(TreeWorker finishedWorker) {
        workerPool.returnObject(finishedWorker);
        activeWorkers.remove(finishedWorker);
        if (workerMonitorPanel != null) workerMonitorPanel.setWorkers(activeWorkers);
    }

    /**
     *  Setup and perform a {@link TreeStage_MaxDepth} search. Tries to get at least one branch to the specified
     *  depth. Uses {@link Sampler_UCB} with {@link EvaluationFunction_Distance}.
     *
     * @param rootNode Tree root node.
     * @param saveName Name of the file to save stage data to. This is sparse.
     * @param desiredDepth Depth to get a branch to.
     * @param fractionOfWorkers 0 to 1, proportion of workers to allot to this stage.
     * @param maxGames Maximum number of games to play before giving up.
     */
    protected void doBasicMaxDepthStage(Node rootNode, String saveName, int desiredDepth, float fractionOfWorkers,
                                        int maxGames) {
        if (fractionOfWorkers > 1)
            throw new RuntimeException("Cannot request more than 100% (i.e. fraction of 1) workers available.");

        String logPrefix = "BasicMaxDepthSearch: ";

        long startTime = System.currentTimeMillis();
        int numWorkersToUse = (int) Math.max(1, fractionOfWorkers * maxWorkers);
        writeLogStageHeader(logPrefix, saveName, rootNode.getTreeDepth(), numWorkersToUse, maxGames);

        DataSaver_StageSelected saver = new DataSaver_StageSelected();
        saver.overrideFilename = saveName;
        saver.setSavePath(saveLoc.getPath() + "/");

        Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Distance());
        TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(desiredDepth, ucbSampler, saver); // Depth to get to
		// sorta steady state. was
        searchMax.terminateAfterXGames = maxGames; // Will terminate after this many games played regardless of
		// whether goals have been met.

        // Grab some workers from the pool.
        List<TreeWorker> tws1 = new ArrayList<>();
        for (int i = 0; i < numWorkersToUse; i++) {
            tws1.add(borrowWorker());
        }

        // Do stage search
        searchMax.initialize(tws1, rootNode);

        float elapsedSeconds = Math.floorDiv(System.currentTimeMillis() - startTime, 100) / 10f; // To one decimal
		// place.
        appendSummaryLog(logPrefix + "Finished after " + elapsedSeconds + " seconds.");
        appendSummaryLog(logPrefix + "Results -- " + (searchMax.getResults().isEmpty() ? "<goal not met>" :
				searchMax.getResults().get(0).getTreeDepth() + " depth achieved."));
        // Return the checked out workers.
        for (TreeWorker w : tws1) {
            returnWorker(w);
        }
    }

    /**
     * Setup and perform a {@link TreeStage_MinDepth} search. Tries to get ALL branches to a minimum depth (if
     * possible). Uses {@link Sampler_FixedDepth}
     *
     * @param rootNode Tree root node.
     * @param saveName Name of the file to save stage data to. This is sparse.
     * @param minDepth Depth to get all branches to, if failure doesn't happen first.
     * @param fractionOfWorkers 0 to 1, proportion of workers to allot to this stage.
     * @param maxGames Maximum number of games to play before giving up.
     */
    protected void doBasicMinDepthStage(Node rootNode, String saveName, int minDepth, float fractionOfWorkers,
										int maxGames) {
        if (fractionOfWorkers > 1)
            throw new RuntimeException("Cannot request more than 100% (i.e. fraction of 1) workers available.");

        String logPrefix = "BasicMinDepthSearch: ";

        long startTime = System.currentTimeMillis();
        int numWorkersToUse = (int) Math.max(1, fractionOfWorkers * maxWorkers);
        writeLogStageHeader(logPrefix, saveName, rootNode.getTreeDepth(), numWorkersToUse, maxGames);

        DataSaver_StageSelected saver = new DataSaver_StageSelected();

        saver.overrideFilename = saveName;
        saver.setSavePath(saveLoc.getPath() + "/");

        TreeStage searchMin = new TreeStage_MinDepth(minDepth, new Sampler_FixedDepth(minDepth), saver);

        // Grab some workers from the pool.
        List<TreeWorker> tws2 = new ArrayList<>();
        for (int i = 0; i < numWorkersToUse; i++) {
            try {
                tws2.add(borrowWorker());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        searchMin.initialize(tws2, rootNode);

        float elapsedSeconds = Math.floorDiv(System.currentTimeMillis() - startTime, 100) / 10f; // To one decimal
		// place.
        appendSummaryLog(logPrefix + "Finished after " + elapsedSeconds + " seconds.");
        appendSummaryLog(logPrefix + "Did " + searchMin.getResults().size() + " deviations.");

        // Return the checked out workers.
        for (TreeWorker w : tws2) {
            returnWorker(w);
        }
    }

    /**
     * Does a search where all games are played until failure, and all games are saved densely. The search is greedy.
     * The goal is to collect a lot of running data, including falls.
     *
     * @param rootNode Root node that the tree is built from.
     * @param saveName Save file name prefix. Timestamp will be appended in the final name.
     * @param fractionOfWorkers Portion of max workers used by this stage.
     * @param numGames Number of games to play.
     */
    protected void doFixedGamesToFailureStage(Node rootNode, String saveName, float fractionOfWorkers, int numGames) {
        if (fractionOfWorkers > 1)
            throw new RuntimeException("Cannot request more than 100% (i.e. fraction of 1) workers available.");

        String logPrefix = "FixedGamesToFailureStage: ";

        long startTime = System.currentTimeMillis();
        int numWorkersToUse = (int) Math.max(1, fractionOfWorkers * maxWorkers);
        writeLogStageHeader(logPrefix, saveName, rootNode.getTreeDepth(), numWorkersToUse, numGames);

        DataSaver_DenseTFRecord saver = new DataSaver_DenseTFRecord();
        saver.filePrefix = saveName;
        saver.setSavePath(saveLoc.getPath() + "/");

        saver.setSaveInterval(1000);
        Sampler_Greedy sampler = new Sampler_Greedy(new EvaluationFunction_Distance());
        TreeStage_FixedGames search = new TreeStage_FixedGames(numGames, sampler, saver); // Depth to get to

        // Grab some workers from the pool.
        List<TreeWorker> tws1 = new ArrayList<>();
        for (int i = 0; i < numWorkersToUse; i++) {
            tws1.add(borrowWorker());
        }

        // Do stage search
        search.initialize(tws1, rootNode);

        float elapsedSeconds = Math.floorDiv(System.currentTimeMillis() - startTime, 100) / 10f; // To one decimal
        // place.
        appendSummaryLog(logPrefix + "Finished after " + elapsedSeconds + " seconds.");
        appendSummaryLog(logPrefix + "Results -- " + (search.getResults().isEmpty() ? "<goal not met>" :
                search.getResults().get(0).maxBranchDepth.get() + " depth achieved."));
        // Return the checked out workers.
        for (TreeWorker w : tws1) {
            returnWorker(w);
        }
    }

    /**
     * This is the heavyweight, full UI, with tree visualization and a bunch of data visualization tabs. Includes some
     * TFlow components which are troublesome on some computers.
     */
    private UI_Full setupFullUI() {
        UI_Full fullUI = new UI_Full();

        /* Make each UI component */
        PanelRunner_AnimatedTransformed runnerPanel = new PanelRunner_AnimatedTransformed();
        PanelRunner_Snapshot snapshotPane = new PanelRunner_Snapshot();
        PanelRunner_Comparison comparisonPane = new PanelRunner_Comparison();
        PanelPlot_States statePlotPane = new PanelPlot_States(6); // 6 plots per view at the bottom.
        PanelPie_ViableFutures viableFuturesPane = new PanelPie_ViableFutures();
        PanelHistogram_LeafDepth leafDepthPane = new PanelHistogram_LeafDepth();
        PanelPlot_Transformed pcaPlotPane =
				new PanelPlot_Transformed(new Transform_PCA(IntStream.range(0, 72).toArray()), 6);
        PanelPlot_Controls controlsPlotPane = new PanelPlot_Controls(6); // 6 plots per view at the bottom.
        PanelPlot_Transformed autoencPlotPane =
				new PanelPlot_Transformed(new Transform_Autoencoder("src/main/resources/tflow_models" +
						"/AutoEnc_72to12_6layer.pb", 12), 6);
        autoencPlotPane.addFilter(new NodeFilter_SurvivalHorizon(1));
        PanelPlot_SingleRun singleRunPlotPane = new PanelPlot_SingleRun(6);
        workerMonitorPanel = new PanelTimeSeries_WorkerLoad(maxWorkers);

        fullUI.addTab(runnerPanel, "Run Animation");
        fullUI.addTab(snapshotPane, "State Viewer");
        fullUI.addTab(comparisonPane, "State Compare");
        fullUI.addTab(statePlotPane, "State Plots");
        fullUI.addTab(viableFuturesPane, "Viable Futures");
        fullUI.addTab(leafDepthPane, "Leaf depth distribution");
        fullUI.addTab(controlsPlotPane, "Controls Plots");
        fullUI.addTab(singleRunPlotPane, "Single Run Plots");
        fullUI.addTab(pcaPlotPane, "PCA Plots");
        fullUI.addTab(autoencPlotPane, "Autoenc Plots");
        fullUI.addTab(workerMonitorPanel, "Worker status");

        Thread runnerPanelThread = new Thread(runnerPanel); // All components with a copy of the GameThreadSafe should
		// have their own threads.
        runnerPanelThread.start();

        Thread monitorThread = new Thread(workerMonitorPanel);
        monitorThread.start();

        System.out.println("GUI: Running in full graphics mode.");
        return fullUI;
    }

    /**
     * Add something to the log which will be finalized when the program is closed.
     */
    public void appendSummaryLog(String addedLine) {
        endLog += addedLine + "\n";
        System.out.println(addedLine);
    }

    /**
     * Write the standard log header for a {@link TreeStage} to the log file.
     *
     * @param stageName Name of the stage to be executed.
     * @param saveName Name of the file to which data is going to be saved.
     * @param treedepth Starting tree depth of this stage.
     * @param numWorkers Number of {@link TreeWorker workers} assigned to this stage.
     * @param maxGames Maximum number of games to be played by this stage.
     */
    private void writeLogStageHeader(String stageName, String saveName, int treedepth, int numWorkers, int maxGames) {
        appendSummaryLog(stageName + "starting from a root of absolute depth " + treedepth);
        appendSummaryLog(stageName + "save file,  " + saveName);
        appendSummaryLog(stageName + "playing a max of " + maxGames + " games.");
        appendSummaryLog(stageName + "checked out " + numWorkers + " workers.");
    }

    /**
     * Return the save location file.
     */
    protected File getSaveLocation() {
        return saveLoc;
    }

    /**
     * Assign the correct generator of actions based on the baseline options and exceptions.
     * Will assign a broader set of options for "recovery" at the specified starting depth.
     * Pass -1 to disable this.
     */
    public static void assignAllowableActions(int recoveryExceptionStart) {
        /* Space of allowed actions to sample */
        //Distribution<Action> uniform_dist = new Distribution_Equal();

        /* Repeated action 1 -- no keys pressed. */
        Distribution<Action> dist1 = new Distribution_Normal(10f, 2f);
        ActionSet actionSet1 = ActionSet.makeActionSet(IntStream.range(1, 25).toArray(), new boolean[]{false, false, false,
                false}, dist1);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> dist2 = new Distribution_Normal(39f, 3f);
        ActionSet actionSet2 = ActionSet.makeActionSet(IntStream.range(20, 60).toArray(), new boolean[]{false, true, true,
                false}, dist2);

        /* Repeated action 3 -- W-O pressed */
        Distribution<Action> dist3 = new Distribution_Normal(10f, 2f);
        ActionSet actionSet3 = ActionSet.makeActionSet(IntStream.range(1, 25).toArray(), new boolean[]{false, false, false,
                false}, dist3);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> dist4 = new Distribution_Normal(39f, 3f);
        ActionSet actionSet4 = ActionSet.makeActionSet(IntStream.range(20, 60).toArray(), new boolean[]{true, false, false,
                true}, dist4);

        ActionSet[] repeatedActions = new ActionSet[]{actionSet1, actionSet2, actionSet3, actionSet4};

        /////// Action Exceptions for starting up. ////////
        /* Repeated action exceptions 1 -- no keys pressed. */
        Distribution<Action> distE1 = new Distribution_Normal(5f, 1f);
        ActionSet actionSetE1 = ActionSet.makeActionSet(IntStream.range(1, 25).toArray(), new boolean[]{false, false, false,
                false}, distE1);

        /*  Repeated action exceptions 2 -- W-O pressed */
        Distribution<Action> distE2 = new Distribution_Normal(34f, 2f);
        ActionSet actionSetE2 = ActionSet.makeActionSet(IntStream.range(20, 50).toArray(), new boolean[]{false, true, true,
                false}, distE2);

        /*  Repeated action exceptions 3 -- no keys pressed. */
        Distribution<Action> distE3 = new Distribution_Normal(24f, 2f);
        ActionSet actionSetE3 = ActionSet.makeActionSet(IntStream.range(10, 45).toArray(), new boolean[]{false, false, false,
                false}, distE3);

        /*  Repeated action exceptions 4 -- Q-P pressed */
        Distribution<Action> distE4 = new Distribution_Normal(49f, 2f);
        ActionSet actionSetE4 = ActionSet.makeActionSet(IntStream.range(25, 65).toArray(), new boolean[]{true, false, false,
                true}, distE4);

        /////// Action Exceptions for recovery. ////////
        /*  Repeated action 1 and 3 -- Nothing pressed */
        Distribution<Action> distFalseFalse = new Distribution_Normal(10f, 2f);
        ActionSet actionSetFalseFalse = ActionSet.makeActionSet(IntStream.range(1, 50).toArray(), new boolean[]{false, false,
                false, false}, distFalseFalse);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> distWO = new Distribution_Normal(39f, 3f);
        ActionSet actionSetWO = ActionSet.makeActionSet(IntStream.range(1, 70).toArray(), new boolean[]{false, true, true,
                false}, distWO);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> distQP = new Distribution_Normal(39f, 3f);
        ActionSet actionSetQP = ActionSet.makeActionSet(IntStream.range(1, 70).toArray(), new boolean[]{true, false, false,
                true}, distQP);

        Map<Integer, ActionSet> actionExceptions = new HashMap<>();
        actionExceptions.put(0, actionSetE1);
        actionExceptions.put(1, actionSetE2);
        actionExceptions.put(2, actionSetE3);
        actionExceptions.put(3, actionSetE4);

        // Put the recovery exceptions in the right spot.
        if (recoveryExceptionStart >= 0) {
            for (int i = 0; i < 4; i++) {
                int sequencePos = (recoveryExceptionStart + i) % 4;

                switch (sequencePos) {
                    case 0:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetFalseFalse);
                        break;
                    case 1:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetWO);
                        break;
                    case 2:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetFalseFalse);
                        break;
                    case 3:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetQP);
                        break;
                    default:
                        throw new IllegalStateException("unknown sequence position.");
                }
            }
        }
        // Define the specific way that these allowed actions are assigned as potential options for nodes.
        Node.potentialActionGenerator = new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
    }

    public static void assignAllowableActionsWider(int recoveryExceptionStart) {
        /* Space of allowed actions to sample */
        //Distribution<Action> uniform_dist = new Distribution_Equal();

        /* Repeated action 1 -- no keys pressed. */
        Distribution<Action> dist1 = new Distribution_Normal(10f, 2f);
        ActionSet actionSet1 = ActionSet.makeActionSet(IntStream.range(1, 35).toArray(), new boolean[]{false, false,
                false,
                false}, dist1);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> dist2 = new Distribution_Normal(39f, 3f);
        ActionSet actionSet2 = ActionSet.makeActionSet(IntStream.range(5, 60).toArray(), new boolean[]{false, true,
                true,
                false}, dist2);

        /* Repeated action 3 -- W-O pressed */
        Distribution<Action> dist3 = new Distribution_Normal(10f, 2f);
        ActionSet actionSet3 = ActionSet.makeActionSet(IntStream.range(1, 35).toArray(), new boolean[]{false, false,
                false,
                false}, dist3);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> dist4 = new Distribution_Normal(39f, 3f);
        ActionSet actionSet4 = ActionSet.makeActionSet(IntStream.range(5, 60).toArray(), new boolean[]{true, false,
                false,
                true}, dist4);

        ActionSet[] repeatedActions = new ActionSet[]{actionSet1, actionSet2, actionSet3, actionSet4};

        /////// Action Exceptions for starting up. ////////
        /* Repeated action exceptions 1 -- no keys pressed. */
        Distribution<Action> distE1 = new Distribution_Normal(5f, 1f);
        ActionSet actionSetE1 = ActionSet.makeActionSet(IntStream.range(1, 35).toArray(), new boolean[]{false, false,
                false,
                false}, distE1);

        /*  Repeated action exceptions 2 -- W-O pressed */
        Distribution<Action> distE2 = new Distribution_Normal(34f, 2f);
        ActionSet actionSetE2 = ActionSet.makeActionSet(IntStream.range(5, 65).toArray(), new boolean[]{false, true,
                true,
                false}, distE2);

        /*  Repeated action exceptions 3 -- no keys pressed. */
        Distribution<Action> distE3 = new Distribution_Normal(24f, 2f);
        ActionSet actionSetE3 = ActionSet.makeActionSet(IntStream.range(1, 35).toArray(), new boolean[]{false, false,
                false,
                false}, distE3);

        /*  Repeated action exceptions 4 -- Q-P pressed */
        Distribution<Action> distE4 = new Distribution_Normal(49f, 2f);
        ActionSet actionSetE4 = ActionSet.makeActionSet(IntStream.range(5, 65).toArray(), new boolean[]{true, false,
                false,
                true}, distE4);

        /////// Action Exceptions for recovery. ////////
        /*  Repeated action 1 and 3 -- Nothing pressed */
        Distribution<Action> distFalseFalse = new Distribution_Normal(10f, 2f);
        ActionSet actionSetFalseFalse = ActionSet.makeActionSet(IntStream.range(1, 50).toArray(), new boolean[]{false, false,
                false, false}, distFalseFalse);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> distWO = new Distribution_Normal(39f, 3f);
        ActionSet actionSetWO = ActionSet.makeActionSet(IntStream.range(1, 70).toArray(), new boolean[]{false, true, true,
                false}, distWO);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> distQP = new Distribution_Normal(39f, 3f);
        ActionSet actionSetQP = ActionSet.makeActionSet(IntStream.range(1, 70).toArray(), new boolean[]{true, false, false,
                true}, distQP);

        Map<Integer, ActionSet> actionExceptions = new HashMap<>();
        actionExceptions.put(0, actionSetE1);
        actionExceptions.put(1, actionSetE2);
        actionExceptions.put(2, actionSetE3);
        actionExceptions.put(3, actionSetE4);

        // Put the recovery exceptions in the right spot.
        if (recoveryExceptionStart >= 0) {
            for (int i = 0; i < 4; i++) {
                int sequencePos = (recoveryExceptionStart + i) % 4;

                switch (sequencePos) {
                    case 0:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetFalseFalse);
                        break;
                    case 1:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetWO);
                        break;
                    case 2:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetFalseFalse);
                        break;
                    case 3:
                        actionExceptions.put(recoveryExceptionStart + i, actionSetQP);
                        break;
                    default:
                        throw new RuntimeException("unknown sequence position.");
                }
            }
        }
        // Define the specific way that these allowed actions are assigned as potential options for nodes.
        Node.potentialActionGenerator = new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
    }
}
