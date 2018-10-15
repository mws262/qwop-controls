package ui;

import java.util.ArrayList;
import java.util.List;

import tree.Node;
import tree.TreeWorker;

public class PanelTimeSeries_WorkerLoad extends PanelTimeSeries implements Runnable {

    private static final long serialVersionUID = 1L;

    private List<TreeWorker> workerList = new ArrayList<>();

    public PanelTimeSeries_WorkerLoad(int maxWorkers) {
        super(maxWorkers);
    }

    public void setWorkers(List<TreeWorker> workers) {
        workerList.clear();
        workerList.addAll(workers);

    }

    @Override
    public void update(Node plotNode) {
    }

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
