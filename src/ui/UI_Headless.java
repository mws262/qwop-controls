package ui;

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
	public void addRootNode(Node node) {}

	@Override
	public void clearRootNodes() {}

}
