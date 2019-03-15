package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static game.GameConstants.*;

/**
 * Handles creating and interacting with the QWOP game world in Box2D. See {@link GameConstants} for all the system
 * parameters.
 * <p>
 * This loads all the Box2D classes needed on a unique ClassLoader. This means that World.class from one instance
 * of this loader is different from the World.class of another loader. This means that they will have their
 * own static variables and should not interfere with each other. This solves the problem of having multithreaded
 * simulations that we don't want to interact with each other.
 *
 * @author matt
 */
@SuppressWarnings("Duplicates")
public class GameThreadSafe implements IGame, Serializable {
    GameClassLoader classLoader = new GameClassLoader();

    /**
     * Number of timesteps in this game.
     */
    private long timestepsSimulated = 0;

    /**
     * Has a game world been created yet?
     */
    private boolean initialized = false;

    /**
     * Initial runner state.
     */
    private static final State initState = new GameThreadSafe().getCurrentState(); // Make sure this stays below all the

    /**
     * Normal stroke for line drawing.
     */
    private static final Stroke normalStroke = new BasicStroke(0.5f);

    /**
     * Has this game reached failure conditions?
     */
    private boolean isFailed = false;

    /**
     * List of shapes for use by graphics stuff.
     */
    private List<Object> shapeList;

    // World definition:
    public Object world;

    // Body/shape definitions:
    private Object trackDef, rFootDef, lFootDef, rCalfDef, lCalfDef, rThighDef, lThighDef, torsoDef, headDef, rUArmDef,
            lUArmDef, rLArmDef, lLArmDef;

    private Object trackShape, rFootShape, lFootShape, rCalfShape, lCalfShape, rThighShape, lThighShape, torsoShape,
            headShape, rUArmShape, lUArmShape, rLArmShape, lLArmShape;

    private Object rFootBody, lFootBody, rCalfBody, lCalfBody, rThighBody, lThighBody, torsoBody, headBody, rUArmBody, lUArmBody, rLArmBody, lLArmBody;

    // Joint definitions
    @SuppressWarnings("unused")
    private Object rHipJDef, lHipJDef, rKneeJDef, lKneeJDef, rAnkleJDef, lAnkleJDef, rShoulderJDef, lShoulderJDef,
            rElbowJDef, lElbowJDef, neckJDef;

    // Joint objects
    private Object rHipJ, lHipJ, rKneeJ, lKneeJ, rAnkleJ, lAnkleJ, rShoulderJ, lShoulderJ, rElbowJ, lElbowJ, neckJ;

    private boolean rFootDown = false;
    private boolean lFootDown = false;

    transient public Color mainRunnerColor = Color.BLACK;
    transient public Stroke mainRunnerStroke = new BasicStroke(1);

    /** Should the game be marked as failed if the thighs touch the ground? (happens with knees touching the ground. **/
    public boolean failOnThighContact = true;

    private final String name;

    //private Object contactListener;


//    private Class<?> _World, _MassData, _BodyDef, _Vec2, _PolygonDef, _CircleDef, _AABB, _RevoluteJointDef, _Body,
//            _ContactListener, _ShapeType, _PolygonShape, _CircleShape, _EdgeShape, _XForm, _ShapeDef, _JointDef;

