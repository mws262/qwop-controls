package controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
import game.action.Action;
import game.action.Command;
import tree.node.NodeQWOPExplorableBase;

import java.util.Objects;

/**
 * A do-nothing placeholder controller. It always concludes that no keys should be pressed.
 *
 * @author matt
 */
public class Controller_Null<C extends Command<?>> implements IController<C> {

    @JsonProperty("nullCommand")
    public final Action<C> nullAction;

    public Controller_Null(@JsonProperty("nullCommand") Action<C> nullAction) {
        this.nullAction = nullAction;
    }

    @Override
    public Action<C> policy(NodeQWOPExplorableBase<?, C> state) {
        return nullAction;
    }

    @Override
    public Action<C> policy(NodeQWOPExplorableBase<?, C> state, IGameSerializable<C> game) {
        return policy(state);
    }

    @Override
    public IController<C> getCopy() {
        return new Controller_Null<>(nullAction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Controller_Null<?> that = (Controller_Null<?>) o;
        return Objects.equals(nullAction, that.nullAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nullAction);
    }

    @Override
    public void close() {}
}
