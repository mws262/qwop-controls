package tree;

import actions.Action;
import game.State;

public interface INode {
    Action getAction();
    State getState();
    INode getParent();
}
