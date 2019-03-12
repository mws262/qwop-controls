package value;

import actions.Action;
import actions.ActionQueue;
import actions.ActionSet;
import distributions.Distribution_Equal;
import game.GameThreadSafe;
import tree.Node;
import tree.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ValueFunction_TensorFlow_StateOnly extends ValueFunction_TensorFlow {

    private static final int STATE_SIZE = 72;
    private static final int VALUE_SIZE = 1;

    private final GameThreadSafe game = new GameThreadSafe();
    private final ActionQueue actionQueue = new ActionQueue();

    public ValueFunction_TensorFlow_StateOnly(File file) throws FileNotFoundException {
        super(file);
    }

    public ValueFunction_TensorFlow_StateOnly(String fileName, List<Integer> hiddenLayerSizes,
                                              List<String> additionalArgs) throws FileNotFoundException {
        super(fileName, STATE_SIZE, VALUE_SIZE, hiddenLayerSizes, additionalArgs);
    }

    @Override
    public Action getMaximizingAction(Node currentNode) {
        if (Node.potentialActionGenerator == null) {
            throw new NullPointerException("Node never was assigned a potential action generator.");
        }
        Set<Action> potentialActions = Node.potentialActionGenerator.getAllPossibleActions();
        //getPotentialChildActionSet(currentNode);

//        ActionSet potentialActions = new ActionSet(new Distribution_Equal());
//        potentialActions.add(new Action(5, false, false, false, false));
//        potentialActions.add(new Action(5, false, true, true, false));
//        potentialActions.add(new Action(5, true, false, false, true));


//        potentialActions.add(new Action(5, true, false, false, false));
//        potentialActions.add(new Action(5, false, false, false, true));
//        potentialActions.add(new Action(5, false, true, false, false));
//        potentialActions.add(new Action(5, false, false, true, false));



        if (potentialActions.size() < 1) {
            throw new IndexOutOfBoundsException("The potential action generator did not generate at least 1 action " +
                    "for the specified node.");
        }

        Action[] actionsToNode = currentNode.getSequence(); // Each evaluation, we need to get back to the edge of
        // the tree.
        float bestActionValue = -Float.MAX_VALUE;
        Action bestAction = null;

        Utility.tic();
        // Iterate over all potential actions to see which results in the highest predicted value.
        for (Action actionBeyond : potentialActions) {
            actionQueue.clearAll();
            game.makeNewWorld();

            // Unconnected node added for the new action.
            Node candidateActionNode = new Node(currentNode, actionBeyond, false);
            actionQueue.addSequence(actionsToNode); // Actions to get back to the edge of the tree.
            actionQueue.addAction(actionBeyond); // New action we want to evaluate.

            while (!actionQueue.isEmpty()) {
                game.step(actionQueue.pollCommand());
            }
            candidateActionNode.setState(game.getCurrentState());

            float candidateActionValue = evaluate(candidateActionNode);
            if (candidateActionValue > bestActionValue) {
                bestAction = actionBeyond;
                bestActionValue = candidateActionValue;
            }
        }
        Utility.toc();
        return Objects.requireNonNull(bestAction);
    }

    @Override
    float[] assembleInputFromNode(Node node) {
        return stateStats.standardizeState(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(Node node) {
        return new float[]{node.getValue()/node.visitCount.floatValue()};
    }
}
