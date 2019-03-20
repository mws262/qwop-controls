package game;

import java.awt.*;

/**
 * All versions of the game should do these things. Primarily so that the single thread and multithreaded versions
 * can be swapped easily, but other implementations could be added with different game rules also.
 *
 * @author matt
 */
public interface IGame {

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

    /**
     * Get the runner's state.
     *
     * @return {@link State} of the runner at the current timestep.
     */
    State getCurrentState();


    /**
     * Get whether the game is considered to be in a failed state.
     * @return Whether the game is failed (true is failed).
     */
    boolean getFailureStatus();

    long getTimestepsSimulatedThisGame();

    void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels);

    void setState(State st);

    void applyBodyImpulse(float v, float v1);
}
