package main;
import java.io.Serializable;
import java.util.ArrayList;

public class CondensedNodeInfo implements Serializable{

	private static final long serialVersionUID = 1L;

	public CondensedNodeInfo parent;

	public ArrayList<CondensedNodeInfo> children = new ArrayList<CondensedNodeInfo>();

	public int action;

	public CondensedStateInfo state;

	public static int nodeCounter = 0;

	/* Note that parentNode relationships need to be defined after constructor. */
	public CondensedNodeInfo(CondensedStateInfo state, int action, CondensedNodeInfo parent) {
		this.state = state;
		this.action = action;
		this.parent = parent;
	}

	/* Create as placeholder. For root node */
	public CondensedNodeInfo(){

	}

	/* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Returns the ROOT of a tree. */
	public static CondensedNodeInfo makeNodesFromRunInfo(ArrayList<CondensedRunInfo> runs){
		CondensedNodeInfo rootNode = new CondensedNodeInfo();
		nodeCounter = 1;

		for (CondensedRunInfo run : runs){ // Go through all runs, placing them in the tree.
			CondensedNodeInfo currentNode = rootNode;

			for (int i = 0; i < run.actions.length; i++){ // Iterate through individual actions of this run, travelling down the tree in the process.

				boolean foundExistingMatch = false;
				for (CondensedNodeInfo child: currentNode.children){ // Look at each child of the currently investigated node.
					if (child.action == run.actions[i]){ // If there is already a node for the action we are trying to place, just use it.
						currentNode = child;
						foundExistingMatch = true;
						continue; // We found a match, move on.
					}
				}
				// If this action is unique at this point in the tree, we need to add a new node there.
				if (!foundExistingMatch){
					CondensedNodeInfo newNode = new CondensedNodeInfo(run.states[i], run.actions[i],currentNode);
					currentNode.children.add(newNode);
					currentNode = newNode;
					nodeCounter++;
				}

				//System.out.println(i);
			}
		}

		return rootNode;
	}
}
