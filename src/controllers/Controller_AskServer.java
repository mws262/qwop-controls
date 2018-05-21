package controllers;

import java.awt.Graphics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import game.GameLoader;
import game.State;
import main.Action;
import main.IController;
import server.Client;

public class Controller_AskServer extends Client implements IController {
	
	private final IController subController;
	
	public Controller_AskServer(IController controller) {
		subController = controller;		
		try {
			initialize();
			sendObject(controller);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("Sent controller template.");
	}

	@Override
	public Action policy(State state) {
		
		try {
			sendObject(state);
			return (Action)receiveObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void draw(Graphics g, GameLoader game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {
		subController.draw(g, game, runnerScaling, xOffsetPixels, yOffsetPixels);
	}
}
