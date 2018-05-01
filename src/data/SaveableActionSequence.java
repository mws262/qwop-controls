package data;

import java.io.Serializable;

import main.Action;

public class SaveableActionSequence implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Action[] actions;
	
	public SaveableActionSequence(Action[] actions) {
		this.actions = actions;
	}
	
	public Action[] getActions() {
		return actions;
	}

}
