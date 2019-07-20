package goals.tree_search;

import game.GameUnified;
import game.state.transform.Transform_Autoencoder;
import game.state.transform.Transform_PCA;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.TreeWorker;
import tree.Utility;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.node.filter.NodeFilter_SurvivalHorizon;
import tree.sampler.Sampler_FixedDepth;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.RolloutPolicyBase;
import tree.stage.TreeStage;
import tree.stage.TreeStage_FixedGames;
import tree.stage.TreeStage_MaxDepth;
import tree.stage.TreeStage_MinDepth;
import ui.IUserInterface;
import ui.PanelLogger;
import ui.UI_Full;
import ui.UI_Headless;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner_AnimatedTransformed;
import ui.runner.PanelRunner_Comparison;
import ui.runner.PanelRunner_ControlledTFlow;
import ui.runner.PanelRunner_Snapshot;
import ui.scatterplot.PanelPlot_Controls;
import ui.scatterplot.PanelPlot_SingleRun;
import ui.scatterplot.PanelPlot_States;
import ui.scatterplot.PanelPlot_Transformed;
import ui.timeseries.PanelTimeSeries_WorkerLoad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

public abstract class SearchTemplate {

    /**
     * Logging to console or file.
     */
    private static Logger logger;
    static {
        Utility.loadLoggerConfiguration();
        logger = LogManager.getLogger(SearchTemplate.class);
    }

    /**
     * Will look for a configuration file in this path.
     */
    static final String configFilePath = "src/main/resources/config/";

    /**
     * Settings loaded from the config file. Should AT LEAST contain:
     * boolean headless
     * string saveLocation
     * workersFractionOfCores
     */
    protected Properties properties;

    /**
     * Where should data be saved?
     */
    private File saveLoc;

    /**
     * Whether or not to run without the full GUI.
     */
    protected final boolean headless;

    /**
     * Some user interface which information will be sent to.
     */
    protected final IUserInterface ui;

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

    public SearchTemplate(File configFile) {

        // Load the configuration file.
        properties = Utility.loadConfigFile(configFile);
        float workersFractionOfCores = Float.parseFloat(properties.getProperty("workersFractionOfCores", "0.8"));
        headless = Boolean.valueOf(properties.getProperty("headless", "false")); // Default to using fullUI

        // Create the data save directory.
        saveLoc = new File("src/main/resources/saved_data/" + properties.getProperty("saveLocation", "./"));

        if (!saveLoc.exists()) {
            boolean success = saveLoc.mkdirs();
            if (!success) try {
                throw new FileNotFoundException("Could not make save directory.");
            } catch (FileNotFoundException e) {
                logger.error("Could not make save directory.", e);
            }
            logger.info("Made a data-saving directory at: " + saveLoc.getName());
        } else {
            logger.info("Using existing data-save directory at: " + saveLoc.getName());
        }

        // Worker threads to run. Each worker independently explores the tree.
        maxWorkers = (int) (workersFractionOfCores * Runtime.getRuntime().availableProcessors());

        // UI CONFIG:
        logger.info("Full UI is " + (headless ? "not" : "") + "on.");
        ui = (headless) ? new UI_Headless() : setupFullUI(); // Make the UI.
        ui.start();

        // Copy the config file into the save directory.
        File configSave = new File(saveLoc.toString() + "/config_" + Utility.getTimestamp() + ".config");
        try {
            FileUtils.copyFile(configFile, configSave);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        logMachineDetails();
    }

    /**
     * Specific tree search will want to decide the configuration of the tree worker.
     * @return A {@link TreeWorker} configured specifically for the tree search's application.
     *
     */
    abstract TreeWorker getTreeWorker();

    List<TreeWorker> getTreeWorkers(int numberOfWorkers) {
        List<TreeWorker> workerList = new ArrayList<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            workerList.add(getTreeWorker());
        }
        return workerList;
    }

