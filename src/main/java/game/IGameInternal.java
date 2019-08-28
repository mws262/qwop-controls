package game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import game.action.Command;
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
public interface IGameInternal<C extends Command<?>> extends IGameExternal<C> {

    /** Reset the runner to its starting state. **/
    void resetGame();

    void step(C command);

    void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels);

    void setState(IState st);

    @JsonIgnore
    IGameInternal<C> getCopy();
}
