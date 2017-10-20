package data;
import java.io.Serializable;

import main.Action;
import main.Node;
import main.State;

/*
 * Structure to hold actions and states in individual QWOP runs. 
 * Right now, duplicate information is not culled, nor are tree 
 * relationships considered.
 */
public class SaveableSingleGame implements Serializable {

	private static final long serialVersionUID = 2L;
	
	public Action[] actions;
	public State[] states;
	
	/** New TrialNodeMinimal **/
	public SaveableSingleGame(Node terminalNode){
		states = new State[terminalNode.treeDepth];
		actions = new Action[terminalNode.treeDepth];
		Node currentNode = terminalNode;
		
		while ( currentNode.treeDepth > 0 ){
			
			actions[currentNode.treeDepth - 1] = currentNode.getAction();
			states[currentNode.treeDepth - 1] = currentNode.state;
			
			currentNode = currentNode.parent;
		}	
	}
}
