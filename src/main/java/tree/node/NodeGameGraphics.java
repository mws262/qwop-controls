package tree.node;

import game.action.Action;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;

/**
 * Concrete implementation of the QWOP tree node which contains additional information for graphical drawing (colors,
 * positions, etc.). Most of the functionality is found in {@link NodeGameGraphicsBase}.
 *
 * If this amount of functionality is not needed:
 * See {@link NodeGameExplorable} for just tree building functionality.
 * See {@link NodeGame} for just state and command storage.
 * See {@link NodeGeneric} for basic tree functionality without any QWOP specifics.
 *
 *
 * @author matt
 */
public class NodeGameGraphics<C extends Command<?>, S extends IState>
        extends NodeGameGraphicsBase<NodeGameGraphics<C, S>, C, S> {

    /**
     * @see NodeGameGraphicsBase#NodeGameGraphicsBase(IState)
     */
    public NodeGameGraphics(S rootState, IActionGenerator<C> actionGenerator) {
        super(rootState, actionGenerator);
    }

    /**
     * @see NodeGameGraphicsBase#NodeGameGraphicsBase(IState, IActionGenerator)
     */
    public NodeGameGraphics(S rootState) {
        super(rootState);
    }

    /**
     * @see NodeGameGraphicsBase#NodeGameGraphicsBase(NodeGameGraphicsBase, Action, IState, IActionGenerator, boolean)
     */
    private NodeGameGraphics(NodeGameGraphics<C, S> parent, Action<C> action, S state,
                             IActionGenerator<C> actionGenerator, boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeGameGraphics<C, S> getThis() {
        return this;
    }

    @Override
    public NodeGameGraphics<C, S> addDoublyLinkedChild(Action<C> action, S state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeGameGraphics<C, S> addBackwardsLinkedChild(Action<C> action, S state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeGameGraphics<C, S> addDoublyLinkedChild(Action<C> action, S state,
                                                     IActionGenerator<C> actionGenerator) {
        return new NodeGameGraphics<>(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeGameGraphics<C, S> addBackwardsLinkedChild(Action<C> action, S state,
                                                        IActionGenerator<C> actionGenerator) {
        return new NodeGameGraphics<>(this, action, state, actionGenerator, false);
    }
}
