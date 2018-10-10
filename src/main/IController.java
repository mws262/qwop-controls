package main;

import java.awt.Graphics;

import game.GameLoader;
import game.State;

public interface IController {

    /**
     * Controller maps a current state to an action to take.
     **/
    Action policy(State state);

    /**
     * Optionally, if we want the controller to draw anything to see what it's doing.
     *
     * @param yOffsetPixels
     * @param xOffsetPixels
     * @param runnerScaling
     **/
    void draw(Graphics g, GameLoader game, float runnerScaling, int xOffsetPixels, int yOffsetPixels);
}
