package data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import main.Evaluator_HandTunedOnState;
import main.IEvaluationFunction;
import main.State;

/**
 * Processes the data from 10/31/17
 * Data was made by running overnight on AMD desktop.
 * Ran UCB search, c = 7, 3 minutes per.
 * 
 * One flaw is that the UCB sampler only saves after a game falls.
 * Many leaves are not falls for quite some time in UCB unlike in
 * my greedy searchers.
 * 
 * Filters out the best runs from each file, resimulates to get
 * data at every timestep, and then repackages.
 * 
 * @author matt
 *
 */
public class MAIN_Process_10_31 {

	/** Name of the directory with the sparse data files. **/
	private static String sourceDir = "data_10_31";
	
	/** Input file extension. **/
	private static String inFileExt = "SaveableSingleGame";
	
	/** Do we delete all existing output files? **/
	private static boolean clearOldOutOfDirectory = true;
	
	/** Value function to pick out the best runs. **/
	private static Evaluator_HandTunedOnState evaluator = new Evaluator_HandTunedOnState();
	
	/** Minimum games to keep per file. **/
	private static int minKeepNumberOfGames = 100;
	
	/** Maximum number of games to keep per file. **/
	private static int maxKeepNumberOfGames = 1000;
	
	/** Score cutoff for saving a game (also limited by min and max). **/
	private static float scoreCutoff = 400;
	
	/** How many actions before falling do we remove when evaluating games. I.E we don't want lots of extremely similar games with slightly different ways of falling. **/
	private static int removeNumberOfEndActions = 5;
	
	/** Skip looking at runs with fewer than this number of actions. **/
	private static int ignoreRunsWithFewerThan = removeNumberOfEndActions * 3;
	
	public static void main(String[] args) {
		
		/** Make the directory for the output. **/
		System.out.println("Setting up output directory...");
		File outDir = new File(sourceDir + "_filtered_dense");
		// if the directory does not exist, create it
		if (!outDir.exists()) {
		    System.out.println("Creating directory for output: " + outDir.getName());
		    boolean result = false;

		    try{
		    	outDir.mkdir();
		        result = true;
		    } catch(SecurityException se){}        
		    if(!result) {    
		        throw new RuntimeException("Failed to create the new output diretory.");
		    }
		}else {
			System.out.println("Folder " + sourceDir + " already exists. Using it.");

			/** Clear existing files if so desired. **/
			if (clearOldOutOfDirectory) {
				System.out.println("Deleting old files (if any) from the specified folder.");
				for(File file: outDir.listFiles()) {
				    if (!file.isDirectory()) {
				    	System.out.println("Deleted: " + file.getName());
				    	file.delete();
				    }
				}
			}
		}
		System.out.println("done");
		
		/** Grab input files. **/
		System.out.println("Identifying input files...");
		File inDir = new File(sourceDir);
		if (!inDir.exists()) throw new RuntimeException("Input directory does not exist here: " + inDir.getName());
		
		double megabyteCount = 0;
		ArrayList<File> inFiles = new ArrayList<File>();
		for(File file: inDir.listFiles()) {	
		    if (!file.isDirectory()) {
		    	String extension = "";
		    	// Get only files with the correct file extension.
		    	int i = file.getName().lastIndexOf('.');
		    	if (i > 0) {
		    	    extension = file.getName().substring(i+1);
		    	}
		    	if (extension.equals(inFileExt)) {
		    		inFiles.add(file);	
		    		megabyteCount += file.length()/1.0e6;
		    	}else {
		    		System.out.println("Ignoring file in input directory: " + file.getName());
		    	}
		    }
		}
		
		System.out.println("Found " + inFiles.size() + " input files with the extension " + inFileExt + ".");
		System.out.println("Total input size: " + Math.round(megabyteCount*10)/10. + " MB.");
		
		System.out.println("done");
		
		/** Import the sparse run data from input files. **/
		System.out.println("Beginning to load sparse run data from input files...");
		
		
		
		SaveableFileIO<SaveableSingleGame> qwopIn = new SaveableFileIO<SaveableSingleGame>();
		ArrayList<ArrayList<SaveableSingleGame>> allLoadedRuns = new ArrayList<ArrayList<SaveableSingleGame>>();
		
		for (int i = 0; i < 1; i++) {
			allLoadedRuns.add(qwopIn.loadObjectsOrdered(inFiles.get(i).getAbsolutePath()));
			System.out.println((int)((i+1.)/(float)inFiles.size() * 100) + "% complete");
		}
		System.out.printf("Done\n");
		
		
		/** Pick the best runs. **/
		System.out.println("Picking the best runs to keep...");
	
		TreeMap<Float,SaveableSingleGame> sortedGamesThisFile = new TreeMap<Float,SaveableSingleGame>();
		TreeMap<Float,SaveableSingleGame> allSortedGames = new TreeMap<Float,SaveableSingleGame>();
		
		
		for (int i = 0; i < allLoadedRuns.size(); i++) {
			
			ArrayList<SaveableSingleGame> currentSet = allLoadedRuns.get(i);
			for (int j = 0; j < currentSet.size(); j++) {
				SaveableSingleGame game = currentSet.get(j);
				if (game.states.length < ignoreRunsWithFewerThan) continue;
				State endState = game.states[game.states.length - removeNumberOfEndActions - 1];
				sortedGamesThisFile.put(evaluator.getValue(endState), game);
			}
			
			// Decide how many to keep
			int savedSoFar = 0;
			float worstScore = Float.MAX_VALUE;
			// Don't save too many or too few. Keep saving while score is good.
			while (savedSoFar < minKeepNumberOfGames || (worstScore > scoreCutoff && savedSoFar < maxKeepNumberOfGames)) {
				Entry<Float, SaveableSingleGame> gameEntry = sortedGamesThisFile.pollLastEntry();
				allSortedGames.put(gameEntry.getKey(),gameEntry.getValue());
				worstScore = gameEntry.getKey(); // We're going in descending order from the end of the map, so the next one will always be the worst we've seen so far.
				savedSoFar++;
			}
			System.out.println("Saved " + savedSoFar + " from this file.");
			
			sortedGamesThisFile.clear();
		}
		
		System.out.println("Picked the best " + allSortedGames.size() + " games from all processed files.");
		System.out.println("done");

		/** Simulate selected runs. **/
		System.out.println("Simulating the selected runs to get dense data and writing to file...");
		
		ProcessDenseData reSim = new ProcessDenseData();
		reSim.process(allSortedGames,100);
		System.out.println("Done.");

	}
	
}
