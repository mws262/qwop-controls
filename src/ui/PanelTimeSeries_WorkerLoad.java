package ui;

import java.util.ArrayList;
import java.util.List;

import main.Node;
import main.PanelTimeSeries;
import main.TreeStage;
import main.TreeWorker;

public class PanelTimeSeries_WorkerLoad extends PanelTimeSeries implements Runnable {

	private static final long serialVersionUID = 1L;

	private final int numPlots;
	
	private List<TreeWorker> workerList = new ArrayList<TreeWorker>();

	public PanelTimeSeries_WorkerLoad(int maxWorkers) {
		super(maxWorkers);
		numPlots = maxWorkers;
	}
	
	public void setWorkers(List<TreeWorker> workers) {
		workerList.clear();
		workerList.addAll(workers);
		
	}
	
	@Override
	public void update(Node plotNode) {}

	@Override
	public void run() {
		while (true) {
			if (isActive()) {
				for (int i = 0; i < workerList.size(); i++) {
					addToSeries(workerList.get(i).getTsPerSecond(), i, 0);
				}
				applyUpdates();
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
