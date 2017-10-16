package main;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ActionGenerator_FixedSequence implements IActionGenerator{
	
	/** These are the possible actions which are generated in a cycle.**/
	private Action[][] repeatedActions;
	
	/** These actions are exceptions which will override the repeated actions. Key is the 
	 * tree depth this applies to. Value is the set of Actions. The keySequence booleans
	 * are still obeyed. Usually exceptions are at the beginning of a sequence.
	 */
	private Map<Integer,Action[]> exceptionActions;
	
	/** Sequence of keypresses in a single cycle. **/
	private boolean[][] keySequence;
	
	/** How many actions are in one repeated cycle. **/
	private final int cycleLength;
	
	/** 
	 * This action generator will produce the nil-WO-nil-QP sequence.
	 * actionRepeats will be chosen in repeating order like 1234,1234,1234, or 123,123,123, etc.
	 * Note that this starts from tree root EVEN if there are action exceptions.
	 * However, action exceptions for specified tree depths will be chosen over the repeating actions
	 * if they are assigned.
	 * 
	 * @param actionExceptions
	 * @param actionRepeats
	 */
	public ActionGenerator_FixedSequence(boolean[][] keySequence, Integer[][] actionRepeats, Map<Integer,Integer[]> actionExceptions) {
		if (keySequence[0].length != 4) throw new IllegalArgumentException("Key sequences must contain elements of 4's corresponding to the QWOP keys.");
		if (keySequence.length != actionRepeats.length) throw new IllegalArgumentException("There must be exactly 1 key combination per action in the repeated set.");
		
		this.keySequence = keySequence;
		cycleLength = keySequence.length; // Usually 4 actions per cycle.
				
		// Make new actions for repeated cycle.
		repeatedActions = new Action[keySequence.length][];
		for (int i = 0; i < keySequence.length; i++){ // For each key combination (usually 4 of them)
			repeatedActions[i] = new Action[actionRepeats[i].length];
			for (int j = 0; j < actionRepeats[i].length; j++){ // For each possible action for that key combination
				repeatedActions[i][j] = new Action(actionRepeats[i][j], keySequence[i]);
			}
		}
		
		// Make new actions for the exceptions.
		exceptionActions = new HashMap<Integer,Action[]>();
		Set<Integer> exceptionDepths = actionExceptions.keySet();
		for (Integer depth : exceptionDepths){
			Action[] actionSet = new Action[actionExceptions.get(depth).length];
			for (int i = 0; i < actionSet.length; i++){
				int phase = depth % cycleLength; // Where is this in the repeated sequence?
				actionSet[i] = new Action(actionExceptions.get(depth)[i], keySequence[phase]);
			}
			exceptionActions.put(depth, actionSet);
		}
	}
	
	@Override
	public Action[] getPotentialChildActionSet(Node parentNode) {
		int actionDepth = parentNode.treeDepth;
		
		// Check if this is an exception case.
		if (exceptionActions != null && exceptionActions.containsKey(actionDepth)) return exceptionActions.get(actionDepth);

		// Otherwise, pick based on cycle.
		return repeatedActions[actionDepth % keySequence.length];
	}
}
