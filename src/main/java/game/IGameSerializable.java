package game;

import game.action.Command;
import game.state.IState;

import java.io.Serializable;

/**
 * An game whose full state can be serialized into a byte array and restored.
 *
 * @author matt
 */
public interface IGameSerializable<C extends Command<?>, S extends IState> extends IGameInternal<C, S>, Serializable {
    byte[] getSerializedState();

    IGameSerializable<C, S> restoreSerializedState(byte[] fullState);

    IGameSerializable<C, S> getCopy();
}
