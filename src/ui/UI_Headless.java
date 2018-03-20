package ui;

import javax.swing.event.ChangeEvent;

import game.GameLoader;
import main.INegotiateGame;
import main.IUserInterface;
import main.Node;

public class UI_Headless implements IUserInterface {

	@Override
	public void run() {
		System.out.println("Headless mode: Running without user interface.");
	}

	@Override
	public void kill() {}

	@Override
	public void selectNode(Node selected) {}

	@Override
	public void stateChanged(ChangeEvent e) {}

	@Override
	public void setLiveGameToView(GameLoader game) {}

	@Override
	public void clearLiveGameToView() {}

	@Override
	public void addRootNode(Node node) {}

	@Override
	public boolean isSnapshotPaneActive() {
		return false;
	}

	@Override
	public boolean isRunnerPaneActive() {
		return false;
	}

	@Override
	public void setNegotiator(INegotiateGame negotiator) {}

}
