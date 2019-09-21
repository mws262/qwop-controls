package controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameExplorableBase;

import java.util.Objects;

/**
 * A do-nothing placeholder controller. It always concludes that no keys should be pressed.
 *
 * @author matt
 */
public class Controller_Constant<C extends Command<?>, S extends IState> implements IController<C, S> {

    @JsonProperty("constantAction")
    public final Action<C> constantAction;

    public Controller_Constant(@JsonProperty("constantAction") Action<C> constantAction) {
        this.constantAction = constantAction;
    }

    @Override
    public Action<C> policy(NodeGameExplorableBase<?, C, S> state) {
        return constantAction;
    }

    @Override
    public Action<C> policy(NodeGameExplorableBase<?, C, S> state, IGameSerializable<C, S> game) {
        return policy(state);
    }

    @Override
    public IController<C, S> getCopy() {
        return new Controller_Constant<>(constantAction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Controller_Constant<?, ?> that = (Controller_Constant<?, ?>) o;
        return Objects.equals(constantAction, that.constantAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constantAction);
    }

    @Override
    public void close() {}
}
