package main;

import java.util.List;

import main.FSM_Game.Status;

public class Negotiator_Updated implements INegotiateGame {

	List<TreeWorker> workers;
	
	/** All visuals and external interaction. **/
	IUserInterface ui;
	
	public Negotiator_Updated(List<TreeWorker> workers, IUserInterface ui) {
		this.workers = workers;
		this.ui = ui;
	}
	
	@Override
	public void statusChange_Game(Status status) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportQWOPKeys(boolean Q, boolean W, boolean O, boolean P) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportEndOfRealTimeSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportGameStep(Action action) {
		// TODO Auto-generated method stub

	}

}
