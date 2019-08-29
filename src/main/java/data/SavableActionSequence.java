package data;

import game.action.Action;
import game.action.Command;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SavableActionSequence<C extends Command<?>> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Action<C>> actions;

    public SavableActionSequence(List<Action<C>> actions) {
        new ArrayList<>(actions);
    }

    public List<Action<C>> getActions() {
        return actions;
    }
}
