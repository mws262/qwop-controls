package tree.node;

import game.action.Action;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;

/**
 * Concrete implementation of the QWOP tree node which contains additional information for graphical drawing (colors,
 * positions, etc.). Most of the functionality is found in {@link NodeQWOPGraphicsBase}.
 *
 * If this amount of functionality is not needed:
 * See {@link NodeQWOPExplorable} for just tree building functionality.
 * See {@link NodeQWOP} for just state and command storage.
 * See {@link NodeGeneric} for basic tree functionality without any QWOP specifics.
 *
 *
 * @author matt
 */
public class NodeQWOPGraphics<C extends Command<?>> extends NodeQWOPGraphicsBase<NodeQWOPGraphics<C>, C> {

    /**
     * @see NodeQWOPGraphicsBase#NodeQWOPGraphicsBase(IState)
     */
    public NodeQWOPGraphics(IState rootState, IActionGenerator<C> actionGenerator) {
        super(rootState, actionGenerator);
    }

    /**
     * @see NodeQWOPGraphicsBase#NodeQWOPGraphicsBase(IState, IActionGenerator)
     */
    public NodeQWOPGraphics(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeQWOPGraphicsBase#NodeQWOPGraphicsBase(NodeQWOPGraphicsBase, Action, IState, IActionGenerator, boolean)
     */
    private NodeQWOPGraphics(NodeQWOPGraphics<C> parent,Action<C> action, IState state,
                             IActionGenerator<C> actionGenerator, boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeQWOPGraphics<C> getThis() {
        return this;
    }

    @Override
    public NodeQWOPGraphics<C> addDoublyLinkedChild(Action<C> action, IState state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPGraphics<C> addBackwardsLinkedChild(Action<C> action, IState state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPGraphics<C> addDoublyLinkedChild(Action<C> action, IState state, IActionGenerator<C> actionGenerator) {
        return new NodeQWOPGraphics<>(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeQWOPGraphics<C> addBackwardsLinkedChild(Action<C> action, IState state, IActionGenerator<C> actionGenerator) {
        return new NodeQWOPGraphics<>(this, action, state, actionGenerator, false);
    }
}
