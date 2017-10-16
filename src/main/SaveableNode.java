package main;
import java.io.Serializable;
import java.util.ArrayList;

public class SaveableNode implements Serializable{

	private static final long serialVersionUID = 1L;

	public SaveableNode parent;

	public ArrayList<SaveableNode> children = new ArrayList<SaveableNode>();

	public int action;

	public State state;

	public static int nodeCounter = 0;

	/* Note that parentNode relationships need to be defined after constructor. */
	public SaveableNode(State state, int action, SaveableNode parent) {
		this.state = state;
		this.action = action;
		this.parent = parent;
	}

	/* Create as placeholder. For root node */
	public SaveableNode(){

	}

	/* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Returns the ROOT of a tree. */
	public static SaveableNode makeNodesFromRunInfo(ArrayList<SaveableSingleGame> runs){
		SaveableNode rootNode = new SaveableNode();
		nodeCounter = 1;

		for (SaveableSingleGame run : runs){ // Go through all runs, placing them in the tree.
			SaveableNode currentNode = rootNode;

			for (int i = 0; i < run.actions.length; i++){ // Iterate through individual actions of this run, travelling down the tree in the process.

				boolean foundExistingMatch = false;
				for (SaveableNode child: currentNode.children){ // Look at each child of the currently investigated node.
					if (child.action == run.actions[i]){ // If there is already a node for the action we are trying to place, just use it.
						currentNode = child;
						foundExistingMatch = true;
						continue; // We found a match, move on.
					}
				}
				// If this action is unique at this point in the tree, we need to add a new node there.
				if (!foundExistingMatch){
					SaveableNode newNode = new SaveableNode(run.states[i], run.actions[i],currentNode);
					currentNode.children.add(newNode);
					currentNode = newNode;
					nodeCounter++;
				}
			}
		}

		return rootNode;
	}
}
