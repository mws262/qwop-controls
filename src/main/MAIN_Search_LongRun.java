package main;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import data.SparseDataToDense;
import samplers.Sampler_UCB;
/**
 * Does the full search in 4 stages.
 * 1. get to steady running depth.
 * 2. expand some node with a bunch of deviations from steady.
 * 3. find recoveries for each deviation.
 * 4. convert sparsely saved data to dense and save to file.
 * 
 * @author matt
 *
 */
public class MAIN_Search_LongRun extends MAIN_Search_Template {

	public MAIN_Search_LongRun() {
		super(new File(Utility.getExcutionPath() + "search.config_long"));
	}

	public static void main(String[] args) {
		MAIN_Search_LongRun manager = new MAIN_Search_LongRun();
		manager.doGames();
	}

	public void doGames() {

		// Load all parameters specific to this search.
		Sampler_UCB.explorationMultiplier = Float.valueOf(properties.getProperty("UCBExplorationMultiplier", "1"));
		boolean doStage1 = Boolean.valueOf(properties.getProperty("doStage1", "false"));
		boolean doStage2 = Boolean.valueOf(properties.getProperty("doStage2", "false"));

		// Stage 1 - searching
		int runsToGenerate = Integer.valueOf(properties.getProperty("runsToGenerate", "1"));
		int getToSteadyDepth = Integer.valueOf(properties.getProperty("getToSteadyDepth", "18")); // Stage terminates after any part of the tree reaches this depth.
		float maxWorkerFraction1 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers1", "1")); // Fraction of the maximum workers used for this stage.
		int bailAfterXGames1 = Integer.valueOf(properties.getProperty("bailAfterXGames1", "1000000")); // Stop stage 1 after this many games even if we don't reach the goal depth.
		String fileSuffix1 = properties.getProperty("fileSuffix1", "");

		String filename1 = "single_run_" + fileSuffix1;

		// Stage 2 saving
		int trimStartBy = 0; 
		int trimEndBy = Integer.valueOf(properties.getProperty("trimEndBy", "4"));

		///////////////////////////////////////////////////////////

		assignAllowableActions(-1);

		// This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.
		// Check if we actually need to do stage 1.

		if (doStage1) {
			int count = 0;
			while (count < runsToGenerate) {
				Node rootNode = new Node();
				ui.clearRootNodes();
				ui.addRootNode(rootNode);

				appendSummaryLog("Starting stage 1. Run: " + count + ".");
				doBasicMaxDepthStage(rootNode, filename1 + Utility.getTimestamp(), getToSteadyDepth, maxWorkerFraction1, bailAfterXGames1);
				appendSummaryLog("Stage 1 done. Run: " + count + ".");
			}
		}


		// Convert the sections of the runs we care about to dense data by resimulating.
		if (doStage2) {
			List<File> filesToConvert = new ArrayList<File>();
			File[] files = getSaveLocation().listFiles();
			for (File f : files) {
				if (f.toString().toLowerCase().contains("single_run_") && f.toString().toLowerCase().contains("saveablesinglegame") && !f.toString().toLowerCase().contains("unsuccessful")) {
					filesToConvert.add(f);
				}
			}
			SparseDataToDense converter = new SparseDataToDense(getSaveLocation().getAbsolutePath() + "/");
			converter.trimFirst = trimStartBy;
			converter.trimLast = trimEndBy;
			converter.convert(filesToConvert, true);	

			appendSummaryLog("Stage 2 done.");
		}
	}
}