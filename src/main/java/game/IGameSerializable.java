package game;

import game.action.Command;

import java.io.Serializable;

/**
 * An game whose full state can be serialized into a byte array and restored.
 *
 * @author matt
 */
public interface IGameSerializable<C extends Command<?>> extends IGameInternal<C>, Serializable {
    byte[] getSerializedState();
    IGameInternal<C> restoreSerializedState(byte[] fullState);
}
