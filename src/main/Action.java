package main;

import java.io.Serializable;

/**
 * Contains the keypresses and durations for a single action. Works like an uneditable queue.
 * Call poll() to get the keystrokes at each timestep execution. Call hasNext() to make sure 
 * there are timesteps left in this action. Call reset() to restore the duration of the action
 * back to original.
 * 
 * @author Matt
 *
 */
public class Action implements Serializable{

	private static final long serialVersionUID = 2L;

	/** Total number of box2d timesteps that this key combination should be held. **/
	private final int timestepsTotal;
	
	/** Number of timesteps left to hold this command. **/
	private int timestepsRemaining;
	
	/** Which of the QWOP keys are pressed? **/
	private final boolean[] keysPressed;
	
	private boolean isExecutableCopy = false;
	
	/** Create an action containing the time to hold and the key combination. **/
	public Action(int totalTimestepsToHold, boolean[] keysPressed){
		if (keysPressed.length != 4) throw new IllegalArgumentException("A QWOP action should have booleans for exactly 4 keys. Tried to create one with a boolean array of size: " + keysPressed.length);
		this.timestepsTotal = totalTimestepsToHold;
		this.keysPressed = keysPressed;
		timestepsRemaining = timestepsTotal;
	}
	
	/** Create an action containing the time to hold and the key combination. **/
	public Action(int totalTimestepsToHold, boolean Q, boolean W, boolean O, boolean P){
		this(totalTimestepsToHold, new boolean[]{Q, W, O, P});
	}
	
	/** Return the keys for this action and deincrement the timestepsRemaining. **/
	public boolean[] poll(){
		if (!isExecutableCopy) throw new RuntimeException("Trying to execute the base version of the Action. Due to multi-threading, this REALLY screws with the counters in the action. Call getCopy for the version you should use.");
		if (timestepsRemaining <=0) throw new IndexOutOfBoundsException("Tried to poll an action which was already completed. Call hasNext() to check before calling poll().");
		timestepsRemaining--;
		
		return keysPressed;
	}
	
	/** Return the keys pressed in this action without incrementing the timesteps. **/
	public boolean[] peek(){
		return keysPressed;
	}
	
	/** Check whether this action is finished (i.e. internal step counter hit zero). **/
	public boolean hasNext(){
		return timestepsRemaining > 0;
	}
	
	/** Reset the number of timesteps in this action remaining. Do this before repeating an action. **/
	public void reset(){
		timestepsRemaining = timestepsTotal;
	}
	
	/** Get the number of timesteps left to hold this key combination. **/
	public int getTimestepsRemaining(){ return timestepsRemaining; }
	
	/** Get the total number of timesteps for this action. **/
	public int getTimestepsTotal(){ return timestepsTotal; }
	
	/** Check if this action is equal to another in regards to keypresses and durations. 
	 * Completely overrides default equals, so when doing ArrayList checks, this will
	 * be the only way to judge.
	 **/
	@Override
	public boolean equals(Object other){
	    if(!(other instanceof Action)) {
	        return false;
	    }
	    Action otherAction = (Action)other;
	    
		boolean equal = true;
		
		// Negate equality if any of the QWOP keys don't match.
		for (int i = 0; i < keysPressed.length; i++){
			if (keysPressed[i] != otherAction.peek()[i]){
				equal = false;
				break;
			}
		}
		
		// Negate if we haven't already and they have different durations.
		if (equal && timestepsTotal != otherAction.getTimestepsTotal()) equal = false;
		
		return equal;
	}
	
	/** Return a string with the current action keys, total time to hold, and time remaining. **/
	public String toString(){
		String reportString = " Keys pressed: ";
		reportString += keysPressed[0] ? "Q" : "";
		reportString += keysPressed[1] ? "W" : "";
		reportString += keysPressed[2] ? "O" : "";
		reportString += keysPressed[3] ? "P" : "";
		
		reportString += "; Timesteps elapsed/total: " + timestepsRemaining + "/" + timestepsTotal;
		
		return reportString;
	}
	
	/** Just returns the total delay time, assuming the user remembers the key sequence stuff. **/
	public String toStringLite() {
		return String.valueOf(timestepsTotal);
	}
	
	/** Get a copy of this action. This avoid multi-threading issues. **/
	public synchronized Action getCopy() {
		Action copiedAction = new Action(timestepsTotal, keysPressed);
		copiedAction.isExecutableCopy = true;
		return copiedAction;
	}
}
