/**
 * 
 */
package savers;

import java.util.ArrayList;

import data.SaveableDenseData;
import data.SaveableFileIO;
import main.IDataSaver;
import main.Node;

/**
 * Saves data at every timestep. Old saver which serializes java classes.
 * Useful for manipulating data in java, not good for tensorflow stuff.
 * 
 * @author matt
 *
 */
public class DataSaver_DenseJava extends DataSaver_Dense{
	
	/** File prefix. Goes in front of date. **/
	public String filePrefix = "qwop_dense_java";
	
	/** Do not include dot before. **/
	public String fileExtension = "SaveableDenseData";
	
	/** Games since last save. **/
	private int saveCounter = 0;
	
	/** Handles class serialization and writing to file. **/
	private SaveableFileIO<SaveableDenseData> fileIO = new SaveableFileIO<SaveableDenseData>();
	
	/** Buffered games waiting to be written to file. **/
	private ArrayList<SaveableDenseData> saveBuffer = new ArrayList<SaveableDenseData>();
	
	@Override
	public void reportGameEnding(Node endNode) {
		// Collect all the states and actions into a data object.
		saveBuffer.add(new SaveableDenseData(stateBuffer,actionBuffer));
		saveCounter++;

		if (saveInterval == saveCounter) {
			fileIO.storeObjectsOrdered(saveBuffer, fileLocation + IDataSaver.generateFileName(filePrefix, fileExtension), false);
			saveCounter = 0;
		}
		// Clear out for the next run to begin.
		stateBuffer.clear();
		actionBuffer.clear();
		
	}

}
