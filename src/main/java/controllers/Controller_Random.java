package controllers;

import game.IGameSerializable;
import game.action.*;
import tree.node.NodeGameExplorableBase;

public class Controller_Random<C extends Command<?>> implements IController<C> {
    @Override
    public Action<C> policy(NodeGameExplorableBase<?, C> state) {
        return state.getAllPossibleChildActions().getRandom();
    }

    @Override
    public Action<C> policy(NodeGameExplorableBase<?, C> state, IGameSerializable<C> game) {
        return policy(state);
    }

    @Override
    public IController<C> getCopy() {
        return new Controller_Random<>();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Controller_Random;
    }

    @Override
    public int hashCode() {
        return Controller_Random.class.hashCode();
    }

    @Override
    public void close() {}
}
