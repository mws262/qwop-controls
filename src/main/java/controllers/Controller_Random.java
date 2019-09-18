package controllers;

import game.IGameSerializable;
import game.action.*;
import game.state.IState;
import tree.node.NodeGameExplorableBase;

public class Controller_Random<C extends Command<?>, S extends IState> implements IController<C, S> {
    @Override
    public Action<C> policy(NodeGameExplorableBase<?, C, S> state) {
        return state.getAllPossibleChildActions().getRandom();
    }

    @Override
    public Action<C> policy(NodeGameExplorableBase<?, C, S> state, IGameSerializable<C, S> game) {
        return policy(state);
    }

    @Override
    public IController<C, S> getCopy() {
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
