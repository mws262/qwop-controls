package data;

import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.io.Serializable;

/**
 * Structure to hold game.command and states in individual QWOP runs for saving to file. This holds data sparsely, meaning
 * that only the {@link IState} at nodes are recorded, along with the {@link Action Actions} going between
 * them. Data at every single timestep is not used, and tree relationships are not considered. This is the container
 * used by {@link savers.DataSaver_Sparse}. The mechanism of saving to file is the direct serialization of the Java
 * classes, rather than a Protobuf or TFRecord format.
 *
 * @author matt
 */
public class SavableSingleGame<C extends Command<?>> implements Serializable {

    private static final long serialVersionUID = 2L;

    public Action<C>[] actions;
    public IState[] states;

    /**
     * Make a new container for holding the sparse representation of a single run.
     *
     * @param terminalNode End node of the run to be saved. All states and game.command up to this point will be saved.
     */
    public SavableSingleGame(NodeGameBase<?, C> terminalNode) {
        states = new IState[terminalNode.getTreeDepth()];
        actions = new Action[terminalNode.getTreeDepth()];
        NodeGameBase<?, C> currentNode = terminalNode;

        while (currentNode.getTreeDepth() > 0) {

            actions[currentNode.getTreeDepth() - 1] = currentNode.getAction();
            states[currentNode.getTreeDepth() - 1] = currentNode.getState();

            currentNode = currentNode.getParent();
        }
    }
}
