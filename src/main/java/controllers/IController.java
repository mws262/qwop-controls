package controllers;

import actions.Action;
import game.GameUnified;
import game.IState;

import java.awt.*;

/**
 * Interface for defining general QWOP controllers. Follows the typical state to action mapping. If an implementation
 * of an IController wants to use a history of states or actions, it should store these locally itself.
 *
 * @author matt
 */
public interface IController {

    /**
     * Controller maps a current state to an action to take.
     *
     * @param state Current state.
     * @return An action to take.
     */
    Action policy(IState state);

    /**
     * Optionally, if we want the controller to draw anything to see what it's doing. Defaults to doing nothing if
     * not overridden.
     *
     * @param yOffsetPixels Vertical pixel offset from scaled world coordinates.
     * @param xOffsetPixels Horizontal pixel offset from scaled world coordinates.
     * @param runnerScaling Scaling from world coordinates to window pixel coordinates.
     **/
    default void draw(Graphics g, GameUnified game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {}
}
