package data;

import java.io.Serializable;

import game.State;
import actions.Action;
import tree.Node;

/*
 * Structure to hold actions and states in individual QWOP runs.
 * Right now, duplicate information is not culled, nor are tree
 * relationships considered.
 */
public class SavableSingleGame implements Serializable {

    private static final long serialVersionUID = 2L;

    public Action[] actions;
    public State[] states;

    public SavableSingleGame(Node terminalNode) {
        states = new State[terminalNode.getTreeDepth()];
        actions = new Action[terminalNode.getTreeDepth()];
        Node currentNode = terminalNode;

        while (currentNode.getTreeDepth() > 0) {

            actions[currentNode.getTreeDepth() - 1] = currentNode.getAction();
            states[currentNode.getTreeDepth() - 1] = currentNode.getState();

            currentNode = currentNode.getParent();
        }
    }
}