    /**
     * Stop keeping track of a specific <code>TreeWorker</code>. This will terminate the worker and remove it from
     * the {@link PanelTimeSeries_WorkerLoad}, if present.
     * @param finishedWorker A worker to terminate and stop tracking.
     */
    void removeWorker(TreeWorker finishedWorker) {
        finishedWorker.terminateWorker();
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
    protected void doBasicMaxDepthStage(NodeQWOPExplorableBase<?> rootNode, String saveName, int desiredDepth,
                                        float fractionOfWorkers,
                                        int maxGames) {
        if (fractionOfWorkers > 1)
            throw new IllegalArgumentException("Cannot request more than 100% (i.e. fraction of 1) workers available." +
                    " Asked for: " + fractionOfWorkers);

        String stageName = "BasicMaxDepthSearch";
        int numWorkersToUse = (int) Math.max(1, fractionOfWorkers * maxWorkers);
        logTreeStage(stageName, saveName, rootNode.getTreeDepth(), numWorkersToUse, maxGames);

        long startTime = System.currentTimeMillis();

        // Will save whatever the stage defines as important.
//        DataSaver_StageSelected saver = new DataSaver_StageSelected();
//        saver.overrideFilename = saveName;
//        saver.setSavePath(saveLoc.getPath() + "/");
//
//        IController randomController = new Controller_Random();
//        Sampler_UCB ucbSampler = new Sampler_UCB(new EvaluationFunction_Constant(0f),
//                new RolloutPolicy_DecayingHorizon(new EvaluationFunction_Distance(), randomController));
        // TODO now saver and sampler are supplied when creating the workers. Make sure everything is still
        //  consistent with this.
        TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(desiredDepth, maxGames);

        // Grab some workers from the pool.
        List<TreeWorker> tws1 = getTreeWorkers(numWorkersToUse);

        // Do stage search
        searchMax.initialize(tws1, rootNode);

        float elapsedSeconds = Math.floorDiv(System.currentTimeMillis() - startTime, 100) / 10f; // To one decimal
        // place.
        logger.info(stageName + " finished after " + elapsedSeconds + " seconds.\n" + "Results -- "
                + (searchMax.getResults().isEmpty() ? "<goal not met>" : searchMax.getResults().get(0).getTreeDepth() + " depth achieved."));

        // Return the checked out workers.
        tws1.forEach(this::removeWorker);
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
    protected void doBasicMinDepthStage(NodeQWOPExplorableBase<?> rootNode, String saveName, int minDepth, float fractionOfWorkers,
                                        int maxGames) {
        if (fractionOfWorkers > 1)
            throw new RuntimeException("Cannot request more than 100% (i.e. fraction of 1) workers available.");

        String stageName = "BasicMinDepthSearch: ";

        long startTime = System.currentTimeMillis();
        int numWorkersToUse = (int) Math.max(1, fractionOfWorkers * maxWorkers);
        logTreeStage(stageName, saveName, rootNode.getTreeDepth(), numWorkersToUse, maxGames);


        // TODO now saver and sampler are supplied when creating the workers. Make sure everything is still
        //  consistent with this.
        TreeStage searchMin = new TreeStage_MinDepth(minDepth);

        // Grab some workers from the pool.
        List<TreeWorker> tws2 = getTreeWorkers(numWorkersToUse);

        searchMin.initialize(tws2, rootNode);

        float elapsedSeconds = Math.floorDiv(System.currentTimeMillis() - startTime, 100) / 10f; // To one decimal
        // place.
        logger.info(stageName + " finished after " + elapsedSeconds + " seconds.\n" +  stageName + "did " + searchMin.getResults().size() + " deviations.");

        // Return the checked out workers.
        tws2.forEach(this::removeWorker);
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
    protected void doFixedGamesToFailureStage(NodeQWOPExplorableBase<?> rootNode, String saveName,
                                              float fractionOfWorkers, int numGames) {
        if (fractionOfWorkers > 1)
            throw new RuntimeException("Cannot request more than 100% (i.e. fraction of 1) workers available.");

        String logPrefix = "FixedGamesToFailureStage: ";

        long startTime = System.currentTimeMillis();
        int numWorkersToUse = (int) Math.max(1, fractionOfWorkers * maxWorkers);
        logTreeStage(logPrefix, saveName, rootNode.getTreeDepth(), numWorkersToUse, numGames);

        // TODO now saver and sampler are supplied when creating the workers. Make sure everything is still
        //  consistent with this.
        TreeStage_FixedGames search = new TreeStage_FixedGames(numGames); // Depth to get to

        // Grab some workers from the pool.
        List<TreeWorker> tws1 = getTreeWorkers(numWorkersToUse);

        // Do stage search
        search.initialize(tws1, rootNode);

        float elapsedSeconds = Math.floorDiv(System.currentTimeMillis() - startTime, 100) / 10f; // To one decimal
        // place.
        logger.info(logPrefix + "Finished after " + elapsedSeconds + " seconds." + logPrefix + "\nResults -- "
                + (search.getResults().isEmpty() ? "<goal not met>" : search.getResults().get(0).getMaxBranchDepth() + " depth achieved."));
        // Return the checked out workers.
        tws1.forEach(this::removeWorker);
    }

    /**
     * This is the heavyweight, full UI, with tree visualization and a bunch of data visualization tabs. Includes some
     * TFlow components which are troublesome on some computers.
     */
    public UI_Full setupFullUI() {
        UI_Full fullUI = new UI_Full();

        /* Make each UI component */
        PanelRunner_AnimatedTransformed runnerPanel = new PanelRunner_AnimatedTransformed("Run Animation");
        PanelRunner_Snapshot snapshotPane = new PanelRunner_Snapshot("State Viewer");
        PanelRunner_Comparison comparisonPane = new PanelRunner_Comparison("State Compare");
        PanelPlot_States statePlotPane = new PanelPlot_States("State Plots", 6); // 6 plots per view at the bottom.
        PanelPie_ViableFutures viableFuturesPane = new PanelPie_ViableFutures("Viable Futures");
        PanelHistogram_LeafDepth leafDepthPane = new PanelHistogram_LeafDepth("Leaf depth distribution");
        PanelPlot_Transformed pcaPlotPane =
                new PanelPlot_Transformed(new Transform_PCA(IntStream.range(0, 72).toArray()), "PCA Plots",6);
        PanelPlot_Controls controlsPlotPane = new PanelPlot_Controls("Controls Plots", 6); // 6 plots per view at the
        // bottom.
        PanelPlot_Transformed autoencPlotPane =
                new PanelPlot_Transformed(new Transform_Autoencoder("src/main/resources/tflow_models" +
                        "/AutoEnc_72to12_6layer.pb", 12), "Autoenc Plots", 6);
        autoencPlotPane.addFilter(new NodeFilter_SurvivalHorizon(1));
        PanelPlot_SingleRun singleRunPlotPane = new PanelPlot_SingleRun("Single Run Plots", 6);
        //workerMonitorPanel = new PanelTimeSeries_WorkerLoad("Worker status", maxWorkers);

        fullUI.addTab(new PanelLogger());
        fullUI.addTab(runnerPanel);
        fullUI.addTab(snapshotPane);
        fullUI.addTab(comparisonPane);
        fullUI.addTab(statePlotPane);
        fullUI.addTab(viableFuturesPane);
        fullUI.addTab(leafDepthPane);
        fullUI.addTab(controlsPlotPane);
        fullUI.addTab(singleRunPlotPane);
        fullUI.addTab(pcaPlotPane);
        fullUI.addTab(autoencPlotPane);

        PanelRunner_ControlledTFlow controllerPane
                = new PanelRunner_ControlledTFlow<>(
                "Controller",
                new GameUnified(),
                "src/main/resources/tflow_models",
                "src/main/resources/tflow_models/checkpoints");
        controllerPane.actionGenerator = RolloutPolicyBase.getRolloutActionGenerator();

        fullUI.addTab(controllerPane);

        fullUI.start();
        //fullUI.addTab(workerMonitorPanel);

//        Thread runnerPanelThread = new Thread(runnerPanel); // All components with a copy of the GameThreadSafe should
//        // have their own threads.
//        runnerPanelThread.start();

//        Thread monitorThread = new Thread(workerMonitorPanel);
//        monitorThread.start();


        logger.info("GUI: Running in full graphics mode.");
        return fullUI;
    }

    /**
     * Write the standard log header for a {@link TreeStage} to the log.
     *
     * @param stageName Name of the stage to be executed.
     * @param saveName Name of the file to which data is going to be saved.
     * @param treedepth Starting tree depth of this stage.
     * @param numWorkers Number of {@link TreeWorker workers} assigned to this stage.
     * @param maxGames Maximum number of games to be played by this stage.
     */
    private void logTreeStage(String stageName, String saveName, int treedepth, int numWorkers, int maxGames) {
        logger.info("Beginning " + stageName + ".\nStart depth: " + treedepth
                + "\nMax games to play: " + maxGames
                + "\nNumber of workers: " + numWorkers
                + "\nSave file: " + saveName);
    }

    /**
     * Write computer details (e.g. OS) to the logging stream.
     */
    private void logMachineDetails() {
        // Write machine details to log.
        logger.info("Machine info:\n " +  "OS: " + System.getProperty("os.name") + " " + System.getProperty("os" +
                ".version"));
        String hostname = "Unknown";
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            logger.warn("Machine name can not be resolved");
        }

        logger.info("Machine name: " + hostname + "\nSave directory: " + saveLoc.getAbsolutePath());
    }

    /**
     * Return the save location file.
     */
    protected File getSaveLocation() {
        return saveLoc;
    }
}
