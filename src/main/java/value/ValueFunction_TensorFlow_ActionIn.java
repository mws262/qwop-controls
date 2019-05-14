//package value;
//
//import actions.Action;
//import actions.ActionList;
//import distributions.Distribution_Equal;
//import game.IGame;
//import tree.INode;
//import tree.Node;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
///**
// * [state, action duration] ----> |NEURAL NET| ----> single scalar value
// *
// * This value function evaluates a Node like this:
// * 1. Get the state from the parent Node.
// * 2. Get the action leading to this node (i.e. its action).
// * 3. Concatenate [state, action] and pass to TensorFlow network.
// * 4. A single scalar value pops out.
// *
// * When using it as a controller, given a node:
// * 1. Take the state at the current node.
// * 2. Get all possible actions leaving this node.
// * 3. Concatenate and pass to TensorFlow.
// * 4. Pick the Action which maximizes the resulting value.
// *
// * @author matt
// */
//public class ValueFunction_TensorFlow_ActionIn extends ValueFunction_TensorFlow {
//
//    private static final int STATE_SIZE = 72;
//    private static final int VALUE_SIZE = 1;
//    private static final int ACTION_SIZE = 1;
//
//    /**
//     * Create a value function from an existing neural network save file. If we want to also load a checkpoint
//     * with network weights, call {@link ValueFunction_TensorFlow_ActionIn#loadCheckpoint}
//     * @param file Existing .pb file defining a TensorFlow graph.
//     */
//    public ValueFunction_TensorFlow_ActionIn(File file) throws FileNotFoundException {
//        super(file);
//    }
//
//    public ValueFunction_TensorFlow_ActionIn(String fileName, List<Integer> hiddenLayerSizes,
//                                             List<String> additionalArgs) throws FileNotFoundException {
//        super(fileName, STATE_SIZE + ACTION_SIZE, VALUE_SIZE, hiddenLayerSizes, additionalArgs);
//    }
//
//    @Override
//    float[] assembleInputFromNode(INode node) { // Uses parent state with this node's action.
//        float[] input = new float[STATE_SIZE + ACTION_SIZE];
//        System.arraycopy(stateStats.standardizeState(node.getParent().getState()), 0, input, 0, STATE_SIZE);
//        input[STATE_SIZE + ACTION_SIZE - 1] = node.getAction().getTimestepsTotal();
//        return input;
//    }
//
//    @Override
//    float[] assembleOutputFromNode(Node node) {
//        return new float[]{node.getValue()/node.visitCount.floatValue()};
//    }
//
//    @Override
//    public Action getMaximizingAction(Node currentNode) {
//        ActionList actionChoices;
//
//        // If no action generator is assigned, just use the actions of this node's children.
//        if (Node.potentialActionGenerator == null) {
//            if (currentNode.getChildCount() == 0) {
//                throw new IllegalStateException("Tried to get a maximizing action using a node with no action " +
//                        "generator or existing children.");
//            }
//            Node[] children = currentNode.getChildren();
//            List<Action> childActions = Arrays.stream(children).map(Node::getAction).collect(Collectors.toList());
//            actionChoices = new ActionList(new Distribution_Equal());
//            actionChoices.addAll(childActions);
//        } else {
//            actionChoices = Node.potentialActionGenerator.getPotentialChildActionSet(currentNode);
//        }
//
//        if (actionChoices.isEmpty() || actionChoices.contains(null)) {
//            throw new IllegalStateException("Node has no action generator and this node has no children with actions.");
//        }
//
//
//        float[][] input = new float[1][STATE_SIZE + ACTION_SIZE];
//        System.arraycopy(stateStats.standardizeState(currentNode.getState()), 0, input[0], 0, STATE_SIZE);
//
//        // Find the action which maximizes the value from this state.
//        float maxValue = -Float.MAX_VALUE;
//        Action bestAction = null;
//        for (Action action : actionChoices) {
//            input[0][STATE_SIZE + ACTION_SIZE - 1] = action.getTimestepsTotal();
//            float value = network.evaluateInput(input)[0][0];
//            if (value > maxValue) {
//                maxValue = value;
//                bestAction = action;
//            }
//        }
//
//        Objects.requireNonNull(bestAction);
//        return bestAction;
//    }
//
//    @Override
//    public Action getMaximizingAction(Node currentNode, IGame game) {
//        return getMaximizingAction(currentNode);
//    }
//
//}
