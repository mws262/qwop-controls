import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;


public class FSM_Game implements Runnable{

	private QWOPGame game;

	/** Failure occurrance flag **/
	private boolean failFlag = false;

	/** Thread loop running? **/
	public boolean running = true;

	/** Whether the FSM is locked for some other action to continue. **/
	private volatile boolean locked = false;

	/** Confirms whether the FSM has finished its previous cycle and is effectively locked. **/
	private volatile boolean isLocked = false; 

	/** Queued commands, IE QWOP key presses **/
	QWOPQueue qwopQueue = new QWOPQueue();
	
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

	private Negotiator negotiator;

	/** Are we flagged to run a single, externally supplied sequence? **/
	private boolean flagForSingle = false;

	/** Do we add delays to make the simulation realtime? **/
	private boolean runRealTime = false;

	/** Queued sequence to run. **/
	private int[] queuedSequence;

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
				//System.out.println("set it!");
				isLocked = true;
				continue; // If we call one of the lock methods, the loop skips everything.
			}else{
				isLocked = false;
			}
			switch (currentStatus){
			case IDLE:
				// Initialize a new game if commands have been added to the queue.
				if (!qwopQueue.isEmpty()){
					currentStatus = Status.INITIALIZE;
				}else if (flagForSingle){ // User selected one to display.
					if (queuedSequence == null) throw new RuntimeException("Game flagged for single run, but no queued sequence ready.");
					qwopQueue.addSequence(queuedSequence);
					runRealTime = true;
					flagForSingle = false;
					currentStatus = Status.LOCKED;
				}
				break;
			case LOCKED:
				// Namely if we want to trigger a one-off realtime run, this avoids going to IDLE where the tree might decide to run something instead.
				if (qwopQueue.isEmpty()) throw new RuntimeException("Game trying to display a user-selected run, but no sequence is added.");
				currentStatus = Status.INITIALIZE;
			case INITIALIZE:
				newGame();
				if (qwopQueue.isEmpty()){
					throw new RuntimeException("Transitions violated. Game was initialized without any queued actions.");
				}else{
					currentStatus = Status.RUNNING_SEQUENCE;
				}
				break;
			case RUNNING_SEQUENCE:
				if (failFlag){ // Stop running if failure detected.
					currentStatus = Status.FAILED;
				}else if (qwopQueue.isEmpty()){
					currentStatus = Status.WAITING; // Await any new commands if unfailed, but nothing to do right now.
				}else{
					boolean[] nextCommand = qwopQueue.pollCommand(); // Get and remove the next keypresses
					Q = nextCommand[0];
					W = nextCommand[1]; 
					O = nextCommand[2];
					P = nextCommand[3];

					game.everyStep(Q,W,O,P);
					getWorld().step(timestep, iterations);
					stepsSimulated++;
					if (runRealTime){
						negotiator.reportQWOPKeys(Q,W,O,P);
						try {
							Thread.sleep(30);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				break;
			case WAITING:
				if (failFlag || runRealTime){
					currentStatus = Status.FAILED;
				}else if(!qwopQueue.isEmpty()){
					currentStatus = Status.RUNNING_SEQUENCE;
				}
				break;
			case FAILED:
				qwopQueue.clearAll(); // Clear remaining commands.
				if (runRealTime) negotiator.reportEndOfRealTimeSim();
				
				if (flagForSingle){ // User selected one to display.
					qwopQueue.addSequence(queuedSequence);
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
				negotiator.statusChange_Game(currentStatus);
			}
			previousStatus = currentStatus;	
		}
	}

	/** Begin a new game. **/
	private void newGame(){
		failFlag = false; // Unflag failure.
		game = new QWOPGame();
		game.Setup();
		getWorld().setContactListener(new CollisionListener());
	}

	public void addSequence(int[] sequence){
		qwopQueue.addSequence(sequence);
	}
	public void addAction(int action){
		qwopQueue.addAction(action);
	}
	/** Callable to instantly stop real-time simulating a run. Usually when the UI tab is changed. **/
	public void killRealtimeRun(){
		if (runRealTime){
			reportFall(); // TODO MATT WHEN YOU GET BACK DEBUG THIS!!!
		}
	}

	/** Queue up a single realtime game to play next time its available. **/
	public void runSingleRealtime(int[] actionSequence){
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
	public CondensedStateInfo getGameState(){
		CondensedStateInfo currentState = new CondensedStateInfo(game);
		currentState.failedState = failFlag;
		return currentState;
	}

	/** Set the external communication channel **/
	public void setNegotiator(Negotiator negotiator){
		this.negotiator = negotiator;
	}

	/** Call this to instantly end a simulation and determine a fall. Mainly should be reported by the collision listener. */
	public void reportFall(){
		failFlag = true;
	}

	/** Get the FSM status. **/
	public Status getFSMStatus(){
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

	/**
	 * All things related to queueing actions should happen in here.
	 * @author Matt
	 *
	 */
	public class QWOPQueue{

		/** Actions are the delays between keypresses. **/
		private Queue<Integer> actionQueue = new LinkedList<Integer>();

		/** Commands are the 4 booleans expressing whether any particular key is down. **/
		private Queue<boolean[]> commandQueue = new LinkedList<boolean[]>();

		/** All actions done or queued since the last reset. Unlike the queue, things aren't removed until reset. **/
		private ArrayList<Integer> actionsInRunList = new ArrayList<Integer>();
		
		/** Integer action currently in progress. If the action is 20, this will be 20 even when 15 commands have been issued. **/
		private int currentAction;

		/** Is there anything at all queued up to execute? **/
		private boolean isEmpty = true;

		/** The fixed sequence of QWOP to be cycled through **/
		public final boolean[] nil_nil = {false, false, false, false}; // Nothing pressed.
		public final boolean[] W_O = {false,true,true,false};
		public final boolean[] Q_P = {true,false,false,true};

		// Note: trying to get away from hardcoding sequences. Want to have keypresses also stored in the nodes soon.
		public final boolean[][] fixedSequence = {nil_nil,W_O,nil_nil,Q_P}; 

		/** Current index in the fixed sequence. **/
		int sequenceIndex = 0;
		int sequenceLength = fixedSequence.length;

		public QWOPQueue() {}

		/** See the action we are currently executing. Does not change the queue. **/
		public int peekThisAction(){
			return currentAction;
		}

		/** See the next action we will execute. Does not change the queue. **/
		public int peekNextAction(){
			return actionQueue.peek();
		}
		
		/** See the next keypresses. **/
		public boolean[] peekCommand(){
			return commandQueue.peek();
		}
		
		/** Adds a new action to the end of the queue. **/
		public void addAction(int action){
			if (action < 0) throw new RuntimeException("Cannot hold keys for negative time. Tried: " + action);
			actionQueue.add(action);
			actionsInRunList.add(action);
			isEmpty = false;
		}
		
		/** Add a sequence of actions. NOTE: sequence is NOT reset unless clearAll is called. **/
		public void addSequence(int[] actions){
			for (int i = 0; i < actions.length; i++){
				addAction(actions[i]);
			}
		}
		
		/** Request the next QWOP keypress commands from the added sequence. **/
		public boolean[] pollCommand(){
			//System.out.println("POLL");
			if (commandQueue.isEmpty() && actionQueue.isEmpty()){
				throw new RuntimeException("Tried to get a command off the queue when nothing is queued up.");
			}

			// If the current action has no more keypresses, load up the next one.
			if (commandQueue.isEmpty()){
				currentAction = actionQueue.poll();
				actionToCommand(currentAction);
			}
			
			boolean[] nextCommand = commandQueue.poll();
			
			if (commandQueue.isEmpty() && actionQueue.isEmpty()){
				isEmpty = true;
			}
			return nextCommand;
		}

		/** Takes an integer action and adds all the boolean commands to the command queue.
		 * Removes the action from the action queue and lists it as the current action.
		 * @param action
		 */
		private void actionToCommand(int action){
			actionToCommand(action,fixedSequence[sequenceIndex]);
			sequenceIndex = (sequenceIndex + 1) % sequenceLength;
		}
		
		private void actionToCommand(int delay, boolean[] keys){
			if (keys.length != 4) throw new RuntimeException("Tried to add keypresses with only " + keys.length + " booleans to define them. Should be 4");
			if (delay < 0) throw new RuntimeException("Cannot hold keys for negative time. Tried: " + delay);

			for (int i = 0; i < delay; i++){
				commandQueue.add(keys); // Each timestep, 1 is removed from the queue.
			}
		}

		/** Remove everything from the queues and reset the sequence. **/
		public void clearAll(){
			commandQueue.clear();
			actionQueue.clear();
			actionsInRunList.clear();
			isEmpty = true;
			sequenceIndex = 0;
		}
		
		/** Check if the queue has anything in it. **/
		public boolean isEmpty(){ return isEmpty; }
		
		public int[] getActionsInCurrentRun(){
			int[] actions = new int[actionsInRunList.size()];
			for (int i = 0; i < actions.length; i++){
				actions[i] = actionsInRunList.get(i);
			}
			return actions;
		}
		
		public int getCurrentActionIdx(){
			return actionsInRunList.size() - actionQueue.size() - 1;
		}
	}
	/** Listens for collisions involving lower arms and head (implicitly with the ground) **/
	private class CollisionListener implements ContactListener{

		boolean rFootDown = false;
		boolean lFootDown = false;

		public CollisionListener(){
		}
		@Override
		public void add(ContactPoint point) {
			Shape fixtureA = point.shape1;
			Shape fixtureB = point.shape2;
			//Failure when head, arms, or thighs hit the ground.
			if(fixtureA.m_body.equals(game.HeadBody) ||
					fixtureB.m_body.equals(game.HeadBody) ||
					fixtureA.m_body.equals(game.LLArmBody) ||
					fixtureB.m_body.equals(game.LLArmBody) ||
					fixtureA.m_body.equals(game.RLArmBody) ||
					fixtureB.m_body.equals(game.RLArmBody)) {
				reportFall();
			}else if(fixtureA.m_body.equals(game.LThighBody)||
					fixtureB.m_body.equals(game.LThighBody)||
					fixtureA.m_body.equals(game.RThighBody)||
					fixtureB.m_body.equals(game.RThighBody)){
				reportFall();
			}else if(fixtureA.m_body.equals(game.RFootBody) || fixtureB.m_body.equals(game.RFootBody)){//Track when each foot hits the ground.
				rFootDown = true;		
			}else if(fixtureA.m_body.equals(game.LFootBody) || fixtureB.m_body.equals(game.LFootBody)){
				lFootDown = true;
			}	
		}
		@Override
		public void persist(ContactPoint point) {}
		@Override
		public void remove(ContactPoint point) {
			//Track when each foot leaves the ground.
			Shape fixtureA = point.shape1;
			Shape fixtureB = point.shape2;
			if(fixtureA.m_body.equals(game.RFootBody) || fixtureB.m_body.equals(game.RFootBody)){
				rFootDown = false;
			}else if(fixtureA.m_body.equals(game.LFootBody) || fixtureB.m_body.equals(game.LFootBody)){
				lFootDown = false;
			}	
		}
		@Override
		public void result(ContactResult point) {}
	}
}
