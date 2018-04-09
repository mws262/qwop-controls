package savers;

import java.util.ArrayList;
import java.util.List;

import data.SaveableFileIO;
import data.SaveableSingleGame;
import game.GameLoader;
import game.State;
import main.Action;
import main.IDataSaver;
import main.Node;

/**
 * Save save any runs specified by the TreeStage at the end of it.
 * This data is saved sparsely.
 * 
 * @author matt
 *
 */
public class DataSaver_StageSelected implements IDataSaver{

	/** File prefix. Goes in front of date. **/
	public String filePrefix = "qwop_stage_sparse_java";
	
	/** Do not include dot before. **/
	public String fileExtension = "SaveableSingleGame";
	
	/** If this string is not empty, use this as the filename instead. **/
	public String overrideFilename = "";
	
	/** Handles class serialization and writing to file. **/
	private SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
	
	/** Buffered games awaiting file write. **/
	private ArrayList<SaveableSingleGame> saveBuffer = new ArrayList<SaveableSingleGame>();
	
	/** File save location. **/
	private String fileLocation = "./";
	
	@Override
	public void reportGameInitialization(State initialState) {}

	@Override
	public void reportTimestep(Action action, GameLoader game) {}

	@Override
	public void reportGameEnding(Node endNode) {}

	@Override
	public void reportStageEnding(Node rootNode, List<Node> targetNodes) {
		for (Node tar : targetNodes) {
			saveBuffer.add(new SaveableSingleGame(tar));
		}
		if (overrideFilename.isEmpty()) {
			fileIO.storeObjectsOrdered(saveBuffer, fileLocation + IDataSaver.generateFileName(filePrefix, fileExtension), false);
		}else {
			fileIO.storeObjectsOrdered(saveBuffer, fileLocation + overrideFilename + "." + fileExtension, false);

		}
		saveBuffer.clear();
		System.out.println("Saved " + targetNodes.size() + " runs sparsely to file at the end of the stage.");
	}

	@Override
	public void setSaveInterval(int numGames) {} // Not applicable for once-per-stage saving.

	@Override
	public void setSavePath(String fileLoc) {
		this.fileLocation = fileLoc;
	}

	@Override
	public IDataSaver clone() {
		DataSaver_DenseTFRecord newSaver = new DataSaver_DenseTFRecord();
		newSaver.setSavePath(fileLocation);
		return newSaver;
	}

}
