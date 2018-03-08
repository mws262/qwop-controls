package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

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
		Action localCopy = action.getCopy();
		actionQueue.add(localCopy);
		actionListFull.add(localCopy);
		isEmpty = false;
	}

	/** Add a sequence of actions. NOTE: sequence is NOT reset unless clearAll is called. **/
	public synchronized void addSequence(Action[] actions){
		for (int i = 0; i < actions.length; i++){
			addAction(actions[i].getCopy());
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
