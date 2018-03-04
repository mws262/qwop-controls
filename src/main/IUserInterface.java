package main;

import javax.swing.event.ChangeEvent;

public interface IUserInterface extends Runnable{

	/** Main graphics loop. **/
	void run();

	/** Stop the FSM. **/
	void kill();

	/** Pick a node for the UI to highlight and potentially display. **/
	void selectNode(Node selected);

	void stateChanged(ChangeEvent e);

	void setNegotiator(INegotiateGame negotiator);
	
	void setLiveGameToView(QWOPGame game);
	
	void clearLiveGameToView();
	
	void addRootNode(Node node);
	
	boolean isSnapshotPaneActive();
	
	boolean isRunnerPaneActive();

}