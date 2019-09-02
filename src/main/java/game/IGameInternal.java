package game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.action.Command;
import game.cartpole.CartPole;
import game.qwop.GameQWOP;
import game.qwop.GameQWOPCaching;
import game.state.IState;

import java.awt.*;

/**
 * All versions of the game should do these things. Primarily so that the single thread and multithreaded versions
 * can be swapped easily, but other implementations could be added with different game rules also.
 *
 * An {@link IGameInternal} must be able to receive commands and provide states, but additionally, it must be able to
 * be explicitly stepped forward in time, and its state must be externally changeable.
 *
 * @author matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GameQWOP.class, name = "qwop"),
        @JsonSubTypes.Type(value = GameQWOPCaching.class, name = "qwop_caching"),
        @JsonSubTypes.Type(value = CartPole.class, name = "cartpole"),
})
public interface IGameInternal<C extends Command<?>, S extends IState> extends IGameExternal<C, S> {

    /** Reset the runner to its starting state. **/
    void resetGame();

    void step(C command);

    void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels);

    void setState(S st);

    @JsonIgnore
    IGameInternal<C, S> getCopy();
}
