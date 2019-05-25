package game;

import java.io.Serializable;

public interface IGameSerializable extends IGameInternal, Serializable {
    byte[] getSerializedState();
    IGameInternal restoreSerializedState(byte[] fullState);
}
