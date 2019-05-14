package ui;

import tree.NodeQWOPExplorableBase;
import tree.TreeWorker;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PanelTimeSeries_WorkerLoad extends PanelTimeSeries implements Runnable {

    private static final long serialVersionUID = 1L;

    private List<TreeWorker> workerList = new ArrayList<>();

    public PanelTimeSeries_WorkerLoad(int maxWorkers) {
        super(maxWorkers);
        JLabel label = new JLabel();
        label.setText("All plots are game timesteps simulated per wall time vs. wall time.");
        add(label);
    }

    public void setWorkers(List<TreeWorker> workers) {
        workerList.clear();
        workerList.addAll(workers);
    }

    @Override
    public void update(NodeQWOPExplorableBase<?> plotNode) { }

    @Override
    public void run() {
        while (true) {
            if (isActive()) {
                for (int i = 0; i < workerList.size(); i++) {
                    addToSeries((float) workerList.get(i).getTsPerSecond(), i, 0);
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
