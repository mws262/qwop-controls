package controllers;

import java.awt.Graphics;

import game.GameLoader;
import game.State;
import main.Action;
import main.IController;

/**
 * A do-nothing placeholder controller.
 * 
 * @author matt
 *
 */
public class Controller_Null implements IController {

	@Override
	public Action policy(State state) {
		return new Action(1, false, false, false, false);
	}

	@Override
	public void draw(Graphics g, GameLoader game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {}

}
