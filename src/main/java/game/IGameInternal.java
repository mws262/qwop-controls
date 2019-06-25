package game;

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
public interface IGameInternal extends IGameExternal {

    /** Reset the runner to its starting state. **/
    void makeNewWorld();

    /**
     * Advance the game by one timestep with the given commands.
     * @param q Whether q is pressed on the keyboard (true is pressed down).
     * @param w Whether w is pressed on the keyboard (true is pressed down).
     * @param o Whether o is pressed on the keyboard (true is pressed down).
     * @param p Whether p is pressed on the keyboard (true is pressed down).
     */
    void step(boolean q, boolean w, boolean o, boolean p);

    /**
     * Advance the game by one timestep with the given commands.
     * @param commands Array of keypress commands to use while advancing the physics by one timestep.
     */
    void step(boolean[] commands);

    void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels);

    void setState(IState st);
}
