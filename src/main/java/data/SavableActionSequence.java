package data;

import java.io.Serializable;

import game.action.Action;

public class SavableActionSequence implements Serializable {

    private static final long serialVersionUID = 1L;

    private Action[] actions;

    public SavableActionSequence(Action[] actions) {
        this.actions = actions;
    }

    public Action[] getActions() {
        return actions;
    }

}
