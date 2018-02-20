package main;

import java.text.SimpleDateFormat;
import java.util.Date;

public interface IDataSaver {
	
	/** Report initial state. **/
	public void reportGameInitialization(State initialState);
	
	/** Report intermediate nodes as they are being run. Useful for dense
	 *  saving of data for TFRecords or other.
	 * @param stepNode
	 */
	public void reportTimestep(Action action, State state);
	
	/** Get the final game state for this run. This will be a failed state.
	 *  It is reported after game failure AND after final node creation.
	 *  Hence it should be called from the EVALUATE_GAME transition of tree
	 *  and not from the FAILED transition for game.
	 * @param endNode
	 */
	public void reportGameEnding(Node endNode);
	
	/** Set the number of games to collect in between saves. **/
	public void setSaveInterval(int numGames);
	
	/** Generate a filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[class name]**/
	static String generateFileName(String prefix, String className) {
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." +  className + "'") ;
		String name = dateFormat.format(date);
		System.out.println("Generated file: " + name);

		return name;
	}
}
