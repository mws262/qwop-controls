package goals.value_function;

import actions.Action;
import actions.ActionQueue;
import flashqwop.FlashQWOPServer;
import flashqwop.QWOPStateListener;
import game.State;
import tree.Node;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.File;
import java.io.FileNotFoundException;

@SuppressWarnings("Duplicates")
public class MAIN_FlashEvaluation implements QWOPStateListener {
    private boolean[] wo = new boolean[]{false, true, true, false};
    private boolean[] qp = new boolean[]{true, false, false, true};
    private boolean[] none = new boolean[]{false, false, false, false};

    private boolean awaitingRestart = true;

    Action[] prefix = new Action[]{
      new Action(7, none),
      new Action(49, wo)
    };

    private ValueFunction_TensorFlow valueFunction = null;
    private Node placeholderNode = new Node(); // TODO only really needs the state. This is just acting as a container.
    private FlashQWOPServer server;

    ActionQueue actionQueue = new ActionQueue();

    public MAIN_FlashEvaluation() {
        loadController();
        server = new FlashQWOPServer();
        server.addStateListener(this);
        restart();
    }

    public void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("chk5");
    }

    public Action getControl(State st) {
        placeholderNode.setState(st);
        return valueFunction.getMaximizingAction(placeholderNode);
    }

    public void restart() {
        server.sendResetSignal();
        awaitingRestart = true;
    }
    @Override
    public void stateReceived(int timestep, State state) {
        if (timestep == 0) { // new run has started.
            awaitingRestart = false;
            actionQueue.clearAll();
            actionQueue.addSequence(prefix);
        } else if (state.isFailed()) {
            restart();
            return;
        } else if (awaitingRestart) {
            return;
        }

        if (actionQueue.isEmpty()) {
            actionQueue.addAction(getControl(state));
        }
        server.sendCommand(actionQueue.pollCommand());

    }

    public static void main(String[] args) {
        MAIN_FlashEvaluation controller = new MAIN_FlashEvaluation();
    }
}
