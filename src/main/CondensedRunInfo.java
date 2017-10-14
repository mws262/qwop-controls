package main;
import java.io.Serializable;

/*
 * Structure to hold actions and states in individual QWOP runs. 
 * Right now, duplicate information is not culled, nor are tree 
 * relationships considered.
 */
public class CondensedRunInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public int[] actions;
	public CondensedStateInfo[] states;
	
	/** New TrialNodeMinimal **/
	public CondensedRunInfo(TrialNodeMinimal terminalNode){
		states = new CondensedStateInfo[terminalNode.treeDepth];
		actions = new int[terminalNode.treeDepth];
		
		TrialNodeMinimal currentNode = terminalNode;
		
		while ( currentNode.treeDepth > 0 ){
			
			actions[currentNode.treeDepth - 1] = currentNode.controlAction;
			states[currentNode.treeDepth - 1] = currentNode.state;
			
			currentNode = currentNode.parent;
		}	
	}
// I'm removing support for importing games from the old versions of the software.
//	@Deprecated
//	/** Old TrialNodes **/
//	public CondensedRunInfo(TrialNode terminalNode) {
//		states = new CondensedStateInfo[terminalNode.TreeDepth];
//		actions = new int[terminalNode.TreeDepth];
//		
//		TrialNode currentNode = terminalNode;
//		
//		while ( currentNode.TreeDepth > 0 ){
//			
//			actions[currentNode.TreeDepth - 1] = currentNode.ControlAction;
//			states[currentNode.TreeDepth - 1] = currentNode.csi;
//			
//			currentNode = currentNode.ParentNode;
//		}
//	}
}