    /**
     * Make a new game on its own ClassLoader.
     */
    public GameThreadSafe() {
//        _World = classLoader._World;
//        _MassData = classLoader._MassData;
//        _BodyDef = classLoader._BodyDef;
//        _Vec2 = classLoader._Vec2;
//        _PolygonDef = classLoader._PolygonDef;
//        _CircleDef = classLoader._CircleDef;
//        _AABB = classLoader._AABB;
//        _RevoluteJointDef = classLoader._RevoluteJointDef;
//        _Body = classLoader._Body;
//        _ContactListener = classLoader._ContactListener;
//        _ShapeType = classLoader._ShapeType;
//        _PolygonShape = classLoader._PolygonShape;
//        _CircleShape = classLoader._CircleShape;
//        _EdgeShape = classLoader._EdgeShape;
//        _XForm = classLoader._XForm;
//        _ShapeDef = classLoader._ShapeDef;
//        _JointDef = classLoader._JointDef;
//
        try {
            oneTimeSetup(); // Create all the shape and body definitions that never need changing.
            makeNewWorld();
        } catch (SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        name = "game" + UUID.randomUUID().toString();
    }

    /**
     * Apply a disturbance impulse to the body COM.
     */
    public void applyBodyImpulse(float xComp, float yComp) {
        try {
            Object worldCenter = torsoBody.getClass().getMethod("getWorldCenter").invoke(torsoBody);
            torsoBody.getClass().getMethod("applyImpulse", classLoader._Vec2, classLoader._Vec2).invoke(torsoBody, makeVec2(xComp
                    , yComp),
                    worldCenter);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    /**
     * Apply a disturbance torque to the body.
     */
    public void applyBodyTorque(float cwTorque) {
        try {
            torsoBody.getClass().getMethod("applyTorque", float.class).invoke(torsoBody, cwTorque);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to avoid the verbose reflection stuff every time.
     */
    private void setMotorSpeed(Object joint, float motorSpeed) {
        try {
            joint.getClass().getField("m_motorSpeed").setFloat(joint, motorSpeed);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to avoid the verbose reflection stuff every time.
     */
    private void setMaxMotorTorque(Object joint, float motorTorque) {
        try {
            joint.getClass().getField("m_maxMotorTorque").setFloat(joint, motorTorque);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to avoid the verbose reflection stuff every time.
     */
    private void setJointLowerBound(Object joint, float lowerBound) {
        try {
            joint.getClass().getField("m_lowerAngle").setFloat(joint, lowerBound);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convenience method to avoid the verbose reflection stuff every time.
     */
    private void setJointUpperBound(Object joint, float upperBound) {
        try {
            joint.getClass().getField("m_upperAngle").setFloat(joint, upperBound);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Step the game forward 1 timestep with the specified keys pressed.
     */
    public void step(boolean q, boolean w, boolean o, boolean p) {
        try {
            // Involuntary couplings (no QWOP presses).
            float neckAngle = (float) neckJ.getClass().getMethod("getJointAngle").invoke(neckJ);
            float neckAngularRate = (float) neckJ.getClass().getMethod("getJointSpeed").invoke(neckJ);
            float neckTorque = -neckStiff * neckAngle + 0 * neckDamp * neckAngularRate;
            neckTorque = neckTorque + 0 * 400f * (neckAngle + 0.2f); //This bizarre term is probably a roundabout way
            // of adjust equilibrium position.

            //Elbow spring torque
            float rElbowAngle = (float) rElbowJ.getClass().getMethod("getJointAngle").invoke(rElbowJ);
            float rElbowAngularRate = (float) rElbowJ.getClass().getMethod("getJointSpeed").invoke(rElbowJ);
            float rElbowTorque = -rElbowStiff * rElbowAngle + 0 * rElbowDamp * rElbowAngularRate;

            float lElbowAngle = (float) lElbowJ.getClass().getMethod("getJointAngle").invoke(lElbowJ);
            float lElbowAngularRate = (float) lElbowJ.getClass().getMethod("getJointSpeed").invoke(lElbowJ);
            float lElbowTorque = -lElbowStiff * lElbowAngle + 0 * lElbowDamp * lElbowAngularRate;

            //For now, using motors with high speed settings and torque limits to simulate springs. I don't know a
            // better way for now.

            setMotorSpeed(neckJ, 1000f * Math.signum(neckTorque));
            setMotorSpeed(rElbowJ, 1000f * Math.signum(rElbowTorque));
            setMotorSpeed(lElbowJ, 1000f * Math.signum(lElbowTorque));

            setMaxMotorTorque(neckJ, Math.abs(neckTorque));
            setMaxMotorTorque(rElbowJ, Math.abs(rElbowTorque));
            setMaxMotorTorque(lElbowJ, Math.abs(lElbowTorque));

            /* QW Press Stuff */
            //See spreadsheet for complete rules and priority explanations.
            if (q) {
                //Set speed 1 for hips:
                setMotorSpeed(lHipJ, lHipSpeed2);
                setMotorSpeed(rHipJ, rHipSpeed2);
                //Set speed 1 for shoulders:
                setMotorSpeed(lShoulderJ, lShoulderSpeed2);
                setMotorSpeed(rShoulderJ, rShoulderSpeed2);
            } else if (w) {
                //Set speed 2 for hips:
                setMotorSpeed(lHipJ, lHipSpeed1);
                setMotorSpeed(rHipJ, rHipSpeed1);
                //set speed 2 for shoulders:
                setMotorSpeed(lShoulderJ, lShoulderSpeed1);
                setMotorSpeed(rShoulderJ, rShoulderSpeed1);
            } else {
                //Set hip and ankle speeds to 0:
                setMotorSpeed(lHipJ, 0f);
                setMotorSpeed(rHipJ, 0f);
                setMotorSpeed(lShoulderJ, 0f);
                setMotorSpeed(rShoulderJ, 0f);
            }

            // Ankle/Hip Coupling -+ 0*Requires either Q or W pressed.
            if (q || w) {
                //Get world ankle positions (using foot and torso anchors -+ 0*TODO: see if this is correct)
                Object rAnkleCurr = rAnkleJ.getClass().getMethod("getAnchor1").invoke(rAnkleJ);
                Object lAnkleCurr = lAnkleJ.getClass().getMethod("getAnchor1").invoke(lAnkleJ);
                Object rHipCurr = rHipJ.getClass().getMethod("getAnchor1").invoke(rHipJ);
                float rAnkleCurrX = rAnkleCurr.getClass().getField("x").getFloat(rAnkleCurr);
                float lAnkleCurrX = lAnkleCurr.getClass().getField("x").getFloat(lAnkleCurr);
                float rHipCurrX = rHipCurr.getClass().getField("x").getFloat(rHipCurr);

                // if right ankle joint is behind the right hip jiont
                // Set ankle motor speed to 1;
                // else speed 2
                if (rAnkleCurrX < rHipCurrX) {
                    setMotorSpeed(rAnkleJ, rAnkleSpeed2);
                } else {
                    setMotorSpeed(rAnkleJ, rAnkleSpeed1);
                }
                // if left ankle joint is behind RIGHT hip joint (weird it's the right one here too)
                // Set its motor speed to 1;
                // else speed 2;
                if (lAnkleCurrX < rHipCurrX) {
                    setMotorSpeed(lAnkleJ, lAnkleSpeed2);
                } else {
                    setMotorSpeed(lAnkleJ, lAnkleSpeed1);
                }
            }

            /* OP Keypress Stuff */
            if (o) {
                //Set speed 1 for knees
                // set l hip limits(-1 1)
                //set right hip limits (-1.3,0.7)
                setMotorSpeed(rKneeJ, rKneeSpeed2);
                setMotorSpeed(lKneeJ, lKneeSpeed2);
                setJointLowerBound(rHipJ, oRHipLimLo);
                setJointUpperBound(rHipJ, oRHipLimHi);
                setJointLowerBound(lHipJ, oLHipLimLo);
                setJointUpperBound(lHipJ, oLHipLimHi);
            } else if (p) {
                //Set speed 2 for knees
                // set L hip limits(-1.5,0.5)
                // set R hip limits(-0.8,1.2)
                setMotorSpeed(rKneeJ, rKneeSpeed1);
                setMotorSpeed(lKneeJ, lKneeSpeed1);
                setJointLowerBound(rHipJ, pRHipLimLo);
                setJointUpperBound(rHipJ, pRHipLimHi);
                setJointLowerBound(lHipJ, pLHipLimLo);
                setJointUpperBound(lHipJ, pLHipLimHi);
            } else {
                // Set knee speeds to 0
                //Joint limits not changed!!
                setMotorSpeed(rKneeJ, 0f);
                setMotorSpeed(lKneeJ, 0f);
            }

            // Step the world forward one timestep:
            world.getClass().getMethod("step", float.class, int.class).invoke(world, timestep, physIterations);


//            // Check to see if the contact listener has heard anything new since the last timestep.
//            boolean contactChanges =
//                    (boolean) contactListener.getClass().getMethod("hasAnythingChanged").invoke(contactListener);
//
//            // If changes have occurred, see if any are failure-worthy.
//            if (contactChanges) {
//                if (checkForBodyContact(headBody)
//                        || checkForBodyContact(rLArmBody)
//                        || checkForBodyContact(lLArmBody)
//                        || checkForBodyContact(torsoBody)
//                        || checkForBodyContact(rUArmBody)
//                        || checkForBodyContact(lUArmBody)) {
//                    isFailed = true;
//                } else if (failOnThighContact // Additional condition if we want thigh contact to cause failure.
//                        && (checkForBodyContact(rThighBody) || checkForBodyContact(lThighBody))) {
//                    isFailed = true;
//                }
//                if (checkForBodyContact(rFootBody)) {
//                    rFootDown = true;
//                }
//                if (checkForBodyContact(lFootBody)) {
//                    lFootDown = true;
//                }
//            }

            // Extra fail conditions besides contacts.
            if (!isFailed) { // Only bother checking if other failures haven't happened.
                float angle = (float) torsoBody.getClass().getMethod("getAngle").invoke(torsoBody);
                if (angle > torsoAngUpper || angle < torsoAngLower) { // Fail if torso angles get too far out of whack.
                    isFailed = true;
                }
            }
            timestepsSimulated++;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Determine whether a specific body is currently in a contacting state with the ground.
     * @param body Body to check.
     * @return True if this body is in contact with the ground, false if not.
     */
//    private boolean checkForBodyContact(Object body) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        return (boolean) contactListener.getClass().getMethod("isContacting", classLoader._Body).invoke(contactListener, body);
//    }

    /**
     * Step the game forward 1 timestep with the specified keys pressed.
     * @param keys 4 element array for whether the Q,W,O,P keys are pressed (true -> pressed).
     */
    public void step(boolean[] keys) {
        if (keys.length != 4) {
            throw new IndexOutOfBoundsException("Provided a key sequence to execute which did not contain 4 key " +
                    "booleans.");
        }
        step(keys[0], keys[1], keys[2], keys[3]);
    }

    /**
     * Get the current full state of the runner.
     */
    public synchronized State getCurrentState() {
        State st = null;
        try {
            st = new State(
                    getCurrentBodyState(torsoBody),
                    getCurrentBodyState(headBody),
                    getCurrentBodyState(rThighBody),
                    getCurrentBodyState(lThighBody),
                    getCurrentBodyState(rCalfBody),
                    getCurrentBodyState(lCalfBody),
                    getCurrentBodyState(rFootBody),
                    getCurrentBodyState(lFootBody),
                    getCurrentBodyState(rUArmBody),
                    getCurrentBodyState(lUArmBody),
                    getCurrentBodyState(rLArmBody),
                    getCurrentBodyState(lLArmBody),
                    getFailureStatus());
        } catch (RuntimeException e) {
            throw e;
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return st;
    }

    public Object[] getAllJoints() {
        return new Object[]{rHipJ, lHipJ, rKneeJ, lKneeJ, rAnkleJ, lAnkleJ, rShoulderJ, lShoulderJ, rElbowJ,
                lElbowJ, neckJ};
    }

    /**
     * Get a new StateVariable for a given body.
     */
    private StateVariable getCurrentBodyState(Object body) {
        StateVariable currentState = null;
        try {
            Object pos = body.getClass().getMethod("getPosition").invoke(body);
            float x = pos.getClass().getField("x").getFloat(pos);
            float y = pos.getClass().getField("y").getFloat(pos);
            float th = (float) body.getClass().getMethod("getAngle").invoke(body);

            Object vel = body.getClass().getMethod("getLinearVelocity").invoke(body);
            float dx = vel.getClass().getField("x").getFloat(vel);
            float dy = vel.getClass().getField("y").getFloat(vel);
            float dth = (float) body.getClass().getMethod("getAngularVelocity").invoke(body);
            currentState = new StateVariable(x, y, th, dx, dy, dth);
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return currentState;
    }
    /**
     * Set the visible state of the runner (positions, velocities). Does not change the internal solver states, and
     * can be a dangerous command to use. Determinism is out the window here.
     *
     * @param state {@link State} to set this game's runner at.
     */
    public void setState(State state) {
        setBodyToStateVariable(rFootBody, state.rfoot);
        setBodyToStateVariable(lFootBody, state.lfoot);

        setBodyToStateVariable(rThighBody, state.rthigh);
        setBodyToStateVariable(lThighBody, state.lthigh);

        setBodyToStateVariable(rCalfBody, state.rcalf);
        setBodyToStateVariable(lCalfBody, state.lcalf);

        setBodyToStateVariable(rUArmBody, state.ruarm);
        setBodyToStateVariable(lUArmBody, state.luarm);

        setBodyToStateVariable(rLArmBody, state.rlarm);
        setBodyToStateVariable(lLArmBody, state.llarm);

        setBodyToStateVariable(headBody, state.head);
        setBodyToStateVariable(torsoBody, state.body);
    }

    /**
     * Set an individual body to a specified {@link StateVariable}. This sets both positions and velocities.
     *
     * @param body          Body to set the state of.
     * @param stateVariable Full state to assign to that body.
     */
    private void setBodyToStateVariable(Object body, StateVariable stateVariable) {
        try {
            body.getClass().getMethod("setXForm", classLoader._Vec2, float.class).invoke(body, makeVec2(stateVariable.getX(),
                    stateVariable.getY()), stateVariable.getTh());
            body.getClass().getMethod("setLinearVelocity", classLoader._Vec2).invoke(body, makeVec2(stateVariable.getDx(),
                    stateVariable.getDy()));
            body.getClass().getMethod("setAngularVelocity", float.class).invoke(body, stateVariable.getDth());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the transform associated with this State. Note that these transforms can ONLY be used with this instance
     * of GameThreadSafe.
     */
    public Object[] getXForms(State st) {
        Object[] transforms = new Object[13];
        try {
            transforms[0] = getXForm(st.body);
            transforms[1] = getXForm(st.head);
            transforms[2] = getXForm(st.rfoot);
            transforms[3] = getXForm(st.lfoot);
            transforms[4] = getXForm(st.rcalf);
            transforms[5] = getXForm(st.lcalf);
            transforms[6] = getXForm(st.rthigh);
            transforms[7] = getXForm(st.lthigh);
            transforms[8] = getXForm(st.ruarm);
            transforms[9] = getXForm(st.luarm);
            transforms[10] = getXForm(st.rlarm);
            transforms[11] = getXForm(st.llarm);
            transforms[12] = getXForm(new StateVariable(0, trackPosY, 0, 0, 0, 0)); // Hardcoded for track.
            // Offset by 20 because its now a box.
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transforms;
    }

    /**
     * Get the transform associated with this body's state variables. Note that these transforms can ONLY be used
     * with this instance of GameThreadSafe.
     */
    public Object getXForm(StateVariable sv) {
        Object xf = null;
        try {
            xf = classLoader._XForm.getConstructor().newInstance();
            Object pos = xf.getClass().getField("position").get(xf); // Position
            pos.getClass().getField("x").setFloat(pos, sv.getX());
            pos.getClass().getField("y").setFloat(pos, sv.getY());
            Object R = xf.getClass().getField("R").get(xf); // Rotation
            R.getClass().getMethod("set", float.class).invoke(R, sv.getTh());
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        return xf;
    }

    /**
     * Draw this game's runner. Must provide scaling from game units to pixels, as well as pixel offsets in x and y.
     */
    public void draw(Graphics g, float scaling, int xOffset, int yOffset) {
        try {
            g.setColor(mainRunnerColor);
            ((Graphics2D) g).setStroke(mainRunnerStroke);
            Object newBody = world.getClass().getMethod("getBodyList").invoke(world);
            float currTorsoPos = torsoPosX;
            while (newBody != null) {
                Object torsoPos = torsoBody.getClass().getMethod("getPosition").invoke(torsoBody);
                currTorsoPos = torsoPos.getClass().getField("x").getFloat(torsoPos);
                int xOffsetPixels = -(int) (scaling * torsoPosX) + xOffset; // Basic offset, plus centering x on torso.

                Object newFixture = newBody.getClass().getMethod("getShapeList").invoke(newBody);

                while (newFixture != null) {
                    // Most links are polygon shapes
                    Object fixtureType = newFixture.getClass().getMethod("getType").invoke(newFixture);
                    if (fixtureType == classLoader._ShapeType.getField("POLYGON_SHAPE").get(null)) {
                        g.setColor(mainRunnerColor);
                        ((Graphics2D) g).setStroke(mainRunnerStroke);
                        Object newShape = classLoader._PolygonShape.cast(newFixture); // PolygonShape
                        Object[] shapeVerts = (Object[]) newShape.getClass().getField("m_vertices").get(newShape); //
                        // Vec2[]

                        for (int k = 0; k < newShape.getClass().getField("m_vertexCount").getInt(newShape); k++) {
                            Object xf = newBody.getClass().getMethod("getXForm").invoke(newBody); // XForm

                            Object ptA = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, xf, shapeVerts[k]); // Vec2
                            Object ptB = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, xf,
                                    shapeVerts[(k + 1) % newShape.getClass().getField("m_vertexCount").getInt(newShape)]); // Vec2
                            g.drawLine((int) (scaling * (ptA.getClass().getField("x").getFloat(ptA) - currTorsoPos)) + xOffsetPixels,
                                    (int) (scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
                                    (int) (scaling * (ptB.getClass().getField("x").getFloat(ptB) - currTorsoPos)) + xOffsetPixels,
                                    (int) (scaling * ptB.getClass().getField("y").getFloat(ptB)) + yOffset);
                        }
                    } else if (fixtureType == classLoader._ShapeType.getField("CIRCLE_SHAPE").get(null)) { // Basically just head
                        g.setColor(mainRunnerColor);
                        ((Graphics2D) g).setStroke(mainRunnerStroke);
                        Object newShape = classLoader._CircleShape.cast(newFixture); // CircleShape
                        float radius = newShape.getClass().getField("m_radius").getFloat(newShape);
                        Object pos = newBody.getClass().getMethod("getPosition").invoke(newBody);

                        g.drawOval((int) (scaling * (pos.getClass().getField("x").getFloat(pos) - radius - currTorsoPos) + xOffsetPixels),
                                (int) (scaling * (pos.getClass().getField("y").getFloat(pos) - radius) + yOffset),
                                (int) (scaling * radius * 2),
                                (int) (scaling * radius * 2));

                    } else if (fixtureType == classLoader._ShapeType.getField("EDGE_SHAPE").get(null)) { // The track.
                        g.setColor(mainRunnerColor);
                        ((Graphics2D) g).setStroke(normalStroke);
                        Object newShape = newFixture.getClass().cast(classLoader._EdgeShape); // EdgeShape
                        Object trans = newBody.getClass().getMethod("getXForm").invoke(newBody); // XForm

                        Object vertex1 = newShape.getClass().getMethod("getVertex1").invoke(newShape); // Vec2
                        Object vertex2 = newShape.getClass().getMethod("getVertex2").invoke(newShape);
                        Object vertex3 = newShape.getClass().getMethod("getVertex3").invoke(newShape);


                        Object ptA = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, trans, vertex1); // Vec2
                        Object ptB = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, trans, vertex2);
                        Object ptC = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, trans, vertex3);

                        g.drawLine((int) (scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffsetPixels,
                                (int) (scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
                                (int) (scaling * ptB.getClass().getField("x").getFloat(ptB)) + xOffsetPixels,
                                (int) (scaling * ptB.getClass().getField("y").getFloat(ptB)) + yOffset);
                        g.drawLine((int) (scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffsetPixels,
                                (int) (scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
                                (int) (scaling * ptC.getClass().getField("x").getFloat(ptC)) + xOffsetPixels,
                                (int) (scaling * ptC.getClass().getField("y").getFloat(ptC)) + yOffset);

                    } else {
                        System.out.println("Shape type unknown.");
                    }
                    newFixture = newFixture.getClass().getMethod("getNext").invoke(newFixture);
                }
                newBody = newBody.getClass().getMethod("getNext").invoke(newBody);
            }

            //This draws the "road" markings to show that the ground is moving relative to the dude.
            int markingWidth = 2000;
            for (int i = 0; i < markingWidth / 69; i++) {
                g.drawString("_", ((-(int) (scaling * currTorsoPos) - i * 70) % markingWidth) + markingWidth,
                        yOffset + (int) (scaling * 9.2f));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Draw the runner at a specified set of transforms.
     */
    public void drawExtraRunner(Graphics2D g, State st, String label, float scaling, int xOffset, int yOffset,
                                Color drawColor, Stroke stroke) {
        drawExtraRunner(g, getXForms(st), label, scaling, xOffset, yOffset, drawColor, stroke);
    }

    /**
     * Draw the runner at a specified set of transforms..
     */
    public void drawExtraRunner(Graphics2D g, Object[] transforms, String label, float scaling, int xOffset,
                                int yOffset, Color drawColor, Stroke stroke) {
        try {
            g.setColor(drawColor);
            Object bodPos = transforms[1].getClass().getField("position").get(transforms[1]);
            g.drawString(label, xOffset + (int) (bodPos.getClass().getField("x").getFloat(bodPos) * scaling) - 20,
                    yOffset - 75);

            for (int i = 0; i < shapeList.size(); i++) {
                g.setColor(drawColor);
                g.setStroke(stroke);

                // Most links are polygon shapes
                Object shape = shapeList.get(i);
                Object fixtureType = shape.getClass().getMethod("getType").invoke(shape);

                if (fixtureType == classLoader._ShapeType.getField("POLYGON_SHAPE").get(null)) {
                    // Ground is black regardless.
                    Object filter = shape.getClass().getField("m_filter").get(shape);
                    if (filter.getClass().getField("groupIndex").getInt(filter) == 1) {
                        g.setColor(Color.BLACK);
                        g.setStroke(normalStroke);
                    }

                    Object polygonShape = classLoader._PolygonShape.cast(shape);
                    Object[] polyVerts = (Object[]) polygonShape.getClass().getField("m_vertices").get(polygonShape);
                    int vertexCount = (int) polygonShape.getClass().getMethod("getVertexCount").invoke(polygonShape);
                    for (int j = 0; j < vertexCount; j++) { // Loop through polygon vertices and draw lines between
                        // them.
                        Object ptA =
                                classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, transforms[i], polyVerts[j]); //
                        // Vec2
                        Object ptB = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, transforms[i],
                                polyVerts[(j + 1) % vertexCount]); // Vec2
                        g.drawLine((int) (scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffset,
                                (int) (scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
                                (int) (scaling * ptB.getClass().getField("x").getFloat(ptB)) + xOffset,
                                (int) (scaling * ptB.getClass().getField("y").getFloat(ptB)) + yOffset);
                    }
                } else if (fixtureType == classLoader._ShapeType.getField("CIRCLE_SHAPE").get(null)) { // Basically just head

                    Object circleShape = classLoader._CircleShape.cast(shape);
                    float radius = (float) circleShape.getClass().getMethod("getRadius").invoke(circleShape);
                    Object circleCenter = classLoader._XForm.getMethod("mul", classLoader._XForm, classLoader._Vec2).invoke(null, transforms[i],
                            circleShape.getClass().getMethod("getLocalPosition").invoke(circleShape)); // Vec2
                    g.drawOval((int) (scaling * (circleCenter.getClass().getField("x").getFloat(circleCenter) - radius) + xOffset),
                            (int) (scaling * (circleCenter.getClass().getField("y").getFloat(circleCenter) - radius) + yOffset),
                            (int) (scaling * radius * 2),
                            (int) (scaling * radius * 2));

//                } else if (fixtureType == _ShapeType.getField("EDGE_SHAPE").get(null)) {
//                    // The track.
                } else {
                    System.out.println("Shape type unknown.");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if the game has reached a failed state.
     *
     * @return Failure status of this game instance.
     */
    public synchronized boolean getFailureStatus() {
        return isFailed;
    }

    /**
     * Check whether this game has completed its makeNewWorld process.
     *
     * @return Whether this game has finished being set up. True - yes, false - no.
     */
    public boolean isGameInitialized() {
        return initialized;
    }

    /**
     * Check if the right foot is touching the ground.
     *
     * @return Whether the right foot is touching the ground (true/false).
     */
    public boolean isRightFootDown() {
        return rFootDown;
    }

    /**
     * Check if the left foot is touching the ground.
     *
     * @return Whether the left foot is touching the ground (true/false).
     */
    public boolean isLeftFootDown() {
        return lFootDown;
    }

    /**
     * Get the QWOP initial condition. Good way to give the root node a state.
     *
     * @return The initial state of the QWOP runner.
     */
    public static State getInitialState() {
        return initState;
    }

    /**
     * Get the number of timesteps simulated so far this game.
     *
     * @return Number of timesteps simulated so far in this game.
     */
    public long getTimestepsSimulatedThisGame() {
        return timestepsSimulated;
    }

    /**
     * Take a state generated by the real Flash game and convert angles to work with my simulated version of QWOP.
     *
     * @param realQWOPState A state using angle offsets found in the real Flash QWOP game. This will be changed in
     *                      place to have the offsets used in this simulated version.
     */
    public static void adjustRealQWOPStateToSimState(State realQWOPState) {
        //TODO find a better way to manage access to th.
//        realQWOPState.body.th += torsoAngAdj;
//        realQWOPState.head.th += headAngAdj;
//        realQWOPState.rthigh.th += rThighAngAdj;
//        realQWOPState.lthigh.th += lThighAngAdj;
//        realQWOPState.rcalf.th += rCalfAngAdj;
//        realQWOPState.lcalf.th += lCalfAngAdj;
//        realQWOPState.ruarm.th += rUArmAngAdj;
//        realQWOPState.luarm.th += lUArmAngAdj;
//        realQWOPState.rlarm.th += rLArmAngAdj;
//        realQWOPState.llarm.th += lLArmAngAdj;
    }

    /**
     * Print methods and fields of the object for debugging all the reflected crap in here.
     *
     * @param obj Object to print the fields and methods of.
     */
    @SuppressWarnings("unused")
    private void debugPrintObjectInfo(Object obj) {
        for (Field m : obj.getClass().getFields()) {
            System.out.println(m.getName());
        }
        for (Method m : obj.getClass().getMethods()) {
            System.out.println(m.getName());
        }
    }

    /**
     * Convenience method to avoid dealing with reflection in the code constantly.
     */
    private Object makeBodyDef(float positionX, float positionY, float angle, float mass, float inertia) {
        Object bodyDef = null;
        try {
            bodyDef = classLoader._BodyDef.getDeclaredConstructor().newInstance();
            Object massData = makeMassData(mass, inertia);
            Object position = makeVec2(positionX, positionY);
            bodyDef.getClass().getField("massData").set(bodyDef, massData);
            bodyDef.getClass().getField("position").set(bodyDef, position);
            bodyDef.getClass().getField("angle").setFloat(bodyDef, angle);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return bodyDef;
    }

    private Object makeBody(Object world, Object bodyDef, Object shape) {
        Object body = null;
        try {
            body = world.getClass().getMethod("createBody", classLoader._BodyDef).invoke(world, bodyDef);
            body.getClass().getMethod("createShape", classLoader._ShapeDef).invoke(body, shape);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return body;
    }

    /**
     * Make a joint definition.
     */
    private Object makeJointDef(Object body1, Object body2, float jointPosX, float jointPosY, float lowerAngle,
                                float upperAngle, float maxTorque, float motorSpeed, boolean enableLimit,
                                boolean enableMotor, boolean collideConnected) {
        Object jDef = null;
        try {
            jDef = classLoader._RevoluteJointDef.getDeclaredConstructor().newInstance();
            Object posVec = makeVec2(jointPosX, jointPosY);

            jDef.getClass().getMethod("initialize", classLoader._Body, classLoader._Body, classLoader._Vec2).invoke(jDef, body1, body2, posVec);
            jDef.getClass().getField("lowerAngle").setFloat(jDef, lowerAngle);
            jDef.getClass().getField("upperAngle").setFloat(jDef, upperAngle);
            jDef.getClass().getField("maxMotorTorque").setFloat(jDef, maxTorque);
            jDef.getClass().getField("motorSpeed").setFloat(jDef, motorSpeed);

            jDef.getClass().getField("enableLimit").setBoolean(jDef, enableLimit);
            jDef.getClass().getField("enableMotor").setBoolean(jDef, enableMotor);
            jDef.getClass().getField("collideConnected").setBoolean(jDef, collideConnected);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return jDef;
    }

    /**
     * Convenience method to avoid dealing with reflection in the code constantly.
     */
    private Object makeBoxShapeDef(float boxX, float boxY, float restitution, float friction, float density,
                                   int groupIdx) {
        Object boxShapeDef = null;
        try {
            boxShapeDef = classLoader._PolygonDef.getDeclaredConstructor().newInstance();
            boxShapeDef.getClass().getMethod("setAsBox", float.class, float.class).invoke(boxShapeDef, boxX, boxY);
            boxShapeDef.getClass().getField("friction").setFloat(boxShapeDef, friction);
            boxShapeDef.getClass().getField("density").setFloat(boxShapeDef, density);
            boxShapeDef.getClass().getField("restitution").setFloat(boxShapeDef, restitution);
            Object shapeFilter = boxShapeDef.getClass().getField("filter").get(boxShapeDef);
            shapeFilter.getClass().getField("groupIndex").setInt(shapeFilter, groupIdx);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        return boxShapeDef;
    }

    /**
     * Convenience method to avoid dealing with reflection in the code constantly.
     */
    private Object makeBoxShapeDef(float boxX, float boxY, float friction, float density, int groupIdx) {
        Object boxShapeDef = null;
        try {
            boxShapeDef = classLoader._PolygonDef.getDeclaredConstructor().newInstance();
            boxShapeDef.getClass().getMethod("setAsBox", float.class, float.class).invoke(boxShapeDef, boxX, boxY);
            boxShapeDef.getClass().getField("friction").setFloat(boxShapeDef, friction);
            boxShapeDef.getClass().getField("density").setFloat(boxShapeDef, density);
            Object shapeFilter = boxShapeDef.getClass().getField("filter").get(boxShapeDef);
            shapeFilter.getClass().getField("groupIndex").setInt(shapeFilter, groupIdx);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        return boxShapeDef;
    }

    /**
     * Convenience method to avoid dealing with reflection in the code constantly.
     */
    private Object makeCircleShapeDef(float radius, float friction, float density, int groupIdx) {
        Object circleShapeDef = null;
        try {
            circleShapeDef = classLoader._CircleDef.getDeclaredConstructor().newInstance();
            circleShapeDef.getClass().getField("radius").setFloat(circleShapeDef, radius);
            circleShapeDef.getClass().getField("friction").setFloat(circleShapeDef, friction);
            circleShapeDef.getClass().getField("density").setFloat(circleShapeDef, density);
            Object shapeFilter = circleShapeDef.getClass().getField("filter").get(circleShapeDef);
            shapeFilter.getClass().getField("groupIndex").setInt(shapeFilter, groupIdx);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return circleShapeDef;
    }

    /**
     * Convenience method to avoid dealing with reflection in the code constantly.
     */
    private Object makeMassData(float mass, float inertia) {
        Object md = null;
        try {
            md = classLoader._MassData.getDeclaredConstructor().newInstance();
            md.getClass().getField("mass").setFloat(md, mass);
            md.getClass().getField("I").setFloat(md, inertia);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return md;
    }

    /**
     * Convenience method to avoid dealing with reflection in the code constantly.
     */
    private Object makeVec2(float x, float y) {
        Object vec = null;
        try {
            vec = classLoader._Vec2.getConstructor(float.class, float.class).newInstance(x, y);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return vec;
    }

    /**
     * Call once to initialize a lot of shape definitions which only need to be created once.
     */
    private void oneTimeSetup() {
        /*
         * Make the bodies and collision shapes
         */
        /* TRACK */
        trackDef = makeBodyDef(trackPosX, trackPosY, 0, 0, 0);
        trackShape = makeBoxShapeDef(trackXDim, trackYDim, trackRest, trackFric, 0, 1);

        /* FEET */
        //Create the fixture shapes, IE collision shapes

        int BODY_GROUP = -1; // Filters collisions. Prevents body parts from hitting other body parts.

        rFootDef = makeBodyDef(rFootPosX, rFootPosY, rFootAng, rFootMass, rFootInertia);
        rFootShape = makeBoxShapeDef(rFootL / 2f, rFootH / 2f, rFootFric, rFootDensity, BODY_GROUP);
        lFootDef = makeBodyDef(lFootPosX, lFootPosY, lFootAng, lFootMass, lFootInertia);
        lFootShape = makeBoxShapeDef(lFootL / 2f, lFootH / 2f, lFootFric, lFootDensity, BODY_GROUP);

        /* CALVES */
        rCalfDef = makeBodyDef(rCalfPosX, rCalfPosY, rCalfAng + rCalfAngAdj, rCalfMass, rCalfInertia);
        rCalfShape = makeBoxShapeDef(rCalfW / 2f, rCalfL / 2f, rCalfFric, rCalfDensity, BODY_GROUP);
        lCalfDef = makeBodyDef(lCalfPosX, lCalfPosY, lCalfAng + lCalfAngAdj, lCalfMass, lCalfInertia);
        lCalfShape = makeBoxShapeDef(lCalfW / 2f, lCalfL / 2f, lCalfFric, lCalfDensity, BODY_GROUP);

        /* THIGHS */
        rThighDef = makeBodyDef(rThighPosX, rThighPosY, rThighAng + rThighAngAdj, rThighMass, rThighInertia);
        rThighShape = makeBoxShapeDef(rThighW / 2f, rThighL / 2f, rThighFric, rThighDensity, BODY_GROUP);
        lThighDef = makeBodyDef(lThighPosX, lThighPosY, lThighAng + lThighAngAdj, lThighMass, lThighInertia);
        lThighShape = makeBoxShapeDef(lThighW / 2f, lThighL / 2f, lThighFric, lThighDensity, BODY_GROUP);

        /* TORSO */
        torsoDef = makeBodyDef(torsoPosX, torsoPosY, torsoAng + torsoAngAdj, torsoMass, torsoInertia);
        torsoShape = makeBoxShapeDef(torsoW / 2f, torsoL / 2f, torsoFric, torsoDensity, BODY_GROUP);

        /* HEAD */
        headDef = makeBodyDef(headPosX, headPosY, headAng + headAngAdj, headMass, headInertia);
        headShape = makeCircleShapeDef(headR, headFric, headDensity, BODY_GROUP);

        /* UPPER ARMS */
        rUArmDef = makeBodyDef(rUArmPosX, rUArmPosY, rUArmAng + rUArmAngAdj, rUArmMass, rUArmInertia);
        rUArmShape = makeBoxShapeDef(rUArmW / 2f, rUArmL / 2f, rUArmFric, rUArmDensity, BODY_GROUP);
        lUArmDef = makeBodyDef(lUArmPosX, lUArmPosY, lUArmAng + lUArmAngAdj, lUArmMass, lUArmInertia);
        lUArmShape = makeBoxShapeDef(lUArmW / 2f, lUArmL / 2f, lUArmFric, lUArmDensity, BODY_GROUP);

        /* LOWER ARMS */
        rLArmDef = makeBodyDef(rLArmPosX, rLArmPosY, rLArmAng + rLArmAngAdj, rLArmMass, rLArmInertia);
        rLArmShape = makeBoxShapeDef(rLArmW / 2f, rLArmL / 2f, rLArmFric, rLArmDensity, BODY_GROUP);
        lLArmDef = makeBodyDef(lLArmPosX, lLArmPosY, lLArmAng + lLArmAngAdj, lLArmMass, lLArmInertia);
        lLArmShape = makeBoxShapeDef(lLArmW / 2f, lLArmL / 2f, lLArmFric, lLArmDensity, BODY_GROUP);
    }

    /**
     * Make (or remake) the world with all the body parts at their initial locations.
     */
    public void makeNewWorld() {
        try {
            isFailed = false;
            rFootDown = false;
            lFootDown = false;
            timestepsSimulated = 0;

            /* World setup */
            Constructor<?> aabbCons = classLoader._AABB.getConstructor(classLoader._Vec2, classLoader._Vec2);
            Object vecAABBLower = makeVec2(aabbMinX, aabbMinY);
            Object vecAABBUpper = makeVec2(aabbMaxX, aabbMaxY);
            Object aabbWorld = aabbCons.newInstance(vecAABBLower, vecAABBUpper);
            Object vecGrav = makeVec2(0f, gravityMagnitude);

            Constructor<?> worldCons = classLoader._World.getConstructor(classLoader._AABB, classLoader._Vec2, boolean.class);
            world = worldCons.newInstance(aabbWorld, vecGrav, true);

            // World settings:
            world.getClass().getMethod("setWarmStarting", boolean.class).invoke(world, true);
            world.getClass().getMethod("setPositionCorrection", boolean.class).invoke(world, true);
            world.getClass().getMethod("setContinuousPhysics", boolean.class).invoke(world, true);

            /* Body setup */
            Object trackBody = world.getClass().getMethod("createBody", classLoader._BodyDef).invoke(world, trackDef);
            trackBody.getClass().getMethod("createShape", classLoader._ShapeDef).invoke(trackBody, trackShape);

            // NOTE: The order of creating bodies actually changes the answers slightly!! This is really dumb, but will
            // affect us if we are trying to match the single and multithreaded version.

            rFootBody = makeBody(world, rFootDef, rFootShape);
            lFootBody = makeBody(world, lFootDef, lFootShape);

            rCalfBody = makeBody(world, rCalfDef, rCalfShape);
            lCalfBody = makeBody(world, lCalfDef, lCalfShape);

            rThighBody = makeBody(world, rThighDef, rThighShape);
            lThighBody = makeBody(world, lThighDef, lThighShape);

            rUArmBody = makeBody(world, rUArmDef, rUArmShape);
            lUArmBody = makeBody(world, lUArmDef, lUArmShape);

            rLArmBody = makeBody(world, rLArmDef, rLArmShape);
            lLArmBody = makeBody(world, lLArmDef, lLArmShape);

            torsoBody = makeBody(world, torsoDef, torsoShape);
            headBody = makeBody(world, headDef, headShape);

            if (shapeList == null) { // Grab shapes one time for the sake of plotting later.
                shapeList = new ArrayList<>();
                shapeList.add(torsoBody.getClass().getMethod("getShapeList").invoke(torsoBody));
                shapeList.add(headBody.getClass().getMethod("getShapeList").invoke(headBody));
                shapeList.add(rFootBody.getClass().getMethod("getShapeList").invoke(rFootBody));
                shapeList.add(lFootBody.getClass().getMethod("getShapeList").invoke(lFootBody));
                shapeList.add(rCalfBody.getClass().getMethod("getShapeList").invoke(rCalfBody));
                shapeList.add(lCalfBody.getClass().getMethod("getShapeList").invoke(lCalfBody));
                shapeList.add(rThighBody.getClass().getMethod("getShapeList").invoke(rThighBody));
                shapeList.add(lThighBody.getClass().getMethod("getShapeList").invoke(lThighBody));
                shapeList.add(rUArmBody.getClass().getMethod("getShapeList").invoke(rUArmBody));
                shapeList.add(lUArmBody.getClass().getMethod("getShapeList").invoke(lUArmBody));
                shapeList.add(rLArmBody.getClass().getMethod("getShapeList").invoke(rLArmBody));
                shapeList.add(lLArmBody.getClass().getMethod("getShapeList").invoke(lLArmBody));
                shapeList.add(trackBody.getClass().getMethod("getShapeList").invoke(trackBody));
            }

            /* Joint makeNewWorld */
            rAnkleJDef = makeJointDef(rFootBody, rCalfBody, rAnklePosX, rAnklePosY, -0.5f, 0.5f, 2000f, 0f,
                    true, false, false);
            rAnkleJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, rAnkleJDef);
            lAnkleJDef = makeJointDef(lFootBody, lCalfBody, lAnklePosX, lAnklePosY, -0.5f, 0.5f, 2000f, 0f,
                    true, false, false);
            lAnkleJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, lAnkleJDef);

            rKneeJDef = makeJointDef(rCalfBody, rThighBody, rKneePosX, rKneePosY, -1.3f, 0.3f, 3000f, 0f, true
                    , true, false);
            rKneeJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, rKneeJDef);
            lKneeJDef = makeJointDef(lCalfBody, lThighBody, lKneePosX, lKneePosY, -1.6f, 0.0f, 3000f, 0f, true
                    , true, false);
            lKneeJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, lKneeJDef);

            rHipJDef = makeJointDef(rThighBody, torsoBody, rHipPosX, rHipPosY, -1.3f, 0.7f, 6000f, 0f, true,
                    true, false);
            rHipJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, rHipJDef);
            lHipJDef = makeJointDef(lThighBody, torsoBody, lHipPosX, lHipPosY, -1.5f, 0.5f, 6000f, 0f, true,
                    true, false);
            lHipJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, lHipJDef);

            neckJDef = makeJointDef(headBody, torsoBody, neckPosX, neckPosY, -0.5f, 0.0f, 1000f, 0f, true,
                    true, false);
            neckJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, neckJDef);

            rShoulderJDef = makeJointDef(rUArmBody, torsoBody, rShoulderPosX, rShoulderPosY, -0.5f, 1.5f,
                    1000f, 0f, true, true, false);
            rShoulderJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, rShoulderJDef);
            lShoulderJDef = makeJointDef(lUArmBody, torsoBody, lShoulderPosX, lShoulderPosY, -2f, 0.0f, 1000f,
                    0f, true, true, false);
            lShoulderJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, lShoulderJDef);

            rElbowJDef = makeJointDef(rLArmBody, rUArmBody, rElbowPosX, rElbowPosY, -0.1f, 0.5f, 0f, 10f, true
                    , true, false);
            rElbowJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, rElbowJDef);
            lElbowJDef = makeJointDef(lLArmBody, lUArmBody, lElbowPosX, lElbowPosY, -0.1f, 0.5f, 0f, 10f, true
                    , true, false);
            lElbowJ = world.getClass().getMethod("createJoint", classLoader._JointDef).invoke(world, lElbowJDef);


           // contactListener = classLoader._GameThreadSafeContactListener.getConstructor().newInstance();

//            world.getClass().getMethod("setContactListener", classLoader._ContactListener).invoke(world, contactListener);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }

        initialized = true;
    }
}
