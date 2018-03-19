package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class FSM_Game implements Runnable{

	private QWOPGame game;

	/** Angle failure limits. Fail if torso angle is too big or small to rule out stupid hopping that eventually falls. **/
	public float torsoAngUpper = 1.2f;
	public float torsoAngLower = -1.2f; // Negative is falling backwards. 0.4 is start angle.

	/** Failure occurrence flag **/
	private boolean failFlag = false;

	/** Thread loop running? **/
	private boolean running = true;

	/** Whether the FSM is locked for some other action to continue. **/
	private volatile boolean locked = false;

	/** Confirms whether the FSM has finished its previous cycle and is effectively locked. **/
	private volatile boolean isLocked = false; 

	/** Queued commands, IE QWOP key presses **/
	public ActionQueue actionQueue = new ActionQueue();

	/** Physics engine stepping parameters. **/
	public final float timestep = 0.04f;
	private final int iterations = 5;
	private float stepsSimulated = 0;

	/** Flags for each of the QWOP keys being down **/
	public boolean Q = false;
	public boolean W = false;
	public boolean O = false;
	public boolean P = false;

	/** State machine state. **/
	private Status currentStatus = Status.IDLE;
	private Status previousStatus = Status.IDLE;

	private INegotiateGame negotiator;

	/** Are we flagged to run a single, externally supplied sequence? **/
	private boolean flagForSingle = false;

	/** Do we add delays to make the simulation realtime? **/
	private boolean runRealTime = false;

	/** Queued sequence to run. **/
	private Action[] queuedSequence;

	/** State machine states for the QWOP game **/
	public enum Status{
		IDLE, WAITING, INITIALIZE, RUNNING_SEQUENCE, LOCKED, FAILED
	}

	public FSM_Game() {
		newGame(); // Just to initialize all the shapes in the game for drawing purposes.
	}

	@Override
	public void run() {	
		while(running){
			if (locked){
				isLocked = true;
				continue; // If we call one of the lock methods, the loop skips everything.
			}else{
				isLocked = false;
			}
			switch (currentStatus){
			case IDLE:
				// Initialize a new game if commands have been added to the queue.
				if (!actionQueue.isEmpty()){
					currentStatus = Status.INITIALIZE;
				}else if (flagForSingle){ // User selected one to display.
					if (queuedSequence == null) throw new RuntimeException("Game flagged for single run, but no queued sequence ready.");
					actionQueue.addSequence(queuedSequence);
					runRealTime = true;
					flagForSingle = false;
					currentStatus = Status.LOCKED;
				}
				break;
			case LOCKED:
				// Namely if we want to trigger a one-off realtime run, this avoids going to IDLE where the tree might decide to run something instead.
				if (actionQueue.isEmpty()) throw new RuntimeException("Game trying to display a user-selected run, but no sequence is added.");
				currentStatus = Status.INITIALIZE;
			case INITIALIZE:
				newGame();
				if (actionQueue.isEmpty()){
					throw new RuntimeException("Transitions violated. Game was initialized without any queued actions.");
				}else{
					currentStatus = Status.RUNNING_SEQUENCE;
				}
				break;
			case RUNNING_SEQUENCE:
				if (failFlag){ // Stop running if failure detected.
					currentStatus = Status.FAILED;
				}else if (actionQueue.isEmpty()){
					currentStatus = Status.WAITING; // Await any new commands if unfailed, but nothing to do right now.
				}else{
					boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
					Q = nextCommand[0];
					W = nextCommand[1]; 
					O = nextCommand[2];
					P = nextCommand[3];

					game.everyStep(Q,W,O,P);
					getWorld().step(timestep, iterations);

					// Extra fail conditions besides contacts.
					float angle = game.torsoBody.getAngle();
					if (angle > torsoAngUpper || angle < torsoAngLower) {
						reportFall();
					}

					negotiator.reportGameStep(actionQueue.peekThisAction());
					stepsSimulated++;
					if (runRealTime){
						negotiator.reportQWOPKeys(Q,W,O,P);
						try {
							Thread.sleep(90);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				break;
			case WAITING:
				if (failFlag || runRealTime){
					currentStatus = Status.FAILED;
				}else if(!actionQueue.isEmpty()){
					currentStatus = Status.RUNNING_SEQUENCE;
				}
				break;
			case FAILED:
				actionQueue.clearAll(); // Clear remaining commands.
				if (runRealTime) negotiator.reportEndOfRealTimeSim();

				if (flagForSingle){ // User selected one to display.
					actionQueue.addSequence(queuedSequence);
					runRealTime = true;
					flagForSingle = false;
					currentStatus = Status.LOCKED;
				}else{ // Normal operation
					currentStatus = Status.IDLE;
					runRealTime = false;
				}

				break;
			default:
				break;
			}
			// Report changes to negotiator.
			if (currentStatus != previousStatus){
				previousStatus = currentStatus;	
				negotiator.statusChange_Game(currentStatus);
			}
		}
	}

	/** QWOP initial condition. Good way to give the root node a state. **/
	public static State getInitialState(){
		QWOPGame g = new QWOPGame();
		g.setup();
		State initState = new State(g);
		initState.failedState = false;
		return initState;
	}
	/** Begin a new game. **/
	private void newGame(){
		failFlag = false; // Unflag failure.
		game = new QWOPGame();
		game.setup();
		getWorld().setContactListener(new CollisionListener());
	}

	public void addSequence(Action[] sequence){
		actionQueue.addSequence(sequence);
	}
	public void addAction(Action action){
		actionQueue.addAction(action);
	}
	/** Callable to instantly stop real-time simulating a run. Usually when the UI tab is changed. **/
	public void killRealtimeRun(){
		if (runRealTime){
			reportFall();
		}
	}

	/** Queue up a single realtime game to play next time its available. **/
	public void runSingleRealtime(Action[] actionSequence){
		queuedSequence = actionSequence;
		flagForSingle = true;
	}

	/** World is the reference to internal Box2D stuff. **/
	public World getWorld(){
		return game.getWorld();
	}

	/** Get the current game. **/
	public QWOPGame getGame(){
		return game;
	}

	/** Check if we're trying to run a game in realtime. **/
	public boolean isRealtime(){
		return runRealTime;
	}

	/** Total time simulated since this execution of the program. **/
	public float getTimeSimulated(){
		return stepsSimulated*timestep;
	}

	/** Get the state of the runner. **/
	public synchronized State getGameState(){
		State currentState = new State(game);
		currentState.failedState = failFlag;
		return currentState;
	}

	/** Set the external communication channel **/
	public void setNegotiator(INegotiateGame negotiator){
		this.negotiator = negotiator;
	}

	/** Call this to instantly end a simulation and determine a fall. Mainly should be reported by the collision listener. */
	public void reportFall(){
		failFlag = true;
	}

	/** Get the FSM status. **/
	public synchronized Status getFSMStatus(){
		return currentStatus;
	}

	/** Stop the FSM after the current cycle, and return the status. Prevents changes from having odd effects in the middle of a cycle. **/
	public Status getFSMStatusAndLock(){
		if (isLocked) throw new RuntimeException("Someone tried to lock the game while it was already locked.");
		locked = true;

		while (!isLocked);

		return getFSMStatus();
	}

	/** Stop the FSM after the current cycle, and return the status. Prevents changes from having odd effects in the middle of a cycle. **/
	public void unlockFSM(){
		if (!locked) throw new RuntimeException("Tried to unlock the game FSM but it wasn't previously locked. Not necessarily fatal, but indicates some bad logic.");
		if (!isLocked) throw new RuntimeException("Tried to unlock the game FSM, but it never got around to locked in the first place. This really shouldn't happen.");
		locked = false;
	}

	/** Stop the game FSM. **/
	public void kill() {
		running = false;
	}

	/**
	 * All things related to queueing actions should happen in here. Actions themselves act like queues,
	 * so this action queue decides when to switch actions when one is depleted.
	 * @author Matt
	 *
	 */
	public class ActionQueue{

		/** Actions are the delays between keypresses. **/
		private Queue<Action> actionQueue = new LinkedList<Action>();

		/** All actions done or queued since the last reset. Unlike the queue, things aren't removed until reset. **/
		private ArrayList<Action> actionListFull = new ArrayList<Action>();

		/** Integer action currently in progress. If the action is 20, this will be 20 even when 15 commands have been issued. **/
		private Action currentAction;

		/** Is there anything at all queued up to execute? Includes both the currentAction and the actionQueue **/
		private boolean isEmpty = true;

		public ActionQueue(){}

		/** See the action we are currently executing. Does not change the queue. **/
		public Action peekThisAction(){
			return currentAction;
		}

		/** See the next action we will execute. Does not change the queue. **/
		public Action peekNextAction(){
			return actionQueue.peek();
		}

		/** See the next keypresses. **/
		public boolean[] peekCommand(){
			return currentAction.peek();
		}

		/** Adds a new action to the end of the queue. **/
		public synchronized void addAction(Action action){
			actionQueue.add(action);
			actionListFull.add(action);
			isEmpty = false;
		}

		/** Add a sequence of actions. NOTE: sequence is NOT reset unless clearAll is called. **/
		public synchronized void addSequence(Action[] actions){
			for (int i = 0; i < actions.length; i++){
				addAction(actions[i]);
			}
		}

		/** Request the next QWOP keypress commands from the added sequence. **/
		public synchronized boolean[] pollCommand(){
			if (actionQueue.isEmpty() && (currentAction == null || !currentAction.hasNext())){
				throw new RuntimeException("Tried to get a command off the queue when nothing is queued up.");
			}

			// If the current action has no more keypresses, load up the next one.
			if (currentAction == null || !currentAction.hasNext()){
				if (currentAction != null) currentAction.reset();
				currentAction = actionQueue.poll();
				//if (currentAction == null) System.out.println("WTF");
			}

			boolean[] nextCommand = currentAction.poll();

			if (!currentAction.hasNext() && actionQueue.isEmpty()){
				currentAction.reset();
				isEmpty = true;
			}
			return nextCommand;
		}

		/** Remove everything from the queues and reset the sequence. **/
		public synchronized void clearAll(){
			actionQueue.clear();
			actionListFull.clear();
			if (currentAction != null) currentAction.reset();
			currentAction = null;

			//while (actionQueue.size() > 0 || currentAction != null
			isEmpty = true;
		}

		/** Check if the queue has anything in it. **/
		public synchronized boolean isEmpty(){ return isEmpty; }

		public Action[] getActionsInCurrentRun(){
			Action[] actions = new Action[actionListFull.size()];
			for (int i = 0; i < actions.length; i++){
				actions[i] = actionListFull.get(i);
			}
			return actions;
		}

		public int getCurrentActionIdx(){
			return actionListFull.size() - actionQueue.size() - 1;
		}
	}
}
