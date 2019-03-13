package goals.cold_start_analysis;

import actions.ActionQueue;
import game.GameSingleThread;
import game.GameThreadSafe;
import game.State;
import org.jbox2d.common.Mat22;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.joints.RevoluteJoint;

import java.awt.*;

/**
 * Take a known sequence of reasonable actions, simulate for some number of actions, and introduce a second runner,
 * with a cloned state, but a cold start of the Box2D internal solvers. Simulate both together for the rest of the
 * run with identical input commands.
 *
 * @author matt
 */
public class MAIN_WarmStartContacts extends CompareWarmStartToColdBase {

    public static void main(String[] args) {
        new MAIN_WarmStartContacts().run();
    }

    public void run() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue actionQueue = getSampleActions();

        GameThreadSafe gameFullRun = new GameThreadSafe(); // This game will run all the commands, start to finish.
        GameThreadSafe gameColdStart = new GameThreadSafe(); // This will start at some point in the middle of the sequence,

        GameSingleThread gameAttemptWarmStart = GameSingleThread.getInstance();
        // with a cloned state from gameFullRun, but a cold start on all the internal solvers.

        // Get to a certain part of the run where we want to introduce another cold start runner.

        // Decide at which action to introduce a cold-started runner.
        int coldStartAction = 10;
        while (actionQueue.getCurrentActionIdx() < coldStartAction) {
            boolean[] nextCmd = actionQueue.pollCommand();
            gameFullRun.step(nextCmd);
            gameAttemptWarmStart.step(nextCmd);
        }
        State coldStartState = gameFullRun.getCurrentState();
        gameColdStart.setState(coldStartState);

        // Retrieve all contacts.
        State st = gameAttemptWarmStart.getCurrentState();
//        ContactEdge rfootC= gameAttemptWarmStart.rFootBody.getContactList();
//        ContactEdge lfootC= gameAttemptWarmStart.lFootBody.getContactList();

        // Save joint info.
        RevoluteJoint[] joints = gameAttemptWarmStart.getAllJoints();
        Vec2[] jointWarmstarts = new Vec2[joints.length];

        Vec2[] jointPivotForces = new Vec2[joints.length];
        float[] jointMotorForces = new float[joints.length];
        float[] jointLimitForces = new float[joints.length];
        float[] jointlimitPositionImpulse = new float[joints.length];
        float[] jointMotorSpeeds = new float[joints.length];
        float[] jointMaxMotorTorque = new float[joints.length];
        float[] jointLowerAngle = new float[joints.length];
        float[] jointUpperAngle = new float[joints.length];
        Mat22[] jointPivotMass = new Mat22[joints.length];

        for (int i = 0; i < joints.length; i++) {
            jointWarmstarts[i] = joints[i].m_lastWarmStartingPivotForce.clone();
            jointPivotForces[i] = joints[i].m_pivotForce.clone();
            jointMotorForces[i] = joints[i].m_motorForce;
            jointLimitForces[i] = joints[i].m_limitForce;
            jointlimitPositionImpulse[i] = joints[i].m_limitPositionImpulse;
            jointMotorSpeeds[i] = joints[i].m_motorSpeed;
            jointMaxMotorTorque[i] = joints[i].m_maxMotorTorque;
            jointLowerAngle[i] = joints[i].m_lowerAngle;
            jointUpperAngle[i] = joints[i].m_upperAngle;
            jointPivotMass[i] = joints[i].m_pivotMass.clone();
        }

        // Attempt to set contacts.
        gameAttemptWarmStart.makeNewWorld();
//        for (int i = 0; i < 7; i++) {
//            gameAttemptWarmStart.step(false, true, true, false);
//        }
        gameAttemptWarmStart.setState(st);

        joints = gameAttemptWarmStart.getAllJoints();
        for (int i = 0; i < joints.length; i++) {
            joints[i].m_lastWarmStartingPivotForce.set(jointWarmstarts[i]);
            joints[i].m_pivotForce.set(jointPivotForces[i]);

            joints[i].m_motorForce = jointMotorForces[i];
            joints[i].m_limitForce = jointLimitForces[i];
            joints[i].m_limitPositionImpulse = jointlimitPositionImpulse[i];
            joints[i].m_motorSpeed = jointMotorSpeeds[i];
            joints[i].m_maxMotorTorque = jointMaxMotorTorque[i];
            joints[i].m_lowerAngle = jointLowerAngle[i];
            joints[i].m_upperAngle = jointUpperAngle[i];

            joints[i].m_pivotMass.set(jointPivotMass[i]);
        }
//        if (lfootC != null) {
//           PolyContact clfoot =
//                   (PolyContact) gameAttemptWarmStart.getWorld().m_contactManager.pairAdded(gameAttemptWarmStart.trackBody.m_shapeList,
//                   gameAttemptWarmStart.lFootBody.m_shapeList);
//           ManifoldPoint manifoldPt1 = clfoot.m_manifold.points[0];
//           ManifoldPoint manifoldPt2 = clfoot.m_manifold.points[1];
//           manifoldPt1.set(lfootC.contact.getManifolds().get(0).points[0]);
//           manifoldPt2.set(lfootC.contact.getManifolds().get(0).points[1]);
//        }
//
//        if (rfootC != null) {
//            PolyContact crfoot =
//                    (PolyContact) gameAttemptWarmStart.getWorld().m_contactManager.pairAdded(gameAttemptWarmStart.trackBody.m_shapeList,
//                            gameAttemptWarmStart.rFootBody.m_shapeList);
//            ManifoldPoint manifoldPt1 = crfoot.m_manifold.points[0];
//            ManifoldPoint manifoldPt2 = crfoot.m_manifold.points[1];
//            manifoldPt1.set(rfootC.contact.getManifolds().get(0).points[0]);
//            manifoldPt2.set(rfootC.contact.getManifolds().get(0).points[1]);
//        }

        runnerPanel.setMainState(gameFullRun.getCurrentState());
        runnerPanel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);
        runnerPanel.addSecondaryState(gameAttemptWarmStart.getCurrentState(), Color.GREEN);
        repaint();
        try { // Pause for a moment so the user can see that the initial positions of the runners match.
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Simulate the rest of the run with both runners.
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand();

            gameFullRun.step(nextCommand);
            gameColdStart.step(nextCommand);
            gameAttemptWarmStart.step(nextCommand);

            runnerPanel.clearSecondaryStates();
            runnerPanel.setMainState(gameFullRun.getCurrentState());
            runnerPanel.addSecondaryState(gameColdStart.getCurrentState(), Color.RED);
            runnerPanel.addSecondaryState(gameAttemptWarmStart.getCurrentState(), Color.GREEN);

            repaint();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
