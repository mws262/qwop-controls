package value;

import actions.Action;
import actions.ActionQueue;
import game.GameThreadSafe;
import game.GameThreadSafeSavable;
import game.State;
import tree.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class ValueFunction_TensorFlow_StateOnly extends ValueFunction_TensorFlow {

    private static final int STATE_SIZE = 72;
    private static final int VALUE_SIZE = 1;

    private GameThreadSafeSavable game = new GameThreadSafeSavable();
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


        Callable<EvaluationResult> noKey = () -> {
            // Test null actions.
            GameThreadSafe gameLocal = game.getRestoredCopy(currentNode.fullState);
            EvaluationResult bestNull = new EvaluationResult();
            for (int i = 1; i < 50; i++) {
                gameLocal.step(false, false, false, false);
                State st = gameLocal.getCurrentState();
                Node nextNode = new Node(currentNode, new Action(i, false, false, false, false), false);
                nextNode.setState(st);
                float val = evaluate(nextNode);
                if (val > bestNull.value) {
                    bestNull.value = val;
                    bestNull.timestep = i;
                }
            }
            return bestNull;
        };

        Callable<EvaluationResult> wo = () -> {
            // Test WO actions.
            GameThreadSafe gameLocal = game.getRestoredCopy(currentNode.fullState);
            EvaluationResult bestWO = new EvaluationResult();
            for (int i = 1; i < 50; i++) {
                gameLocal.step(false, true, true, false);
                State st = gameLocal.getCurrentState();
                Node nextNode = new Node(currentNode, new Action(i, false, true, true, false), false);
                nextNode.setState(st);
                float val = evaluate(nextNode);
                if (val > bestWO.value) {
                    bestWO.value = val;
                    bestWO.timestep = i;
                }
            }
            return bestWO;
        };

        // Test QP actions.
        Callable<EvaluationResult> qp = () -> {
            GameThreadSafe gameLocal = game.getRestoredCopy(currentNode.fullState);

            EvaluationResult bestQP = new EvaluationResult();
            for (int i = 1; i < 50; i++) {
                gameLocal.step(true, false, false, true);
                State st = gameLocal.getCurrentState();
                Node nextNode = new Node(currentNode, new Action(i, true, false, false, true), false);
                nextNode.setState(st);
                float val = evaluate(nextNode);
                if (val > bestQP.value) {
                    bestQP.value = val;
                    bestQP.timestep = i;
                }
            }
            return bestQP;
        };

        ExecutorService ex = Executors.newFixedThreadPool(3);
        List<Callable<EvaluationResult>> evaluations = new ArrayList<>();
        evaluations.add(noKey);
        evaluations.add(wo);
        evaluations.add(qp);

        List<Future<EvaluationResult>> allResults = null;
        try {
            allResults = ex.invokeAll(evaluations);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ex.shutdown();
        try {
            ex.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        EvaluationResult nullResult = null;
        EvaluationResult woResult = null;
        EvaluationResult qpResult = null;
        try {
            nullResult = allResults.get(0).get();
            woResult = allResults.get(1).get();
            qpResult = allResults.get(2).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }


        Action bestAction;
        if (nullResult.value >= qpResult.value  && nullResult.value >= woResult.value) {
            bestAction = new Action(nullResult.timestep, false, false, false, false);
        } else if (qpResult.value > nullResult.value && qpResult.value > woResult.value) {
            bestAction = new Action(qpResult.timestep, true, false, false, true);
        } else {
            bestAction = new Action(woResult.timestep, false, true, true, false);
        }


        return bestAction;
    }

    private void runToNode(Node n) {
        actionQueue.addSequence(n.getSequence());
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
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
