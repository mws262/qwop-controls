package value;

import actions.Action;
import actions.ActionQueue;
import actions.ActionSet;
import distributions.Distribution_Equal;
import game.GameSingleThread;
import game.GameThreadSafe;
import game.State;
import org.jbox2d.common.Mat22;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.dynamics.joints.RevoluteJoint;
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

    private final GameSingleThread game = GameSingleThread.getInstance();
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
        float initX = currentNode.getState().body.getX();

        // Test null actions.
        game.makeNewWorld();
        runToNode(currentNode);
        //game.setState(currentNode.getState());
        float bestNullVal = -Float.MAX_VALUE;
        int bestNullTs = 0;
        for (int i = 1; i < 30; i++) {
            game.step(false, false, false, false);
            State st = game.getCurrentState();
            Node nextNode = new Node(currentNode, new Action(i, false, false, false, false), false);
            nextNode.setState(st);
            float val = evaluate(nextNode);
            if (val > bestNullVal) {
                bestNullVal = val;
                bestNullTs = i;
            }
        }

        // Test WO actions.
        game.makeNewWorld();
        runToNode(currentNode);
        // Test null actions.
//        game.setState(currentNode.getState());

        float bestWOVal = -Float.MAX_VALUE;
        int bestWOTs = 0;
        for (int i = 1; i < 50; i++) {
            game.step(false, true, true, false);
            State st = game.getCurrentState();
            Node nextNode = new Node(currentNode, new Action(i, false, true, true, false), false);
            nextNode.setState(st);
            float val = evaluate(nextNode);
            if (val > bestWOVal) {
                bestWOVal = val;
                bestWOTs = i;
            }
        }

        // Test QP actions.
        game.makeNewWorld();
        runToNode(currentNode);
        // Test null actions.
//        game.setState(currentNode.getState());

        float bestQPVal = -Float.MAX_VALUE;
        int bestQPTs = 0;
        for (int i = 1; i < 50; i++) {
            game.step(true, false, false, true);
            State st = game.getCurrentState();
            Node nextNode = new Node(currentNode, new Action(i, true, false, false, true), false);
            nextNode.setState(st);
            float val = evaluate(nextNode);
            if (val > bestQPVal) {
                bestQPVal = val;
                bestQPTs = i;
            }
        }

        Action bestAction;
        if (bestNullVal >= bestQPVal && bestNullVal >= bestWOVal) {
            bestAction = new Action(bestNullTs, false, false, false, false);
        } else if (bestQPVal > bestNullVal && bestQPVal > bestWOVal) {
            bestAction = new Action(bestQPTs, true, false, false, true);
        } else {
            bestAction = new Action(bestWOTs, false, true, true, false);
        }


        return bestAction;
    }

    private void runToNode(Node n) {
        actionQueue.addSequence(n.getSequence());
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
    }
