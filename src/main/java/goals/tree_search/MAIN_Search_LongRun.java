package goals.tree_search;

import actions.IActionGenerator;
import data.SparseDataToDenseTFRecord;
import game.GameUnified;
import samplers.Sampler_UCB;
import tree.NodeQWOPGraphics;
import tree.TreeWorker;
import tree.Utility;

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
public class MAIN_Search_LongRun extends MAIN_Search_Template {

    public MAIN_Search_LongRun() {
        super(new File("src/main/resources/config/" + "search.config_long"));
    }

    public static void main(String[] args) {
        MAIN_Search_LongRun manager = new MAIN_Search_LongRun();
        manager.doGames();
    }

    public void doGames() {
        // Load all parameters specific to this search.
        Sampler_UCB.explorationMultiplier = Float.parseFloat(properties.getProperty("UCBExplorationMultiplier", "1"));
        boolean doStage1 = Boolean.parseBoolean(properties.getProperty("doStage1", "false"));
        boolean doStage2 = Boolean.parseBoolean(properties.getProperty("doStage2", "false"));

        // Stage 1 - searching
        int runsToGenerate = Integer.parseInt(properties.getProperty("runsToGenerate", "1"));
        int getToSteadyDepth = Integer.parseInt(properties.getProperty("getToSteadyDepth", "18")); // Stage terminates
        // after any part of the tree reaches this depth.
        float maxWorkerFraction1 = Float.parseFloat(properties.getProperty("fractionOfMaxWorkers1", "1")); // Fraction
        // of the maximum workers used for this stage.
        int bailAfterXGames1 = Integer.parseInt(properties.getProperty("bailAfterXGames1", "1000000")); // Stop stage
        // 1 after this many games even if we don't reach the goal depth.
        String fileSuffix1 = properties.getProperty("fileSuffix1", "");

        String filename1 = "single_run_" + fileSuffix1;

        // Stage 2 saving
        int trimStartBy = 0;
        int trimEndBy = Integer.parseInt(properties.getProperty("trimEndBy", "4"));

        ///////////////////////////////////////////////////////////

        IActionGenerator actionGenerator = getExtendedActionGenerator(-1);

        // This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.
        // Check if we actually need to do stage 1.

        if (doStage1) {
            int count = 0;
            while (count < runsToGenerate) {
                NodeQWOPGraphics rootNode = new NodeQWOPGraphics(GameUnified.getInitialState(), actionGenerator);
                NodeQWOPGraphics.pointsToDraw.clear();
                ui.clearRootNodes();
                ui.addRootNode(rootNode);

                logger.info("Starting stage 1. Run: " + count + ".");
                doBasicMaxDepthStage(rootNode, filename1 + Utility.getTimestamp(), getToSteadyDepth,
                        maxWorkerFraction1, bailAfterXGames1);
                logger.info("Stage 1 done. Run: " + count + ".");
                count++;
            }
        }

        // Convert the sections of the runs we care about to dense data by re-simulating.
        if (doStage2) {
            List<File> filesToConvert = new ArrayList<>();
            File[] files = getSaveLocation().listFiles();
            for (File f : Objects.requireNonNull(files)) {
                if (f.toString().toLowerCase().contains("single_run_") && f.toString().toLowerCase().contains(
                        "savablesinglegame") && !f.toString().toLowerCase().contains("unsuccessful")) {
                    filesToConvert.add(f);
                }
            }
            SparseDataToDenseTFRecord converter = new SparseDataToDenseTFRecord(getSaveLocation().getAbsolutePath() + "/");
            converter.trimFirst = trimStartBy;
            converter.trimLast = trimEndBy;
            converter.convert(filesToConvert, true);

            logger.info("Stage 2 done.");
        }

        System.exit(0);
    }

    @Override
    TreeWorker getTreeWorker() {
        return TreeWorker.makeStandardTreeWorker();
    }
}
