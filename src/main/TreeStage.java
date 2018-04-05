package main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * If we want to switch and change between different 
 * 
 * 
 * @author matt
 *
 */
public abstract class TreeStage implements Runnable{

	/** **/
	private Node treeRoot;

	/** Currently only supporting one sampler per stage. Must define which sampler to use in the inheritors of this abstract class. **/
	protected ISampler sampler;

	/** Each stage gets its own workers to avoid contamination. Probably could combine later if necessary. **/
	private List<Thread> workerThreads = new ArrayList<Thread>();
	private List<TreeWorker> workers = new ArrayList<TreeWorker>();

	/** Thread managing this stage and its workers. **/
	private Thread stageThread;

	/** Number of TreeWorkers to be used. **/
	protected int numWorkers;

	/** Is the managing thread of this stage running? **/
	private volatile boolean running = false;

	private final Object lock = new Object();

	public void initialize(Node treeRoot, int numWorkers) {
		this.treeRoot = treeRoot;
		this.numWorkers = numWorkers;
		running = true;
		stageThread = new Thread(this);
		stageThread.start();

		for (int i = 0; i < numWorkers; i++) {
			// Make workers
			TreeWorker w;
			ISampler s = sampler.clone(); // Sampler must be determined in the extender of this class.
			w = new TreeWorker(treeRoot, s);
			workers.add(w);

			// Make worker threads
			Thread wThread = new Thread(w);
			wThread.setName(w.workerName);
			workerThreads.add(wThread);		
		}

		// Start threads
		for (Thread t : workerThreads) {
			t.start();
		}

		// This blocks the main thread until this stage is done.
		synchronized(lock){
			while (running) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
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
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** Externally check if this stage has wrapped up yet. **/
	public boolean isFinished() {
		return !running;
	}

	/** Query the stage for its final results. **/
	public abstract List<Node> getResults();

	/** Check through the tree for termination conditions. **/
	public abstract boolean checkTerminationConditions();

	/** Terminate this stage, destroying the workers and their threads in the process. **/
	public void terminate() {
		running = false;
		
		// Stop threads and get rid of them.
		Iterator<TreeWorker> iter = workers.iterator();
		while (iter.hasNext()) {
			TreeWorker tw = iter.next();
			tw.terminateWorker();
		}

		// Wait for all threads to end.
		for (Thread wt : workerThreads) {
			try {
				wt.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// Stop the monitoring thread and let the main thread continue.
		synchronized(lock){
			lock.notify();
		}
	}

	/** Get the root node that this stage is operating from. It cannot change from an external caller's perspective, so no set method. **/
	public Node getRootNode() {
		return treeRoot;
	}
}
