package goals.value_function;

import actions.Action;
import actions.ActionQueue;
import flashqwop.FlashQWOPServer;
import flashqwop.QWOPStateListener;
import game.GameUnified;
import game.State;
import tree.Node;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

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

        getControl(GameUnified.getInitialState()); // TODO make this better. The first controller evaluation ever
        // takes 8 times longer than the rest. I don't know why. In the meantime, just do the first evaluation in a
        // non-time-critical section of the code. In the long term, the controller should be an anytime approach
        // anyway.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    int count = 1;
    boolean[] prevCommand = null;
    int timestepsTracked = 0;
    @Override
    public synchronized void stateReceived(int timestep, State state) {
        if (timestep == 0) { // new run has started.
            System.out.println("zero timestep");
            awaitingRestart = false;
            actionQueue.clearAll();
            actionQueue.addSequence(prefix);
            prevCommand = null;
            timestepsTracked = 0;
        } else if (awaitingRestart) {
            return;
        } else if (state.isFailed()) {
            restart();
            return;
        }
        long t1 = System.currentTimeMillis();

        assert timestep == timestepsTracked; // Have we lost any timesteps?

        if (actionQueue.isEmpty()) {
            Action a = getControl(state);
            System.out.println(a.toString());
            actionQueue.addAction(a);
        }

        boolean[] command = actionQueue.pollCommand();
//        System.out.println(timestep + ", " + state.body.getTh() + ", " + state.body.getDth());
        server.sendCommand(command);


//        // Only send command when it's different from the previous.
//        boolean[] nextCommand = actionQueue.pollCommand();
//        if (!Arrays.equals(prevCommand, nextCommand)) {
//            server.sendCommand(nextCommand);
//        }
//        prevCommand = nextCommand;
        long controlEvalTime = System.currentTimeMillis() - t1;
        if (controlEvalTime > 30) {
            System.out.println("Warning: the control loop time was " + controlEvalTime + "ms. This might be too high.");
        }
        timestepsTracked++;
    }

    public static void main(String[] args) {
        MAIN_FlashEvaluation controller = new MAIN_FlashEvaluation();
    }
}
