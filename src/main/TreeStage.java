package main;

import java.util.ArrayList;
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
	
	/** Is the managing thread of this stage running? **/
	private boolean running = false;
	private boolean isFinished = false;
	
	public void initialize(Node treeRoot, int numWorkers) {
		this.treeRoot = treeRoot;
		
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
			
			// Start threads
			wThread.start();
		}
		
		stageThread = new Thread(this);
		stageThread.start();
	}
	
	@Override
	public void run() {
		// Monitor the progress of this stage's workers.
		while (running) {
			if (checkTerminationConditions()) {
				terminate();
			}
		}
	}
	
	/** Externally check if this stage has wrapped up yet. **/
	public boolean isFinished() {
		return isFinished;
	}
	
	/** Query the stage for its final results. **/
	public abstract List<Node> getResults();
	
	/** Check through the tree for termination conditions. **/
	public abstract boolean checkTerminationConditions();
	
	/** Terminate this stage, destroying the workers and their threads in the process. **/
	public void terminate() {
		// Stop the monitoring thread.
		running = false;
		try {
			stageThread.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		// Stop threads and get rid of them.
		for (int i = 0; i < workers.size(); i++) {
			workers.get(i).active = false;
			try {
				workerThreads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/** Get the root node that this stage is operating from. It cannot change from an external caller's perspective, so no set method. **/
	public Node getRootNode() {
		return treeRoot;
	}
}
