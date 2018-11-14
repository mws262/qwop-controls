package goals.tree_search;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import data.SavableFileIO;
import data.SavableSingleGame;
import data.SparseDataToDenseTFRecord;
import samplers.Sampler_UCB;
import tree.Node;

/**
 * Does the full search in 4 stages.
 * 1. get to steady running depth.
 * 2. expand some node with a bunch of deviations from steady.
 * 3. find recoveries for each deviation.
 * 4. convert sparsely saved data to dense and save to file.
 *
 * @author matt
 */
public class MAIN_Search_Full extends MAIN_Search_Template {

    public MAIN_Search_Full() {
        super(new File("src/main/resources/config/search.config_full"));
    }

    public static void main(String[] args) {
        MAIN_Search_Full manager = new MAIN_Search_Full();
        manager.doGames();
    }

    public void doGames() {

        // Load all parameters specific to this search.
        Sampler_UCB.explorationMultiplier = Float.valueOf(properties.getProperty("UCBExplorationMultiplier", "1"));
        boolean doStage1 = Boolean.valueOf(properties.getProperty("doStage1", "false"));
        boolean doStage2 = Boolean.valueOf(properties.getProperty("doStage2", "false"));
        boolean doStage3 = Boolean.valueOf(properties.getProperty("doStage3", "false"));
        boolean doStage4 = Boolean.valueOf(properties.getProperty("doStage4", "false"));

        boolean autoResume = Boolean.valueOf(properties.getProperty("autoResume", "true")); // If true, overrides the
        // recoveryResumePoint and looks at which files have been completed.

        // Stage 1
        int getToSteadyDepth = Integer.valueOf(properties.getProperty("getToSteadyDepth", "18")); // Stage terminates
        // after any part of the tree reaches this depth.
        float maxWorkerFraction1 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers1", "1")); // Fraction
        // of the maximum workers used for this stage.
        int bailAfterXGames1 = Integer.valueOf(properties.getProperty("bailAfterXGames1", "1000000")); // Stop stage
        // 1 after this many games even if we don't reach the goal depth.
        String fileSuffix1 = properties.getProperty("fileSuffix1", "");

        String filename1 = "steadyRunPrefix" + fileSuffix1;

        // Stage 2
        int trimSteadyBy = Integer.valueOf(properties.getProperty("trimSteadyBy", "7"));
        int deviationDepth = Integer.valueOf(properties.getProperty("deviationDepth", "2"));
        float maxWorkerFraction2 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers2", "1"));
        int bailAfterXGames2 = Integer.valueOf(properties.getProperty("bailAfterXGames2", "1000000")); // Stop stage
        // 1 after this many games even if we don't reach the goal depth.
        String fileSuffix2 = properties.getProperty("fileSuffix2", "");

        String filename2 = "deviations" + fileSuffix2;

        // Stage 3
        int stage3StartDepth = getToSteadyDepth - trimSteadyBy + deviationDepth;
        int recoveryResumePoint = Integer.valueOf(properties.getProperty("resumePoint", "0")); // Return here if
        // we're restarting.
        int getBackToSteadyDepth = Integer.valueOf(properties.getProperty("recoveryActions", "14")); // This many
        // moves to recover.
        float maxWorkerFraction3 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers3", "1"));
        int bailAfterXGames3 = Integer.valueOf(properties.getProperty("bailAfterXGames3", "100000")); // Stop stage 1
        // after this many games even if we don't reach the goal depth.

        String fileSuffix3 = properties.getProperty("fileSuffix3", "");

        // Stage 4
        int trimEndBy = Integer.valueOf(properties.getProperty("trimEndBy", "4"));

        ///////////////////////////////////////////////////////////

        assignAllowableActions(stage3StartDepth);

        // This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.
        // Check if we actually need to do stage 1.
        if (doStage1 && autoResume) {
            File[] existingFiles = getSaveLocation().listFiles();
            for (File f : Objects.requireNonNull(existingFiles)) {
                if (f.getName().contains("steadyRunPrefix")) {
                    appendSummaryLog("Found a completed stage 1 file. Skipping.");
                    doStage1 = false;
                    break;
                }
            }
        }

        if (doStage1) {
            Node rootNode = new Node();
            Node.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(rootNode);

            appendSummaryLog("Starting stage 1.");
            doBasicMaxDepthStage(rootNode, filename1, getToSteadyDepth, maxWorkerFraction1, bailAfterXGames1);
            appendSummaryLog("Stage 1 done.");
        }

        // This stage generates deviations from nominal. Load nominal gait. Do not allow expansion near the root.
        // Expand to a fixed, small depth.
        // Check if we actually need to do stage 2.
        if (doStage2 && autoResume) {
            File[] existingFiles = getSaveLocation().listFiles();
            for (File f : Objects.requireNonNull(existingFiles)) {
                if (f.getName().contains("deviations")) {
                    appendSummaryLog("Found a completed stage 2 file. Skipping.");
                    doStage2 = false;
                    break;
                }
            }
        }
        if (doStage2) {
            appendSummaryLog("Starting stage 2.");
            Node rootNode = new Node();

            Node.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(rootNode);

            SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();
            List<SavableSingleGame> glist = new ArrayList<>();
            File saveFile = new File(getSaveLocation().getPath() + "/" + filename1 +
                    ".SavableSingleGame");
            fileIO.loadObjectsToCollection(saveFile, glist);
            Node.makeNodesFromRunInfo(glist, rootNode, getToSteadyDepth - trimSteadyBy - 1);
            Node currNode = rootNode;
            while (currNode.getTreeDepth() < getToSteadyDepth - trimSteadyBy) {
                currNode = currNode.getChildByIndex(0);
            }

            doBasicMinDepthStage(currNode, filename2, deviationDepth, maxWorkerFraction2, bailAfterXGames2);

            appendSummaryLog("Stage 2 done.");
        }

        // Expand the deviated spots and find recoveries.
        if (doStage3) {
            appendSummaryLog("Starting stage 3.");

            // Auto-detect where we left off if this setting is selected.
            if (autoResume) {
                File[] existingFiles = getSaveLocation().listFiles();
                int startIdx = 0;
                boolean foundFile = false;
                while (startIdx < Objects.requireNonNull(existingFiles).length) {
                    for (File f : existingFiles) {
                        if (f.getName().contains("recoveries" + startIdx)) {
                            appendSummaryLog("Found file for recovery " + startIdx);
                            foundFile = true;
                            break;
                        }
                    }
                    if (!foundFile)
                        break;
                    startIdx++;
                    foundFile = false;
                }
                appendSummaryLog("Resuming at recovery #: " + startIdx);
                recoveryResumePoint = startIdx;
            }

            // Saver setup.
            Node rootNode = new Node();
            Node.pointsToDraw.clear();
            ui.clearRootNodes();
            ui.addRootNode(rootNode);

            SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();
            List<SavableSingleGame> glist = new ArrayList<>();
            File saveFile = new File(getSaveLocation().getPath() + "/" + filename2 +
                    ".SavableSingleGame");
            fileIO.loadObjectsToCollection(saveFile, glist);
            Node.makeNodesFromRunInfo(glist, rootNode, stage3StartDepth);

            List<Node> leafList = new ArrayList<>();
            rootNode.getLeaves(leafList);

            int count = 0;
            int startAt = recoveryResumePoint;
            Node previousLeaf = null;

            for (Node leaf : leafList) {
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
                appendSummaryLog("Expanded leaf: " + count + ".");
            }
            appendSummaryLog("Stage 3 done.");
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

            appendSummaryLog("Stage 4 done.");
        }
    }
}
