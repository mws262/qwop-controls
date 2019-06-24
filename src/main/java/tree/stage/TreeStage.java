package tree.stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.sampler.ISampler;
import savers.IDataSaver;
import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * If we want to switch and change between different tree stage goals.
 *
 * @author matt
 */
public abstract class TreeStage implements Runnable {

    /** **/
    private NodeQWOPExplorableBase<?> stageRoot;

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
    public final List<TreeWorker> workers = new ArrayList<>();

    /**
     * Number of TreeWorkers to be used.
     */
    protected int numWorkers;

    /**
     * Is the managing thread of this stage running?
     */
    private volatile boolean running = true;

    /**
     * Does this stage block the main thread until done?
     */
    public boolean blocking = true;

    private final Object lock = new Object();

    private Logger logger = LogManager.getLogger(TreeStage.class);

    public void initialize(List<TreeWorker> treeWorkers, NodeQWOPExplorableBase<?> stageRoot) {
        numWorkers = treeWorkers.size();
        if (numWorkers < 1)
            throw new RuntimeException("Tried to assign a tree stage an invalid number of workers: " + numWorkers);

        workers.addAll(treeWorkers);
        this.stageRoot = stageRoot;

        for (TreeWorker tw : treeWorkers) {
            tw.setRoot(stageRoot);
            tw.setSaver(saver);
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
    public abstract List<NodeQWOPBase<?>> getResults();

    /**
     * Check through the tree for termination conditions.
     */
    public abstract boolean checkTerminationConditions();

    /**
     * Terminate this stage, destroying the workers and their threads in the process.
     */
    public void terminate() {
        running = false;
        logger.info("Terminate called on a stage.");
        saver.reportStageEnding(getRootNode().getRoot(), getResults()); // Changed to save ALL the way back to real
        // root, not just subtree root.

        workers.forEach(TreeWorker::pauseWorker);

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
    public NodeQWOPExplorableBase<?> getRootNode() {
        return stageRoot;
    }

    public int getNumberOfWorkers() {
        return numWorkers;
    }
}
