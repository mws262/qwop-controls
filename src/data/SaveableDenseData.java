package data;

import java.io.Serializable;
import java.util.ArrayList;

import main.State;
import main.Action;
/**
 * Holds all the states and actions on a TIMESTEP level for an entire game.
 * Note that a state corresponds to the action that is being held during it, not
 * the action which leads to it like in other parts of the code. Also, there may
 * be 1 more state than action. this is because we arrive at the final state
 * without needing to select an action once we get there.
 * 
 * @author matt
 *
 */
public class SaveableDenseData implements Serializable{

	private static final long serialVersionUID = 1L;

	private final State[] state;
	
	private final Action[] action;
	
	public SaveableDenseData(State[] state, Action[] action) {
		if (state.length != action.length && state.length - action.length != 1) throw new RuntimeException("State and action data must be of the same size, or state must have 1 more element than action. State size: " + state.length + ". Action size: " + action.length);
		this.state = state;
		this.action = action;
	}
	
	public SaveableDenseData(ArrayList<State> state, ArrayList<Action> action) {
		if (state.size() != action.size() && state.size() - action.size() != 1) throw new RuntimeException("State and action data must be of the same size, or state must have 1 more element than action. State size: " + state.size() + ". Action size: " + action.size());
		
		this.state = new State[state.size()];
		this.action = new Action[action.size()];
		
		for (int i = 0; i < state.size(); i++) {
			this.state[i] = state.get(i);
		}
		
		for (int i = 0; i < action.size(); i++) {
			this.action[i] = action.get(i);
		}
	}

	public State[] getState() {
		return state;
	}

	public Action[] getAction() {
		return action;
	}
}
