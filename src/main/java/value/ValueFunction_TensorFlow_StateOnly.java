package value;

import actions.Action;
import actions.ActionQueue;
import game.GameUnified;
import game.IGame;
import game.State;
import tree.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class ValueFunction_TensorFlow_StateOnly extends ValueFunction_TensorFlow {

    private static final int STATE_SIZE = 72;
    private static final int VALUE_SIZE = 1;

    private IGame game = new GameUnified();
    private final ActionQueue actionQueue = new ActionQueue();

    public static GameUnified gameSingle;

    public ValueFunction_TensorFlow_StateOnly(File file) throws FileNotFoundException {
        super(file);
    }

    public ValueFunction_TensorFlow_StateOnly(String fileName, List<Integer> hiddenLayerSizes,
                                              List<String> additionalArgs) throws FileNotFoundException {
        super(fileName, STATE_SIZE, VALUE_SIZE, hiddenLayerSizes, additionalArgs);
    }

    @Override
    public Action getMaximizingAction(Node currentNode) {

        byte[] fullState = gameSingle.getFullState();

            EvaluationResult bestNull = new EvaluationResult();
            for (int i = 1; i < 30; i++) {
                gameSingle.step(false, false, false, false);
                State st = gameSingle.getCurrentState();
                Node nextNode = new Node(currentNode, new Action(i, false, false, false, false), false);
                nextNode.setState(st);
                float val = evaluate(nextNode);
                if (val > bestNull.value) {
                    bestNull.value = val;
                    bestNull.timestep = i;
                }
            }

            gameSingle = gameSingle.restoreFullState(fullState);
            EvaluationResult bestWO = new EvaluationResult();
            for (int i = 1; i < 50; i++) {
                gameSingle.step(false, true, true, false);
                State st = gameSingle.getCurrentState();
                Node nextNode = new Node(currentNode, new Action(i, false, true, true, false), false);
                nextNode.setState(st);
                float val = evaluate(nextNode);
                if (val > bestWO.value) {
                    bestWO.value = val;
                    bestWO.timestep = i;
                }
            }

        gameSingle = gameSingle.restoreFullState(fullState);
        EvaluationResult bestQP = new EvaluationResult();
            for (int i = 1; i < 50; i++) {
                gameSingle.step(true, false, false, true);
                State st = gameSingle.getCurrentState();
                Node nextNode = new Node(currentNode, new Action(i, true, false, false, true), false);
                nextNode.setState(st);
                float val = evaluate(nextNode);
                if (val > bestQP.value) {
                    bestQP.value = val;
                    bestQP.timestep = i;
                }
            }

        Action bestAction;
        if ((bestNull.value >= bestQP.value) && (bestNull.value >= bestWO.value)) {
            bestAction = new Action(bestNull.timestep, false, false, false, false);
        } else if ((bestQP.value > bestNull.value) && (bestQP.value > bestWO.value)) {
            bestAction = new Action(bestQP.timestep, true, false, false, true);
        } else {
            bestAction = new Action(bestWO.timestep, false, true, true, false);
        }

        gameSingle = gameSingle.restoreFullState(fullState);

        return bestAction;
    }

    @Override
    float[] assembleInputFromNode(Node node) {
        return stateStats.standardizeState(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(Node node) {
        return new float[]{node.getValue()/node.visitCount.floatValue()};
    }

    private static class EvaluationResult {
        float value = -Float.MAX_VALUE;
        int timestep = -1;
    }
}
