package goals.tree_search;

import controllers.Controller_Random;
import game.action.ActionGenerator_FixedSequence;
import game.qwop.CommandQWOP;
import game.action.IActionGenerator;
import data.SavableFileIO;
import data.SavableSingleGame;
import data.SparseDataToDenseTFRecord;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.node.*;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.sampler.ISampler;
import tree.sampler.Sampler_UCB;
import tree.TreeWorker;
import tree.node.NodeGameExplorable;
import tree.node.NodeGameGraphics;
import tree.sampler.rollout.RolloutPolicyBase;
import tree.sampler.rollout.RolloutPolicy_DeltaScore;
import value.updaters.ValueUpdater_Average;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Does the full search in 4 stages.
 * 1. get to steady running depth.
 * 2. expand some node with a bunch of deviations from steady.
 * 3. find recoveries for each deviation.
 * 4. convert sparsely saved data to dense and save to file.
 *
 * @author matt
 */
public class MAIN_Search_Full extends SearchTemplate {

    private static final Logger logger = LogManager.getLogger(MAIN_Search_Full.class);

    public MAIN_Search_Full() {
        super(new File("src/main/resources/config/search.config_full"));
    }

    public static void main(String[] args) {
        MAIN_Search_Full manager = new MAIN_Search_Full();
        manager.doGames();
    }

