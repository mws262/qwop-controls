package data;

import game.action.Action;
import game.action.Command;

import java.io.Serializable;

public class SavableActionSequence<C extends Command<?>> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Action<C>[] actions;

    public SavableActionSequence(Action<C>[] actions) {
        this.actions = actions;
    }

    public Action<C>[] getActions() {
        return actions;
    }

}