//
//    @Override
//    public Action getMaximizingAction(Node currentNode) {
//        if (Node.potentialActionGenerator == null) {
//            throw new NullPointerException("Node never was assigned a potential action generator.");
//        }
//        Set<Action> potentialActions = Node.potentialActionGenerator.getAllPossibleActions();
//        //getPotentialChildActionSet(currentNode);
//
////        ActionSet potentialActions = new ActionSet(new Distribution_Equal());
////        potentialActions.add(new Action(5, false, false, false, false));
////        potentialActions.add(new Action(5, false, true, true, false));
////        potentialActions.add(new Action(5, true, false, false, true));
//
//
////        potentialActions.add(new Action(5, true, false, false, false));
////        potentialActions.add(new Action(5, false, false, false, true));
////        potentialActions.add(new Action(5, false, true, false, false));
////        potentialActions.add(new Action(5, false, false, true, false));
//
//
//
//        if (potentialActions.size() < 1) {
//            throw new IndexOutOfBoundsException("The potential action generator did not generate at least 1 action " +
//                    "for the specified node.");
//        }
//
//        Action[] actionsToNode = currentNode.getSequence(); // Each evaluation, we need to get back to the edge of
//        // the tree.
//        float bestActionValue = -Float.MAX_VALUE;
//        Action bestAction = null;
//
//
//        // Get to the edge once.
//        actionQueue.clearAll();
////        game.makeNewWorld();
////        actionQueue.addSequence(actionsToNode); // Actions to get back to the edge of the tree.
////        while (!actionQueue.isEmpty()) {
////            game.step(actionQueue.pollCommand());
////        }
////
////        // Retrieve all contacts.
////        State st = game.getCurrentState();
//
//
//        // Save joint info.
////        Object[] joints = game.getAllJoints();
////        Vec2[] jointWarmstarts = new Vec2[joints.length];
////
////        Vec2[] jointPivotForces = new Vec2[joints.length];
////        float[] jointMotorForces = new float[joints.length];
////        float[] jointLimitForces = new float[joints.length];
////        float[] jointlimitPositionImpulse = new float[joints.length];
////        float[] jointMotorSpeeds = new float[joints.length];
////        float[] jointMaxMotorTorque = new float[joints.length];
////        float[] jointLowerAngle = new float[joints.length];
////        float[] jointUpperAngle = new float[joints.length];
////        Mat22[] jointPivotMass = new Mat22[joints.length];
////
////
////
////        for (int i = 0; i < joints.length; i++) {
////            RevoluteJoint joint = (RevoluteJoint)joints[i];
////            jointWarmstarts[i] = joint.m_lastWarmStartingPivotForce.clone();
////            jointPivotForces[i] = joint.m_pivotForce.clone();
////
////            jointMotorForces[i] = joint.m_motorForce;
////            jointLimitForces[i] = joint.m_limitForce;
////            jointlimitPositionImpulse[i] = joint.m_limitPositionImpulse;
////            jointMotorSpeeds[i] = joint.m_motorSpeed;
////            jointMaxMotorTorque[i] = joint.m_maxMotorTorque;
////            jointLowerAngle[i] = joint.m_lowerAngle;
////            jointUpperAngle[i] = joint.m_upperAngle;
////            jointPivotMass[i] = joint.m_pivotMass.clone();
////        }
//
//        Utility.tic();
//        for (Action actionBeyond : potentialActions) {
//            //game.makeNewWorld();
//            game.setState(currentNode.getState());
////            for (int i = 0; i < joints.length; i++) {
////                RevoluteJoint joint = (RevoluteJoint)joints[i];
////
////                joint.m_lastWarmStartingPivotForce.set(jointWarmstarts[i]);
////                joint.m_pivotForce.set(jointPivotForces[i]);
////                joint.m_motorForce = jointMotorForces[i];
////                joint.m_limitForce = jointLimitForces[i];
////                joint.m_limitPositionImpulse = jointlimitPositionImpulse[i];
////                joint.m_motorSpeed = jointMotorSpeeds[i];
////                joint.m_maxMotorTorque = jointMaxMotorTorque[i];
////                joint.m_lowerAngle = jointLowerAngle[i];
////                joint.m_upperAngle = jointUpperAngle[i];
////                joint.m_pivotMass.set(jointPivotMass[i]);
////            }
//            Node candidateActionNode = new Node(currentNode, actionBeyond, false);
//
//            actionQueue.addAction(actionBeyond); // New action we want to evaluate.
//            while (!actionQueue.isEmpty()) {
//                game.step(actionQueue.pollCommand());
//            }
//            candidateActionNode.setState(game.getCurrentState());
//
//            float candidateActionValue = evaluate(candidateActionNode);
//            if (candidateActionValue > bestActionValue) {
//                bestAction = actionBeyond;
//                bestActionValue = candidateActionValue;
//            }
//        }
//        Utility.toc();
////        Utility.tic();
////        // Iterate over all potential actions to see which results in the highest predicted value.
////        for (Action actionBeyond : potentialActions) {
////            actionQueue.clearAll();
////            game.makeNewWorld();
////
////            // Unconnected node added for the new action.
////            Node candidateActionNode = new Node(currentNode, actionBeyond, false);
////            actionQueue.addSequence(actionsToNode); // Actions to get back to the edge of the tree.
////            actionQueue.addAction(actionBeyond); // New action we want to evaluate.
////
////            while (!actionQueue.isEmpty()) {
////                game.step(actionQueue.pollCommand());
////            }
////            candidateActionNode.setState(game.getCurrentState());
////
////            float candidateActionValue = evaluate(candidateActionNode);
////            if (candidateActionValue > bestActionValue) {
////                bestAction = actionBeyond;
////                bestActionValue = candidateActionValue;
////            }
////        }
////        Utility.toc();
//        return Objects.requireNonNull(bestAction);
//    }

    @Override
    float[] assembleInputFromNode(Node node) {
        return stateStats.standardizeState(node.getState());
    }

    @Override
    float[] assembleOutputFromNode(Node node) {
        return new float[]{node.getValue()/node.visitCount.floatValue()};
    }
}