    public void doGames() {

        // Load all parameters specific to this search.
//        Sampler_UCB.explorationMultiplier = Float.parseFloat(properties.getProperty("UCBExplorationMultiplier", "1"));
        boolean doStage1 = Boolean.parseBoolean(properties.getProperty("doStage1", "false"));
        boolean doStage2 = Boolean.parseBoolean(properties.getProperty("doStage2", "false"));
        boolean doStage3 = Boolean.parseBoolean(properties.getProperty("doStage3", "false"));
        boolean doStage4 = Boolean.parseBoolean(properties.getProperty("doStage4", "false"));

        boolean autoResume = Boolean.parseBoolean(properties.getProperty("autoResume", "true")); // If true, overrides the
        // recoveryResumePoint and looks at which files have been completed.

        // Stage 1
        int getToSteadyDepth = Integer.parseInt(properties.getProperty("getToSteadyDepth", "18")); // Stage terminates
        // after any part of the tree reaches this depth.
        float maxWorkerFraction1 = Float.parseFloat(properties.getProperty("fractionOfMaxWorkers1", "1")); // Fraction
        // of the maximum workers used for this stage.
        int bailAfterXGames1 = Integer.parseInt(properties.getProperty("bailAfterXGames1", "1000000")); // Stop stage
        // 1 after this many games even if we don't reach the goal depth.
        String fileSuffix1 = properties.getProperty("fileSuffix1", "");

        String filename1 = "steadyRunPrefix" + fileSuffix1;

        // Stage 2
        int trimSteadyBy = Integer.parseInt(properties.getProperty("trimSteadyBy", "7"));
        int deviationDepth = Integer.parseInt(properties.getProperty("deviationDepth", "2"));
        float maxWorkerFraction2 = Float.parseFloat(properties.getProperty("fractionOfMaxWorkers2", "1"));
        int bailAfterXGames2 = Integer.parseInt(properties.getProperty("bailAfterXGames2", "1000000")); // Stop stage
        // 1 after this many games even if we don't reach the goal depth.
        String fileSuffix2 = properties.getProperty("fileSuffix2", "");

        String filename2 = "deviations" + fileSuffix2;

        // Stage 3
        int stage3StartDepth = getToSteadyDepth - trimSteadyBy + deviationDepth;
        int recoveryResumePoint = Integer.parseInt(properties.getProperty("resumePoint", "0")); // Return here if
        // we're restarting.
        int getBackToSteadyDepth = Integer.parseInt(properties.getProperty("recoveryActions", "14")); // This many
        // moves to recover.
        float maxWorkerFraction3 = Float.parseFloat(properties.getProperty("fractionOfMaxWorkers3", "1"));
        int bailAfterXGames3 = Integer.parseInt(properties.getProperty("bailAfterXGames3", "100000")); // Stop stage 1
        // after this many games even if we don't reach the goal depth.

        String fileSuffix3 = properties.getProperty("fileSuffix3", "");

        // Stage 4
        int trimEndBy = Integer.parseInt(properties.getProperty("trimEndBy", "4"));

        ///////////////////////////////////////////////////////////

        IActionGenerator<CommandQWOP> actionGenerator =
                ActionGenerator_FixedSequence.makeDefaultGenerator(stage3StartDepth);

        // This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.
        // Check if we actually need to do stage 1.
        if (doStage1 && autoResume) {
            File[] existingFiles = getSaveLocation().listFiles();
            for (File f : Objects.requireNonNull(existingFiles)) {
                if (f.getName().contains("steadyRunPrefix")) {
                    logger.info("Found a completed stage 1 file. Skipping.");
                    doStage1 = false;
                    break;
                }
            }
        }

        if (doStage1) {
            NodeGameGraphics<CommandQWOP, StateQWOP> rootNode = new NodeGameGraphics<>(GameQWOP.getInitialState(),
                    actionGenerator);
            NodeGameGraphics.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(rootNode);

            logger.info("Starting stage 1.");
            doBasicMaxDepthStage(rootNode, filename1, getToSteadyDepth, maxWorkerFraction1, bailAfterXGames1);
            logger.info("Stage 1 done.");
        }

        // This stage generates deviations from nominal. Load nominal gait. Do not allow expansion near the root.
        // Expand to a fixed, small depth.
        // Check if we actually need to do stage 2.
        if (doStage2 && autoResume) {
            File[] existingFiles = getSaveLocation().listFiles();
            for (File f : Objects.requireNonNull(existingFiles)) {
                if (f.getName().contains("deviations")) {
                    logger.info("Found a completed stage 2 file. Skipping.");
                    doStage2 = false;
                    break;
                }
            }
        }
        if (doStage2) {
            logger.info("Starting stage 2.");
            NodeGameGraphics<CommandQWOP, StateQWOP> rootNode = new NodeGameGraphics<>(GameQWOP.getInitialState(),
                    actionGenerator);

            NodeGameGraphics.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(rootNode);

            SavableFileIO<SavableSingleGame<CommandQWOP, StateQWOP>> fileIO = new SavableFileIO<>();
            List<SavableSingleGame<CommandQWOP, StateQWOP>> glist = new ArrayList<>();
            File saveFile = new File(getSaveLocation().getPath() + "/" + filename1 +
                    ".SavableSingleGame");
            fileIO.loadObjectsToCollection(saveFile, glist);
            NodeGame.makeNodesFromRunInfo(glist, rootNode);
            NodeGameExplorable.stripUncheckedActionsExceptOnLeaves(rootNode, getToSteadyDepth - trimSteadyBy - 1);
            NodeGameGraphics<CommandQWOP, StateQWOP> currNode = rootNode;
            while (currNode.getTreeDepth() < getToSteadyDepth - trimSteadyBy) {
                currNode = currNode.getChildByIndex(0);
            }

            doBasicMinDepthStage(currNode, filename2, deviationDepth, maxWorkerFraction2, bailAfterXGames2);

            logger.info("Stage 2 done.");
        }

        // Expand the deviated spots and find recoveries.
        if (doStage3) {
            logger.info("Starting stage 3.");

            // Auto-detect where we left off if this setting is selected.
            if (autoResume) {
                File[] existingFiles = getSaveLocation().listFiles();
                int startIdx = 0;
                boolean foundFile = false;
                while (startIdx < Objects.requireNonNull(existingFiles).length) {
                    for (File f : existingFiles) {
                        if (f.getName().contains("recoveries" + startIdx)) {
                            logger.info("Found file for recovery " + startIdx);
                            foundFile = true;
                            break;
                        }
                    }
                    if (!foundFile)
                        break;
                    startIdx++;
                    foundFile = false;
                }
                logger.info("Resuming at recovery #: " + startIdx);
                recoveryResumePoint = startIdx;
            }

            // Saver resetGame.
            NodeGameGraphics<CommandQWOP, StateQWOP> rootNode = new NodeGameGraphics<>(GameQWOP.getInitialState(),
                    actionGenerator);
            NodeGameGraphics.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(rootNode);

            SavableFileIO<SavableSingleGame<CommandQWOP, StateQWOP>> fileIO = new SavableFileIO<>();
            List<SavableSingleGame<CommandQWOP, StateQWOP>> glist = new ArrayList<>();
            File saveFile = new File(getSaveLocation().getPath() + "/" + filename2 +
                    ".SavableSingleGame");
            fileIO.loadObjectsToCollection(saveFile, glist);
            NodeGame.makeNodesFromRunInfo(glist, rootNode);
            NodeGameExplorableBase.stripUncheckedActionsExceptOnLeaves(rootNode, stage3StartDepth);


            List<NodeGameGraphics<CommandQWOP, StateQWOP>> leafList = new ArrayList<>();
            rootNode.getLeaves(leafList);

            int count = 0;
            int startAt = recoveryResumePoint;
            NodeGameGraphics previousLeaf = null;

            for (NodeGameGraphics<CommandQWOP, StateQWOP> leaf : leafList) {
                if (count >= startAt) {
                    doBasicMaxDepthStage(leaf, "recoveries" + count + fileSuffix3, getBackToSteadyDepth, maxWorkerFraction3, bailAfterXGames3);
                }
                // Turn off drawing for this one.
                leaf.turnOffBranchDisplay();
                leaf.getParent().removeFromChildren(leaf);
                if (previousLeaf != null) {
                    previousLeaf.destroyNodesBelowAndCheckExplored();
                }
                previousLeaf = leaf;

                count++;
                logger.info("Expanded leaf: " + count + ".");
            }
            logger.info("Stage 3 done.");
        }

        // Convert the sections of the runs we care about to dense data by re-simulating.
        if (doStage4) {
            List<File> filesToConvert = new ArrayList<>();
            File[] files = getSaveLocation().listFiles();
            for (File f : Objects.requireNonNull(files)) {
                if (f.toString().toLowerCase().contains("recoveries") && f.toString().toLowerCase().contains("savablesinglegame") && !f.toString().toLowerCase().contains("unsuccessful")) {
                    filesToConvert.add(f);
                }
            }
            SparseDataToDenseTFRecord converter = new SparseDataToDenseTFRecord(getSaveLocation().getAbsolutePath() + "/");
            converter.trimFirst = stage3StartDepth;
            converter.trimLast = trimEndBy;
            converter.convert(filesToConvert, true);

            logger.info("Stage 4 done.");
        }
    }

    @Override
    TreeWorker<CommandQWOP, StateQWOP> getTreeWorker() {
        ISampler<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(
                new EvaluationFunction_Constant<>(0f),
                new RolloutPolicy_DeltaScore<>(
                        new EvaluationFunction_Distance<>(),
                        RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                        new Controller_Random<>()),
                new ValueUpdater_Average<>(), 5, 1); // TODO hardcoded.
        return TreeWorker.makeStandardQWOPTreeWorker(sampler);
    }
}
