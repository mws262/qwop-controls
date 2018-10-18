package data;

import java.io.Serializable;
import java.util.ArrayList;

import game.State;
import actions.Action;

public class SavableNode implements Serializable {

    private static final long serialVersionUID = 2L;

    public SavableNode parent;

    public ArrayList<SavableNode> children = new ArrayList<>();

    public Action action;

    public State state;

    public static int nodeCounter = 0;

    /* Note that parentNode relationships need to be defined after constructor. */
    public SavableNode(State state, Action action, SavableNode parent) {
        this.state = state;
        this.action = action;
        this.parent = parent;
    }

    /* Create as placeholder. For root node */
    public SavableNode() {

    }

    /* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Returns the ROOT of a tree. */
    public static SavableNode makeNodesFromRunInfo(ArrayList<SavableSingleGame> runs) {
        SavableNode rootNode = new SavableNode();
        nodeCounter = 1;

        for (SavableSingleGame run : runs) { // Go through all runs, placing them in the tree.
            SavableNode currentNode = rootNode;

            for (int i = 0; i < run.actions.length; i++) { // Iterate through individual actions of this run,
            	// travelling down the tree in the process.

                boolean foundExistingMatch = false;
                for (SavableNode child : currentNode.children) { // Look at each child of the currently investigated
                	// node.
                    if (child.action == run.actions[i]) { // If there is already a node for the action we are trying
                    	// to place, just use it.
                        currentNode = child;
                        foundExistingMatch = true;
                        continue; // We found a match, move on.
                    }
                }
                // If this action is unique at this point in the tree, we need to add a new node there.
                if (!foundExistingMatch) {
                    SavableNode newNode = new SavableNode(run.states[i], run.actions[i], currentNode);
                    currentNode.children.add(newNode);
                    currentNode = newNode;
                    nodeCounter++;
                }
            }
        }
        return rootNode;
    }
}
