package main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import game.StateVariable;
import samplers.ISampler;
import savers.IDataSaver;

/**
 * If we want to switch and change between different
 *
 * @author matt
 */
public abstract class TreeStage implements Runnable {

    /** **/
    private Node stageRoot;

    /**
     * Currently only supporting one sampler per stage. Must define which sampler to use in the inheritors of this
     * abstract class.
     **/
    protected ISampler sampler;

    /**
     * Data saving selection. Must define which saver to use in the inheritors of this abstract class.
     **/
    protected IDataSaver saver;

    /**
     * Each stage gets its own workers to avoid contamination. Probably could combine later if necessary.
     **/
    public List<TreeWorker> workers = new ArrayList<>();

    /**
     * Thread managing this stage and its workers.
     **/
    private Thread stageThread;

    /**
     * Number of TreeWorkers to be used.
     **/
    protected int numWorkers;

    /**
     * Is the managing thread of this stage running?
     **/
    private volatile boolean running = true;

    /**
     * Does this stage block the mai thread until done?
     **/
    public boolean blocking = true;

    private final Object lock = new Object();

    public void initialize(List<TreeWorker> treeWorkers, Node stageRoot) {
        numWorkers = treeWorkers.size();
        if (numWorkers < 1)
            throw new RuntimeException("Tried to assign a tree stage an invalid number of workers: " + numWorkers);

        this.workers = treeWorkers;
        this.stageRoot = stageRoot;

        for (TreeWorker tw : treeWorkers) {
            tw.setRoot(stageRoot);
            tw.setSaver(saver);
            tw.setSampler(sampler);
            tw.startWorker();
        }

        running = true;
        Thread stageThread = new Thread(this);
        stageThread.start();

        if (blocking) {
            // This blocks the main thread until this stage is done.
            synchronized (lock) {
                while (running) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //int thresh = 2000;
    @Override
    public void run() {
        // Monitor the progress of this stage's workers.
        while (running) {
            // EXPERIMENTAL MEMORY PRUNING.
            //
            //
            //			if (TreeWorker.getTotalGamesPlayed()  > thresh) {
            //				count = 0;
            //				pruneStatesForMemory(getRootNode());
            //				thresh += 2000;
            //			}

            if (checkTerminationConditions()) {
                terminate();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Externally check if this stage has wrapped up yet.
     **/
    public boolean isFinished() {
        return !running;
    }

    /**
     * Query the stage for its final results.
     **/
    public abstract List<Node> getResults();

    /**
     * Check through the tree for termination conditions.
     **/
    public abstract boolean checkTerminationConditions();

    private static int count = 0;

    private void pruneStatesForMemory(Node node) {
        if (!node.children.isEmpty() && node.state != null) {//node.uncheckedActions.size() == 0) {
            for (StateVariable st : node.state.getStateList()) {
                st = null;
            }
            node.state = null;
            count++;
            for (Node child : node.children) {
                pruneStatesForMemory(child);
            }
        }
    }

    /**
     * Terminate this stage, destroying the workers and their threads in the process.
     **/
    public void terminate() {
        running = false;
        System.out.println("Terminate called on a stage.");
        saver.reportStageEnding(getRootNode().getRoot(), getResults()); // Changed to save ALL the way back to real
        // root, not just subtree root.
        // Stop threads and get rid of them.

        for (TreeWorker tw : workers) {
            tw.pauseWorker(); // Pause the worker until another stage needs it.
        }

        // Stop the monitoring thread and let the main thread continue.
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * Check if workers are running.
     **/
    public boolean areWorkersRunning() {
        synchronized (workers) {

            Iterator<TreeWorker> iter = workers.iterator();
            if (!iter.hasNext())
                return true; // It's stupid if the termination condition gets caught before any threads have a chance to get going.
            while (iter.hasNext()) {
                if (iter.next().isRunning()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the root node that this stage is operating from. It cannot change from an external caller's perspective,
     * so no set method.
     **/
    public Node getRootNode() {
        return stageRoot;
    }

    public int getNumberOfWorkers() {
        return numWorkers;
    }
}
