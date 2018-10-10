package controllers;

import java.awt.Graphics;
import java.io.IOException;

import controllers.Controller_NearestNeighborApprox.DecisionHolder;
import game.GameLoader;
import game.State;
import main.Action;
import main.Utility;
import server.Client;

public class Controller_AskServer extends Client implements IController {

	private final IController subController;
	private DecisionHolder currentDecision;

	public Controller_AskServer(IController controller) {
		subController = controller;		
		try {
			initialize();
			sendObject(controller);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Sent controller template.");
	}

	@Override
	public Action policy(State state) {

		try {
			sendObject(state);
			Utility.tic();
			System.out.println("Trying to receive decision.");
			Action nextDecision = (Action)receiveObject();
			System.out.println("Received decision.");

			Utility.toc();
			return nextDecision;
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void draw(Graphics g, GameLoader game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {
		subController.draw(g, game, runnerScaling, xOffsetPixels, yOffsetPixels);
//		if (currentDecision != null) {
//			float offset = currentDecision.chosenTrajectory.states.get(currentDecision.chosenIdx).state.body.x;
//			for (StateHolder sh : currentDecision.chosenTrajectory.states) {
//				game.drawExtraRunner((Graphics2D)g, sh.state, "", runnerScaling, xOffsetPixels - (int)(runnerScaling*offset), yOffsetPixels, PanelRunner.ghostGray, PanelRunner.normalStroke);
//			}
//		}
	}
}
