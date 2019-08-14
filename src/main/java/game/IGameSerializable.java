package game;

import java.io.Serializable;

/**
 * An game whose full state can be serialized into a byte array and restored.
 *
 * @author matt
 */
public interface IGameSerializable extends IGameInternal, Serializable {
    byte[] getSerializedState();
    IGameInternal restoreSerializedState(byte[] fullState);
}
