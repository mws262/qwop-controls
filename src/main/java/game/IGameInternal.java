package game;

import java.awt.*;

/**
 * All versions of the game should do these things. Primarily so that the single thread and multithreaded versions
 * can be swapped easily, but other implementations could be added with different game rules also.
 *
 * This extends {@link IGameCommandTarget}. A command target is required to receive keypress commands, but does not
 * need to implement the game itself. An implementation of {@link IGame} must do both.
 *
 * This also extends {@link IGameStateSource}. A state source must provide game states but is not required to take
 * inputs. A full {@link IGame} interface must implement both.
 *
 * @author matt
 */
public interface IGame extends IGameCommandTarget, IGameStateSource{

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

    void setState(State st);
}
