package ui;

import main.Node;
import main.PanelTimeSeries;
import main.TreeStage;

public class PanelTimeSeries_WorkerLoad extends PanelTimeSeries implements Runnable {

	private static final long serialVersionUID = 1L;

	private final TreeStage stage;

	private final int numPlots;

	public PanelTimeSeries_WorkerLoad(TreeStage stage, int numWorkers) {
		super(numWorkers);
		this.stage = stage;
		numPlots = numWorkers;
	}

	@Override
	public void update(Node plotNode) {}

	@Override
	public void run() {
		while (!stage.isFinished()) {
			if (isActive()) {
				for (int i = 0; i < stage.workers.size(); i++) {
					addToSeries(stage.workers.get(i).getTsPerSecond(), i, 0);
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
