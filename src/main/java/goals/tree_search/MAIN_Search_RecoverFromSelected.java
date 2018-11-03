package goals.tree_search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import actions.Action;
import data.SavableActionSequence;
import data.SparseDataToDenseTFRecord;
import org.apache.commons.lang3.ArrayUtils;

import data.SavableFileIO;
import game.GameLoader;
import samplers.Sampler_UCB;
import tree.Node;
import tree.Utility;

/**
 * Does the full search in 4 stages.
 * 1. get to steady running depth.
 * 2. expand some node with a bunch of deviations from steady.
 * 3. find recoveries for each deviation.
 * 4. convert sparsely saved data to dense and save to file.
 *
 * @author matt
 */
public class MAIN_Search_RecoverFromSelected extends MAIN_Search_Template {

    public MAIN_Search_RecoverFromSelected() {
        super(new File("src/main/resources/config/" + "search.config_selected"));
    }

    public static void main(String[] args) {
        MAIN_Search_RecoverFromSelected manager = new MAIN_Search_RecoverFromSelected();
        manager.doGames();
    }

    public void doGames() {
        // Load all parameters specific to this search.
        Sampler_UCB.explorationMultiplier = Float.valueOf(properties.getProperty("UCBExplorationMultiplier", "1"));

        // Stage 1 - recovering
        int getBackToSteadyDepth = Integer.valueOf(properties.getProperty("getBackToSteadyDepth", "18")); // Stage
		// terminates after any part of the tree reaches this depth.
        float maxWorkerFraction1 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers1", "1")); // Fraction
		// of the maximum workers used for this stage.
        int bailAfterXGames1 = Integer.valueOf(properties.getProperty("bailAfterXGames1", "1000000")); // Stop stage
		// 1 after this many games even if we don't reach the goal depth.
        String fileSuffix1 = properties.getProperty("fileSuffix1", "");

        String filename1 = "controller_recovery_" + fileSuffix1;

        // Stage 2 saving
        int trimStartBy;
        int trimEndBy = Integer.valueOf(properties.getProperty("trimEndBy", "4"));

        // For extending and fixing saved games from MAIN_Controlled
        // See which files need to be covered.
        SavableFileIO<SavableActionSequence> actionSequenceLoader = new SavableFileIO<>();
        File[] actionFiles = getSaveLocation().listFiles();
        Arrays.sort(Objects.requireNonNull(actionFiles));
        ArrayUtils.reverse(actionFiles);

        GameLoader game = new GameLoader();

        for (File f : actionFiles) {
            if (f.getName().contains("SavableActionSequence") && f.getName().contains("6_08")) {
                appendSummaryLog("Processing file: " + f.getName());
                List<SavableActionSequence> actionSeq = new ArrayList<>();
                actionSequenceLoader.loadObjectsToCollection(f, actionSeq);

                List<Action[]> acts = actionSeq.stream().map(SavableActionSequence::getActions).collect(Collectors.toList());

                trimStartBy = acts.get(0).length;
                assignAllowableActions(trimStartBy); // Assign the broader recovery action sets to the depth at the
				// end of the action list.

                // Recreate the tree section.
                Node root = new Node();
                Node.makeNodesFromActionSequences(acts, root, game);

                // Put it on the UI.
                Node.pointsToDraw.clear();
                ui.clearRootNodes();
                ui.addRootNode(root);

                List<Node> leafList = new ArrayList<>();
                root.getLeaves(leafList);

                // Expand the deviated spots and find recoveries.
                appendSummaryLog("Starting leaf expansion.");
                Node previousLeaf = null;

                // Should only be 1 element in leafList now. Keeping the loop for the future however.
                for (Node leaf : leafList) {
                    String name = filename1 + Utility.getTimestamp();
                    doBasicMaxDepthStage(leaf, name, getBackToSteadyDepth, maxWorkerFraction1, bailAfterXGames1);

                    // Save results
                    File[] files = getSaveLocation().listFiles();
                    for (File dfiles : Objects.requireNonNull(files)) {
                        if (dfiles.toString().toLowerCase().contains(name) && !dfiles.toString().toLowerCase().contains("unsuccessful")) {
                            SparseDataToDenseTFRecord converter =
									new SparseDataToDenseTFRecord(getSaveLocation().getAbsolutePath() + "/");
                            converter.trimFirst = trimStartBy;
                            converter.trimLast = trimEndBy;
                            List<File> toConvert = new ArrayList<>();
                            toConvert.add(dfiles);
                            converter.convert(toConvert, false);
                            appendSummaryLog("Converted to TFRecord.");
                            break;
                        }
                    }
                    // Turn off drawing for this one.
                    leaf.turnOffBranchDisplay();
                    //FIXME: Fix line below since some methods have become private.
//                    leaf.parent.children.remove(leaf);
                    if (previousLeaf != null) {
                        previousLeaf.destroyAllNodesBelowAndCheckExplored();
                    }
                    previousLeaf = leaf;

                    appendSummaryLog("Expanded leaf.");
                }
            }
            appendSummaryLog("Stage done.");
        }
    }
}
