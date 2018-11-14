package tree;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

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
     */
    protected ISampler sampler;

    /**
     * Data saving selection. Must define which saver to use in the inheritors of this abstract class.
     */
    protected IDataSaver saver;

    /**
     * Each stage gets its own workers to avoid contamination. Probably could combine later if necessary.
     */
    public List<TreeWorker> workers;

    /**
     * Number of TreeWorkers to be used.
     */
    protected int numWorkers;

    /**
     * Is the managing thread of this stage running?
     */
    private volatile boolean running = true;

    /**
     * Does this stage block the mai thread until done?
     */
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
            // This blocks the goals thread until this stage is done.
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

    @Override
    public void run() {
        // Monitor the progress of this stage's workers.
        while (running) {
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
     */
    public boolean isFinished() {
        return !running;
    }

    /**
     * Query the stage for its final results.
     */
    public abstract List<Node> getResults();

    /**
     * Check through the tree for termination conditions.
     */
    public abstract boolean checkTerminationConditions();

    /**
     * Terminate this stage, destroying the workers and their threads in the process.
     */
    public void terminate() {
        running = false;
        System.out.println("Terminate called on a stage.");
        saver.reportStageEnding(getRootNode().getRoot(), getResults()); // Changed to save ALL the way back to real
        // root, not just subtree root.

        for (TreeWorker tw : workers) {
            tw.pauseWorker(); // Pause the worker until another stage needs it.
        }

        // Stop the monitoring thread and let the goals thread continue.
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * Check if workers are running.
     */
    public boolean areWorkersRunning() {
        synchronized (workers) {
            Iterator<TreeWorker> iterator = workers.iterator();
            if (!iterator.hasNext())
                return true; // It's stupid if the termination condition gets caught before any threads have a chance to get going.
            while (iterator.hasNext()) {
                if (iterator.next().isRunning()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the root node that this stage is operating from. It cannot change from an external caller's perspective,
     * so no set method.
     */
    public Node getRootNode() {
        return stageRoot;
    }

    public int getNumberOfWorkers() {
        return numWorkers;
    }

    /**
     * Sometimes a stage end is triggered before a necessary state is assigned to a node. This method can waste time
     * for us until the state is assigned!
     * @param n Node to wait for its state to be assigned.
     */
    void waitForStateAssignment(Node n) throws TimeoutException {
        int max = 1000; // Maximum waiting intervals before timing out.
        int count = 0;
        while (n.isStateUnassigned()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
            if (count > max) {
                throw new TimeoutException("Waited for a state to be assigned to a node for too long. Something " +
                        "bigger is up.");
            }
        }
    }
}
