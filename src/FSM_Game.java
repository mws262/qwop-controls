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
	private Queue<boolean[]> commandQueue = new LinkedList<boolean[]>();

	/** Physics engine stepping parameters. **/
	public final float timestep = 0.04f;
	private final int iterations = 5;

	/** Flags for each of the QWOP keys being down **/
	public boolean Q = false;
	public boolean W = false;
	public boolean O = false;
	public boolean P = false;

	/** State machine state. **/
	private Status currentStatus = Status.IDLE;
	private Status previousStatus = Status.IDLE;

	/** The fixed sequence of QWOP to be cycled through **/
	public static final boolean[] nil_nil = {false, false, false, false}; // Nothing pressed.
	public static final boolean[] W_O = {false,true,true,false};
	public static final boolean[] Q_P = {true,false,false,true};

	// Note: trying to get away from hardcoding sequences. Want to have keypresses also stored in the nodes soon.
	public static final boolean[][] fixedSequence = {nil_nil,W_O,nil_nil,Q_P}; 
	/** Current index in the fixed sequence. **/
	int sequenceIndex = 0;
	int sequenceLength = fixedSequence.length;

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
				if (!commandQueue.isEmpty()){
					currentStatus = Status.INITIALIZE;
				}else if (flagForSingle){ // User selected one to display.
					if (queuedSequence == null) throw new RuntimeException("Game flagged for single run, but no queued sequence ready.");
					addSequence_old(queuedSequence, true);
					flagForSingle = false;
					currentStatus = Status.LOCKED;
				}
				break;
			case LOCKED:
				// Namely if we want to trigger a one-off realtime run, this avoids going to IDLE where the tree might decide to run something instead.
				if (commandQueue.isEmpty()) throw new RuntimeException("Game trying to display a user-selected run, but no sequence is added.");
				currentStatus = Status.INITIALIZE;
			case INITIALIZE:
				newGame();
				if (commandQueue.isEmpty()){
					throw new RuntimeException("Transitions violated. Game was initialized without any queued actions.");
				}else{
					currentStatus = Status.RUNNING_SEQUENCE;
				}
				break;
			case RUNNING_SEQUENCE:
				if (failFlag){ // Stop running if failure detected.
					currentStatus = Status.FAILED;
				}else if (commandQueue.isEmpty()){
					currentStatus = Status.WAITING; // Await any new commands if unfailed, but nothing to do right now.
				}else{

					boolean[] nextCommand = commandQueue.poll(); // Get and remove the next keypresses
					Q = nextCommand[0];
					W = nextCommand[1]; 
					O = nextCommand[2];
					P = nextCommand[3];

					game.everyStep(Q,W,O,P);
					getWorld().step(timestep, iterations);

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
					System.out.println("What?");
					currentStatus = Status.FAILED;
				}else if(!commandQueue.isEmpty()){
					currentStatus = Status.RUNNING_SEQUENCE;
				}
				break;
			case FAILED:
				commandQueue.clear(); // Clear remaining commands.

				if (runRealTime) negotiator.reportEndOfRealTimeSim();
				
				if (flagForSingle){ // User selected one to display.
					addSequence_old(queuedSequence, true);
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

	/** Queue one action (delay + keys down). **/
	public void addAction(int delay, boolean[] keys){
		if (keys.length != 4) throw new RuntimeException("Tried to add keypresses with only " + keys.length + " booleans to define them. Should be 4");
		if (delay < 0) throw new RuntimeException("Cannot hold keys for negative time. Tried: " + delay);

		for (int i = 0; i < delay; i++){
			commandQueue.add(keys); // Each timestep, 1 is removed from the queue.
		}
	}

	/** Queue a sequence of actions. **/
	public void addSequence(int[] delays, boolean[][] keys){
		for (int i = 0; i < delays.length; i++){
			addAction(delays[i], keys[i]);
		}
	}

	/** Assumes a fixed sequence of keypresses. Adds whatever next keypress should come in that sequence and advances. **/
	public void addAction_old(int action){
		if (action < 0) throw new RuntimeException("Cannot hold keys for negative time. Tried: " + action);
		addAction(action,fixedSequence[sequenceIndex]);
		sequenceIndex = (sequenceIndex + 1) % sequenceLength;
	}

	/** Assumes a fixed sequence of keypresses. Adds whatever next keypress should come in that sequence. Always starts from first action, unlike the new version of this method. **/
	public void addSequence_old(int[] seq, boolean realTime){
		if (currentStatus != Status.IDLE) throw new RuntimeException("Cannot add a new sequence unless the game has returned to idle.");
		commandQueue.clear();
		sequenceIndex = 0;
		for (int i = 0; i < seq.length; i++){
			//System.out.println(seq[i]);
			addAction_old(seq[i]);
		}	
		this.runRealTime = realTime;
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
