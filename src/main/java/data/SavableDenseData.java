package data;

import game.action.Action;
import game.action.Command;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Holds all the states and game.command on a TIMESTEP level for an entire game.
 * Note that a state corresponds to the command that is being held during it, not
 * the command which leads to it like in other parts of the code. Also, there may
 * be 1 more state than command. this is because we arrive at the final state
 * without needing to select an command once we get there.
 *
 * @author matt
 */
public class SavableDenseData<C extends Command<?>> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final IState[] state;

    private final Action<C>[] action;

    private static final Logger logger = LogManager.getLogger(SavableDenseData.class);

    public SavableDenseData(IState[] state, Action<C>[] action) {
        if (state.length != action.length && state.length - action.length != 1) {
            //throw new RuntimeException("StateQWOP and command data must be of the same size, or state must have 1 more
			// element than command. StateQWOP size: " + state.length + ". Action size: " + command.length);
            logger.warn("StateQWOP size: " + state.length + ". Action size: " + action.length + ". Ignoring for " +
					"now.");
        }
        this.state = state;
        this.action = action;
    }

    public SavableDenseData(ArrayList<IState> state, ArrayList<Action<C>> action) {
        this(state.toArray(new IState[0]), action.toArray(new Action[0])); // Convert to arrays and give to other
        // constructor.
    }

    public IState[] getState() {
        return state;
    }

    public Action<C>[] getAction() {
        return action;
    }
}
