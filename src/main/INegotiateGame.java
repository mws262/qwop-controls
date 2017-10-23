package main;

/**
 * Things that FSM_Game needs from a negotiator to function.
 * @author Matt
 *
 */
public interface INegotiateGame {

	/**** All game status changes reported here. ****/
	void statusChange_Game(FSM_Game.Status status);
	
	/** Key changes during real time simulations reported here. **/
	void reportQWOPKeys(boolean Q, boolean W, boolean O, boolean P);
	
	/** Completed realtime simulations reported here. **/
	void reportEndOfRealTimeSim();

	/** Every game timestep gets reported here. **/
	void reportGameStep(Action action);

}