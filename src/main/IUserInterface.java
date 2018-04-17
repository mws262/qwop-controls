package main;

public interface IUserInterface extends Runnable{

	/** Main graphics loop. **/
	@Override
	void run();

	/** Stop the FSM. **/
	void kill();

	/** Pick a node for the UI to highlight and potentially display. **/
	void selectNode(Node selected);

	void addRootNode(Node node);
	
	void clearRootNodes();
	
	public interface TabbedPaneActivator {
		public void activateTab();
		public void deactivateTab();
		public boolean isActive();
		public void update(Node node);
	}
}