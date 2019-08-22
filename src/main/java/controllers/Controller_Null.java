package controllers;

import game.IGameSerializable;
import game.action.Action;
import game.action.Command;
import game.action.CommandQWOP;
import tree.node.NodeQWOPExplorableBase;

/**
 * A do-nothing placeholder controller. It always concludes that no keys should be pressed.
 *
 * @author matt
 */
public class Controller_Null<C extends Command<?>> implements IController<C> {

    @Override
    public Action<C> policy(NodeQWOPExplorableBase<?, C> state) {
        return null;
    } // TODO fix.

    @Override
    public Action<C> policy(NodeQWOPExplorableBase<?, C> state, IGameSerializable<C> game) {
        return policy(state);
    }

    @Override
    public IController<C> getCopy() {
        return new Controller_Null<>();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Controller_Null;
    }

    @Override
    public int hashCode() {
        return Controller_Null.class.hashCode();
    }

    @Override
    public void close() {}
}
