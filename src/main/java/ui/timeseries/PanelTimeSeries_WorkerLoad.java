package ui.timeseries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import tree.TreeWorker;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PanelTimeSeries_WorkerLoad extends PanelTimeSeries implements Runnable {

    private static final long serialVersionUID = 1L;

    private List<TreeWorker> workerList = new ArrayList<>();

    private final String name;

    private AtomicBoolean active = new AtomicBoolean(false);

    private Thread thread;

    public PanelTimeSeries_WorkerLoad(@JsonProperty("name") String name, @JsonProperty("numberOfPlots") int numberOfPlots) {
        super(numberOfPlots);
        this.name = name;
        JLabel label = new JLabel();
        label.setText("All plots are game timesteps simulated per wall time vs. wall time.");
        add(label);
    }

    public void setWorkers(List<TreeWorker<?>> workers) {
        workerList.clear();
        workerList.addAll(workers);
    }

    @Override
    public void run() {
        while (active.get()) {
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void activateTab() {
        active.set(true);
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void deactivateTab() {
        active.set(false);
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    @JsonIgnore
    public boolean isActive() {
        return active.get();
    }
}
