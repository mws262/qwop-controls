package game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.action.Command;
import game.qwop.GameQWOP;
import game.qwop.GameQWOPCaching;
import game.state.IState;

import java.io.Serializable;

/**
 * An game whose full state can be serialized into a byte array and restored.
 *
 * @author matt
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GameQWOP.class, name = "qwop"),
        @JsonSubTypes.Type(value = GameQWOPCaching.class, name = "qwop_caching"),
})
public interface IGameSerializable<C extends Command<?>, S extends IState> extends IGameInternal<C, S>, Serializable {

    @JsonIgnore
    byte[] getSerializedState();

    IGameSerializable<C, S> restoreSerializedState(byte[] fullState);

    @JsonIgnore
    IGameSerializable<C, S> getCopy();

    // Probably not be used by non-qwop games.
    default void setPhysicsIterations(int iterations) {}
}
