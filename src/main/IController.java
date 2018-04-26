package main;

import game.State;

public interface IController {

	/** Controller maps a current state to an action to take. **/
	public Action policy(State state);

}
