package ui;

import javax.swing.event.ChangeEvent;

import main.INegotiateGame;
import main.IUserInterface;
import main.Node;
import main.QWOPGame;

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
	public void setLiveGameToView(QWOPGame game) {}

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
