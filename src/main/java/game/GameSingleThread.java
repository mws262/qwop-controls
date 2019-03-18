package game;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.XForm;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.annotations.Flat;

import java.awt.*;
import java.io.*;
import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReentrantLock;

import static game.GameConstants.*;

/**
 * NOTE: There can only be one of these! Call getInstance() to retrieve it. Callers will acquire the lock for the
 * game and must releaseGame after using for others to use.
 * This creates the QWOP game using the Box2D library. This operates on the primary classloader. This means that
 * multiple instances of this class will interfere with others due to static information inside Box2D.
 * {@link GameThreadSafe} uses a separate classloader for each instance and can be done in multithreaded applications.
 * However, dealing with all the reflection in GameThreadSafe is really annoying. Hence, this class is more readable.
 *
 * @author matt
 */
@SuppressWarnings("Duplicates")
public class GameSingleThread implements IGame, Serializable {

    /**
     * This is the only instance of the single threaded game allowed. If multiple copies run, they interfere. Run
     * {@link GameSingleThread#getInstance()}
     */
    private static GameSingleThread instance;

    /**
     * Lock allowing only one thread to access the game instance at a time.
     */
    private static ReentrantLock lock = new ReentrantLock();

    /**
     * Keep track of sim stats since beginning of execution.
     **/
    private long timestepsSimulated = 0;

    /**
     * Box2D world to be populated for QWOP.
     **/
    private World world;

    /**
     * Has this game reached failure conditions?
     **/
    private boolean isFailed = false;

    /**
     * TODO: This is a hack.
     * After loading, the reference to this GameSingleThread may be invalid, and this should throw an exception if
     * the user continues to access. This is mostly because I can't figure out loading such that all the fields here
     * are replaced correctly.
     */
    transient private boolean invalidated = false;

    /**
     * Should enclose the entire area we want collision checked.
     **/
    private static final AABB worldAABB = new AABB(new Vec2(aabbMinX, aabbMinY), new Vec2(aabbMaxX, aabbMaxY));

    /* Individual body objects */
    private Body rFootBody, lFootBody, rCalfBody, lCalfBody, rThighBody, lThighBody, torsoBody, rUArmBody, lUArmBody,
            rLArmBody, lLArmBody, headBody, trackBody;

    private Body[] allBodies;

    /* Joint Definitions */
    private RevoluteJointDef rHipJDef, lHipJDef, rKneeJDef, lKneeJDef, rAnkleJDef, lAnkleJDef, rShoulderJDef,
            lShoulderJDef, rElbowJDef, lElbowJDef, neckJDef;

    /* Joint objects */
    private RevoluteJoint rHipJ, lHipJ, rKneeJ, lKneeJ, rAnkleJ, lAnkleJ, rShoulderJ, lShoulderJ, rElbowJ, lElbowJ,
            neckJ;

    /**
     * Filters collisions. Prevents body parts from hitting other body parts.
     **/
    private static final int BODY_GROUP = -1;

    /**
     * Gravity vector. Positive since -y is up.
     **/
    private static final Vec2 gravity = new Vec2(0, gravityMagnitude);

    private static final Vec2 rFootPos = new Vec2(rFootPosX, rFootPosY), lFootPos = new Vec2(lFootPosX, lFootPosY),
            rCalfPos = new Vec2(rCalfPosX, rCalfPosY), lCalfPos = new Vec2(lCalfPosX, lCalfPosY),
            rThighPos = new Vec2(rThighPosX, rThighPosY), lThighPos = new Vec2(lThighPosX, lThighPosY),
            torsoPos = new Vec2(torsoPosX, torsoPosY),
            headPos = new Vec2(headPosX, headPosY),
            rUArmPos = new Vec2(rUArmPosX, rUArmPosY),
            lUArmPos = new Vec2(lUArmPosX, lUArmPosY),
            rLArmPos = new Vec2(rLArmPosX, rLArmPosY),
            lLArmPos = new Vec2(lLArmPosX, lLArmPosY);

    /* Joints Positions*/
    private static final Vec2 rAnklePos = new Vec2(rAnklePosX, rAnklePosY),
            lAnklePos = new Vec2(lAnklePosX, lAnklePosY),
            rKneePos = new Vec2(rKneePosX, rKneePosY),
            lKneePos = new Vec2(lKneePosX, lKneePosY),
            rHipPos = new Vec2(rHipPosX, rHipPosY),
            lHipPos = new Vec2(lHipPosX, lHipPosY),
            rShoulderPos = new Vec2(rShoulderPosX, rShoulderPosY),
            lShoulderPos = new Vec2(lShoulderPosX, lShoulderPosY),
            rElbowPos = new Vec2(rElbowPosX, rElbowPosY),
            lElbowPos = new Vec2(lElbowPosX, lElbowPosY),
            neckPos = new Vec2(neckPosX, neckPosY);

    /**
     * List of shapes for use by graphics stuff. Making it static -- IE, assuming that in multiple games, the runner doesn't change shape.
     **/
    private static Shape[] shapeList = new Shape[13];

    private static final BodyDef trackDef = new BodyDef(),
            rFootDef = new BodyDef(),
            lFootDef = new BodyDef(),
            rCalfDef = new BodyDef(),
            lCalfDef = new BodyDef(),
            rThighDef = new BodyDef(),
            lThighDef = new BodyDef(),
            torsoDef = new BodyDef(),
            headDef = new BodyDef(),
            rUArmDef = new BodyDef(),
            lUArmDef = new BodyDef(),
            rLArmDef = new BodyDef(),
            lLArmDef = new BodyDef();

    private static final PolygonDef trackShape = new PolygonDef(),
            rFootShape = new PolygonDef(),
            lFootShape = new PolygonDef(),
            rCalfShape = new PolygonDef(),
            lCalfShape = new PolygonDef(),
            rThighShape = new PolygonDef(),
            lThighShape = new PolygonDef(),
            torsoShape = new PolygonDef(),
            rUArmShape = new PolygonDef(),
            lUArmShape = new PolygonDef(),
            rLArmShape = new PolygonDef(),
            lLArmShape = new PolygonDef();

    private static final CircleDef headShape = new CircleDef();

    private static final MassData rFootMassData = new MassData(),
            lFootMassData = new MassData(),
            rCalfMassData = new MassData(),
            lCalfMassData = new MassData(),
            rThighMassData = new MassData(),
            lThighMassData = new MassData(),
            torsoMassData = new MassData(),
            headMassData = new MassData(),
            rUArmMassData = new MassData(),
            lUArmMassData = new MassData(),
            rLArmMassData = new MassData(),
            lLArmMassData = new MassData();

    /**
     * Initial runner state.
     **/
    private static State initState;
    // the other static assignments to avoid null pointers.

    /** Can turn off feet (just leg stumps) for trying stuff out. **/
    private static boolean noFeet = false;

    /** Listens for collisions between any body part and the ground. **/
    private CollisionListener collisionListener = new CollisionListener();

    /** Should the game be marked as failed if the thighs touch the ground? (happens with knees touching the ground. **/
    public static boolean failOnThighContact = false;

    /**
     * Normal stroke for line drawing.
     **/
    private static final Stroke normalStroke = new BasicStroke(0.5f);

    /**
     * For faster serialization.
     */
    private static FSTConfiguration fstConfiguration;

    // Make the single instance of this game!
    static {
        instance = new GameSingleThread();
    }

    private GameSingleThread() {
        fstConfiguration = FSTConfiguration.createDefaultConfiguration(); // A little faster than
        // createDefaultConfiguration().
        oneTimeSetup();
        lock.lock();
        makeNewWorld();
        lock.unlock();
        initState = getCurrentState(); // Make sure this stays below all
    }

    /**
     * This is how you actually get the single-threaded game. This is the singleton pattern, i.e. only one instance
     * of this class should ever exist. This will also lock the game, and any future thread trying to access this
     * will wait until the original thread calls {@link GameSingleThread#releaseGame()}.
     * @return The single instance of this class.
     */
    public static synchronized GameSingleThread getInstance() {
        lock.lock();
//        instance.makeNewWorld();
        return instance;
    }

    /**
     * Release the lock that the current thread has on the game. Only the thread which owns the lock may do this.
     */
    public void releaseGame() {
        lock.unlock();
    }
    /**
     * Call once to initialize a lot of shape definitions which only need to be created once.
     **/
    private void oneTimeSetup() {
        /*
         * Make the bodies and collision shapes
         */

        /* TRACK */
        trackDef.position = new Vec2(trackPosX, trackPosY);
        trackShape.setAsBox(trackXDim, trackYDim);
        trackShape.restitution = trackRest;
        trackShape.friction = trackFric;
        trackShape.filter.groupIndex = 1;

        /* FEET */
        //Create the fixture shapes, IE collision shapes.
        rFootShape.setAsBox(rFootL / 2f, rFootH / 2f);
        lFootShape.setAsBox(lFootL / 2f, lFootH / 2f);

        rFootShape.friction = (rFootFric);
        lFootShape.friction = (lFootFric);
        rFootShape.density = rFootDensity;
        lFootShape.density = lFootDensity;
        rFootShape.filter.groupIndex = BODY_GROUP;
        lFootShape.filter.groupIndex = BODY_GROUP;

        rFootDef.position = rFootPos;
        rFootDef.angle = rFootAng;
        lFootDef.position.set(lFootPos);
        lFootDef.angle = lFootAng;

        rFootMassData.mass = rFootMass;
        rFootMassData.I = rFootInertia;
        rFootDef.massData = rFootMassData;
        lFootMassData.mass = lFootMass;
        lFootMassData.I = lFootInertia;
        lFootDef.massData = lFootMassData;

        /* CALVES */
        rCalfShape.setAsBox(rCalfW / 2f, rCalfL / 2f);
        lCalfShape.setAsBox(lCalfW / 2f, lCalfL / 2f);

        rCalfShape.friction = rCalfFric;
        lCalfShape.friction = lCalfFric;
        rCalfShape.density = rCalfDensity;
        lCalfShape.density = lCalfDensity;
        rCalfShape.filter.groupIndex = BODY_GROUP;
        lCalfShape.filter.groupIndex = BODY_GROUP;

        rCalfDef.position = (rCalfPos);
        rCalfDef.angle = rCalfAng + rCalfAngAdj;
        lCalfDef.position = (lCalfPos);
        lCalfDef.angle = lCalfAng + lCalfAngAdj;

        rCalfMassData.I = rCalfInertia;
        rCalfMassData.mass = rCalfMass;
        lCalfMassData.I = lCalfInertia;
        lCalfMassData.mass = lCalfMass;
        rCalfDef.massData = rCalfMassData;
        lCalfDef.massData = lCalfMassData;

        /* THIGHS */
        rThighShape.setAsBox(rThighW / 2f, rThighL / 2f);
        lThighShape.setAsBox(lThighW / 2f, lThighL / 2f);

        rThighShape.friction = rThighFric;
        lThighShape.friction = lThighFric;
        rThighShape.density = rThighDensity;
        lThighShape.density = lThighDensity;
        rThighShape.filter.groupIndex = BODY_GROUP;
        lThighShape.filter.groupIndex = BODY_GROUP;

        rThighDef.position.set(rThighPos);
        lThighDef.position.set(lThighPos);
        rThighDef.angle = rThighAng + rThighAngAdj;
        lThighDef.angle = lThighAng + lThighAngAdj;

        rThighMassData.I = rThighInertia;
        rThighMassData.mass = rThighMass;
        lThighMassData.I = lThighInertia;
        lThighMassData.mass = lThighMass;
        rThighDef.massData = rThighMassData;
        lThighDef.massData = lThighMassData;

        /* TORSO */
        torsoShape.setAsBox(torsoW / 2f, torsoL / 2f);
        torsoShape.friction = torsoFric;
        torsoShape.density = torsoDensity;
        torsoShape.filter.groupIndex = BODY_GROUP;

        torsoDef.position.set(torsoPos);
        torsoDef.angle = torsoAng + torsoAngAdj;

        torsoMassData.I = torsoInertia;
        torsoMassData.mass = torsoMass;
        torsoDef.massData = torsoMassData;

        /* HEAD */
        headShape.radius = (headR);
        headShape.friction = headFric;
        headShape.density = headDensity;
        headShape.restitution = 0f;
        headShape.filter.groupIndex = BODY_GROUP;

        headDef.position.set(headPos);
        headDef.angle = headAng + headAngAdj;

        headMassData.I = headInertia;
        headMassData.mass = headMass;
        headDef.massData = headMassData;

        /* UPPER ARMS */
        rUArmShape.setAsBox(rUArmW / 2f, rUArmL / 2f);
        lUArmShape.setAsBox(lUArmW / 2f, lUArmL / 2f);
        rUArmShape.friction = rUArmFric;
        lUArmShape.friction = lUArmFric;
        rUArmShape.density = rUArmDensity;
        lUArmShape.density = lUArmDensity;
        rUArmShape.filter.groupIndex = BODY_GROUP;
        lUArmShape.filter.groupIndex = BODY_GROUP;

        rUArmDef.position.set(rUArmPos);
        lUArmDef.position.set(lUArmPos);
        rUArmDef.angle = rUArmAng + rUArmAngAdj;
        lUArmDef.angle = lUArmAng + lUArmAngAdj;

        rUArmMassData.I = rUArmInertia;
        rUArmMassData.mass = rUArmMass;
        lUArmMassData.I = lUArmInertia;
        lUArmMassData.mass = lUArmMass;
        rUArmDef.massData = rUArmMassData;
        lUArmDef.massData = lUArmMassData;

        /* LOWER ARMS */
        rLArmShape.setAsBox(rLArmW / 2f, rLArmL / 2f);
        lLArmShape.setAsBox(lLArmW / 2f, lLArmL / 2f);
        rLArmShape.friction = rLArmFric;
        lLArmShape.friction = lLArmFric;
        rLArmShape.density = rLArmDensity;
        lLArmShape.density = lLArmDensity;
        rLArmShape.filter.groupIndex = BODY_GROUP;
        lLArmShape.filter.groupIndex = BODY_GROUP;

        rLArmDef.position.set(rLArmPos);
        lLArmDef.position.set(lLArmPos);
        rLArmDef.angle = rLArmAng + rLArmAngAdj;
        lLArmDef.angle = lLArmAng + lLArmAngAdj;

        rLArmMassData.I = rLArmInertia;
        rLArmMassData.mass = rLArmMass;
        lLArmMassData.I = lLArmInertia;
        lLArmMassData.mass = lLArmMass;
        rLArmDef.massData = rLArmMassData;
        lLArmDef.massData = lLArmMassData;
    }

    public void makeNewWorld() {
        if (!lock.isHeldByCurrentThread()) {
            throw new ConcurrentModificationException("Cannot attempt to alter the single threaded game without first" +
                    " obtaining its lock through getInstance. If having issues, make sure that all other threads have" +
                    " called releaseGame to forfeit their locks.");
        }

        if (invalidated) {
            throw new RuntimeException("Attempted to access an old copy of the game after a newer one was loaded from" +
                    " serialized game. Dispose of this copy.");
        }

        isFailed = false;
        timestepsSimulated = 0;

        /* World Settings */
        world = new World(worldAABB, gravity, true);
        world.setWarmStarting(true);
        world.setPositionCorrection(true);
        world.setContinuousPhysics(true);

        // NOTE: The order of creating bodies actually changes the answers slightly!! This is really dumb, but will
        // affect us if we are trying to match the single and multithreaded version.

        /* TRACK */
        trackBody = world.createBody(trackDef);
        trackBody.createShape(trackShape);

        /* FEET */
        if (!noFeet) {
            rFootBody = getWorld().createBody(rFootDef);
            lFootBody = getWorld().createBody(lFootDef);
            rFootBody.createShape(rFootShape);
            lFootBody.createShape(lFootShape);
        }

        /* CALVES */
        rCalfBody = getWorld().createBody(rCalfDef);
        lCalfBody = getWorld().createBody(lCalfDef);
        rCalfBody.createShape(rCalfShape);
        lCalfBody.createShape(lCalfShape);

        /* THIGHS */
        rThighBody = getWorld().createBody(rThighDef);
        lThighBody = getWorld().createBody(lThighDef);
        rThighBody.createShape(rThighShape);
        lThighBody.createShape(lThighShape);

        /* UPPER ARMS */
        rUArmBody = getWorld().createBody(rUArmDef);
        lUArmBody = getWorld().createBody(lUArmDef);
        rUArmBody.createShape(rUArmShape);
        lUArmBody.createShape(lUArmShape);

        /* LOWER ARMS */
        rLArmBody = getWorld().createBody(rLArmDef);
        lLArmBody = getWorld().createBody(lLArmDef);
        rLArmBody.createShape(rLArmShape);
        lLArmBody.createShape(lLArmShape);

        /* TORSO */
        torsoBody = getWorld().createBody(torsoDef);
        torsoBody.createShape(torsoShape);

        /* HEAD */
        headBody = getWorld().createBody(headDef);
        headBody.createShape(headShape);

        allBodies = new Body[]{rCalfBody, lCalfBody, rThighBody, lThighBody, torsoBody, rUArmBody,
                lUArmBody, rLArmBody, lLArmBody, rFootBody, lFootBody, headBody};
        /*
         *  Joints
         */

        if (!noFeet) {
            //Right Ankle:

            rAnkleJDef = new RevoluteJointDef();
            rAnkleJDef.initialize(rFootBody, rCalfBody, rAnklePos); //Body1, body2, anchor in world coords
            rAnkleJDef.enableLimit = true;
            rAnkleJDef.upperAngle = 0.5f;
            rAnkleJDef.lowerAngle = -0.5f;
            rAnkleJDef.enableMotor = false;
            rAnkleJDef.maxMotorTorque = 2000f;
            rAnkleJDef.motorSpeed = 0f; // Speed1,2: -2,2
            rAnkleJDef.collideConnected = false;

            rAnkleJ = (RevoluteJoint) getWorld().createJoint(rAnkleJDef);

            //Left Ankle:
            lAnkleJDef = new RevoluteJointDef();
            lAnkleJDef.initialize(lFootBody, lCalfBody, lAnklePos);
            lAnkleJDef.enableLimit = true;
            lAnkleJDef.upperAngle = 0.5f;
            lAnkleJDef.lowerAngle = -0.5f;
            lAnkleJDef.enableMotor = false;
            lAnkleJDef.maxMotorTorque = 2000f;
            lAnkleJDef.motorSpeed = 0f;// Speed1,2: 2,-2
            lAnkleJDef.collideConnected = false;

            lAnkleJ = (RevoluteJoint) getWorld().createJoint(lAnkleJDef);
        }

        /* Knee joints */
        //Right Knee:
        rKneeJDef = new RevoluteJointDef();
        rKneeJDef.initialize(rCalfBody, rThighBody, rKneePos);
        rKneeJDef.enableLimit = true;
        rKneeJDef.upperAngle = 0.3f;
        rKneeJDef.lowerAngle = -1.3f;
        rKneeJDef.enableMotor = true;//?
        rKneeJDef.maxMotorTorque = 3000f;
        rKneeJDef.motorSpeed = 0f; //Speeds 1,2: -2.5,2.5
        rKneeJDef.collideConnected = false;

        rKneeJ = (RevoluteJoint) getWorld().createJoint(rKneeJDef);

        //Left Knee:
        lKneeJDef = new RevoluteJointDef();
        lKneeJDef.initialize(lCalfBody, lThighBody, lKneePos);
        lKneeJDef.enableLimit = true;
        lKneeJDef.upperAngle = 0f;
        lKneeJDef.lowerAngle = -1.6f;
        lKneeJDef.enableMotor = true;
        lKneeJDef.maxMotorTorque = 3000f;
        lKneeJDef.motorSpeed = 0f;// Speed1,2: -2.5,2.5
        lKneeJDef.collideConnected = false;

        lKneeJ = (RevoluteJoint) getWorld().createJoint(lKneeJDef);

        /* Hip Joints */

        //Right Hip:
        rHipJDef = new RevoluteJointDef();
        rHipJDef.initialize(rThighBody, torsoBody, rHipPos);
        rHipJDef.enableLimit = true;
        rHipJDef.upperAngle = 0.7f;
        rHipJDef.lowerAngle = -1.3f;
        rHipJDef.enableMotor = true;
        rHipJDef.motorSpeed = 0f;
        rHipJDef.maxMotorTorque = 6000f;
        rHipJDef.collideConnected = false;
        rHipJ = (RevoluteJoint) getWorld().createJoint(rHipJDef);

        //Left Hip:
        lHipJDef = new RevoluteJointDef();
        lHipJDef.initialize(lThighBody, torsoBody, lHipPos);
        lHipJDef.enableLimit = true;
        lHipJDef.upperAngle = 0.5f;
        lHipJDef.lowerAngle = -1.5f;
        lHipJDef.enableMotor = true;
        lHipJDef.motorSpeed = 0f;
        lHipJDef.maxMotorTorque = 6000f;
        lHipJDef.collideConnected = false;
        lHipJ = (RevoluteJoint) getWorld().createJoint(lHipJDef);

        //Neck Joint
        neckJDef = new RevoluteJointDef();
        neckJDef.initialize(headBody, torsoBody, neckPos);
        neckJDef.enableLimit = true;
        neckJDef.upperAngle = 0f;
        neckJDef.lowerAngle = -0.5f;
        neckJDef.enableMotor = true;
        neckJDef.maxMotorTorque = 1000f; //Arbitrarily large to allow for torque control.
        neckJDef.motorSpeed = 0f;
        neckJDef.collideConnected = false;
        neckJ = (RevoluteJoint) getWorld().createJoint(neckJDef);

        /* Arm Joints */
        //Right shoulder
        rShoulderJDef = new RevoluteJointDef();
        rShoulderJDef.initialize(rUArmBody, torsoBody, rShoulderPos);
        rShoulderJDef.enableLimit = true;
        rShoulderJDef.upperAngle = 1.5f;
        rShoulderJDef.lowerAngle = -0.5f;
        rShoulderJDef.enableMotor = true;
        rShoulderJDef.maxMotorTorque = 1000f;
        rShoulderJDef.motorSpeed = 0f; // Speed 1,2: 2,-2
        rShoulderJDef.collideConnected = false;
        rShoulderJ = (RevoluteJoint) getWorld().createJoint(rShoulderJDef);

        //Left shoulder
        lShoulderJDef = new RevoluteJointDef();
        lShoulderJDef.initialize(lUArmBody, torsoBody, lShoulderPos);
        lShoulderJDef.enableLimit = true;
        lShoulderJDef.upperAngle = 0f;
        lShoulderJDef.lowerAngle = -2f;
        lShoulderJDef.enableMotor = true;
        lShoulderJDef.maxMotorTorque = 1000f;
        lShoulderJDef.motorSpeed = 0f; // Speed 1,2: -2,2
        lShoulderJDef.collideConnected = false;
        lShoulderJ = (RevoluteJoint) getWorld().createJoint(lShoulderJDef);

        //Right elbow
        rElbowJDef = new RevoluteJointDef();
        rElbowJDef.initialize(rLArmBody, rUArmBody, rElbowPos);
        rElbowJDef.enableLimit = true;
        rElbowJDef.upperAngle = 0.5f;
        rElbowJDef.lowerAngle = -0.1f;
        rElbowJDef.enableMotor = true;
        rElbowJDef.maxMotorTorque = 0f;
        rElbowJDef.motorSpeed = 10f; //TODO: investigate further
        rElbowJDef.collideConnected = false;
        rElbowJ = (RevoluteJoint) getWorld().createJoint(rElbowJDef);

        //Left elbow
        lElbowJDef = new RevoluteJointDef();
        lElbowJDef.initialize(lLArmBody, lUArmBody, lElbowPos);
        lElbowJDef.enableLimit = true;
        lElbowJDef.upperAngle = 0.5f;
        lElbowJDef.lowerAngle = -0.1f;
        lElbowJDef.enableMotor = true;
        lElbowJDef.maxMotorTorque = 0f;
        lElbowJDef.motorSpeed = 10f; //TODO: investigate further
        lElbowJDef.collideConnected = false;
        lElbowJ = (RevoluteJoint) getWorld().createJoint(lElbowJDef);

        //My current understanding is that the shapes never change. Only the transforms. Hence, this is now static and we only capture the states once.
        if (shapeList[0] == null) {
            shapeList[0] = torsoBody.getShapeList();
            shapeList[1] = headBody.getShapeList();
            shapeList[2] = rFootBody.getShapeList();
            shapeList[3] = lFootBody.getShapeList();
            shapeList[4] = rCalfBody.getShapeList();
            shapeList[5] = lCalfBody.getShapeList();
            shapeList[6] = rThighBody.getShapeList();
            shapeList[7] = lThighBody.getShapeList();
            shapeList[8] = rUArmBody.getShapeList();
            shapeList[9] = lUArmBody.getShapeList();
            shapeList[10] = rLArmBody.getShapeList();
            shapeList[11] = lLArmBody.getShapeList();
            shapeList[12] = trackBody.getShapeList();
        }

        // Listen for ground-body contacts.
        world.setContactListener(collisionListener);
    }

    private void setMaxMotorTorque(float torqueLimitMultiplier) {
        if (!noFeet) {
            rAnkleJ.setMaxMotorTorque(2000f * torqueLimitMultiplier);
            lAnkleJ.setMaxMotorTorque(2000f * torqueLimitMultiplier);
        }

        rKneeJ.setMaxMotorTorque(3000f * torqueLimitMultiplier);
        lKneeJ.setMaxMotorTorque(3000f * torqueLimitMultiplier);

        rHipJ.setMaxMotorTorque(6000f * torqueLimitMultiplier);
        lHipJ.setMaxMotorTorque(6000f * torqueLimitMultiplier);

        neckJ.setMaxMotorTorque(1000f * torqueLimitMultiplier);

        rShoulderJ.setMaxMotorTorque(1000f * torqueLimitMultiplier);
        lShoulderJ.setMaxMotorTorque(1000f * torqueLimitMultiplier);

        rElbowJ.setMaxMotorTorque(0f);
        lElbowJ.setMaxMotorTorque(0f);
    }

    public void step(boolean[] command) {
        if (command.length != 4) {
            throw new IllegalArgumentException("Command is not the correct length. Expected 4, got: " + command.length);
        }
        step(command[0], command[1], command[2], command[3]);
    }

    /**
     * Step the game forward 1 timestep with the specified keys pressed.
     **/
    public void step(boolean q, boolean w, boolean o, boolean p) {
        if (!lock.isHeldByCurrentThread()) {
            throw new ConcurrentModificationException("Cannot attempt to alter the single threaded game without first" +
                    " obtaining its lock through getInstance. If having issues, make sure that all other threads have" +
                    " called releaseGame to forfeit their locks.");
        }
        if (invalidated) {
            throw new RuntimeException("Attempted to access an old copy of the game after a newer one was loaded from" +
                    " serialized game. Dispose of this copy.");
        }

        /* Involuntary Couplings (no QWOP presses) */

        //Neck spring torque
        float NeckTorque = -neckStiff * neckJ.getJointAngle() + 0 * neckDamp * neckJ.getJointSpeed();
        NeckTorque = NeckTorque + 0 * 400f * (neckJ.getJointAngle() + 0.2f); //This bizarre term is probably a roundabout way of adjust equilibrium position.

        //Elbow spring torque
        float RElbowTorque = -rElbowStiff * rElbowJ.getJointAngle() + 0 * rElbowDamp * rElbowJ.getJointSpeed();
        float LElbowTorque = -lElbowStiff * lElbowJ.getJointAngle() + 0 * lElbowDamp * lElbowJ.getJointSpeed();

        //For now, using motors with high speed settings and torque limits to simulate springs. I don't know a better way for now.

        neckJ.m_motorSpeed = (1000f * Math.signum(NeckTorque)); //If torque is negative, make motor speed negative.
        rElbowJ.m_motorSpeed = (1000f * Math.signum(RElbowTorque));
        lElbowJ.m_motorSpeed = (1000f * Math.signum(LElbowTorque));

        neckJ.m_maxMotorTorque = (Math.abs(NeckTorque));
        rElbowJ.m_maxMotorTorque = (Math.abs(RElbowTorque));
        lElbowJ.m_maxMotorTorque = (Math.abs(LElbowTorque));

        /* QW Press Stuff */
        //See spreadsheet for complete rules and priority explanations.
        if (q) {
            //Set speed 1 for hips:
            lHipJ.m_motorSpeed = (lHipSpeed2);
            rHipJ.m_motorSpeed = (rHipSpeed2);

            //Set speed 1 for shoulders:
            lShoulderJ.m_motorSpeed = (lShoulderSpeed2);
            rShoulderJ.m_motorSpeed = (rShoulderSpeed2);

        } else if (w) {
            //Set speed 2 for hips:
            lHipJ.m_motorSpeed = (lHipSpeed1);
            rHipJ.m_motorSpeed = (rHipSpeed1);

            //set speed 2 for shoulders:
            lShoulderJ.m_motorSpeed = (lShoulderSpeed1);
            rShoulderJ.m_motorSpeed = (rShoulderSpeed1);

        } else {
            //Set hip and ankle speeds to 0:
            lHipJ.m_motorSpeed = (0f);
            rHipJ.m_motorSpeed = (0f);

            lShoulderJ.m_motorSpeed = (0f);
            rShoulderJ.m_motorSpeed = (0f);
        }

        //Ankle/Hip Coupling -+ 0*Requires either Q or W pressed.
        if (q || w && !noFeet) {
            //Get world ankle positions (using foot and torso anchors -+ 0
            Vec2 RAnkleCur = rAnkleJ.getAnchor1();
            Vec2 LAnkleCur = lAnkleJ.getAnchor1();

            Vec2 RHipCur = rHipJ.getAnchor1();


            // if right ankle joint is behind the right hip jiont
            // Set ankle motor speed to 1;
            // else speed 2
            if (RAnkleCur.x < RHipCur.x) {
                rAnkleJ.m_motorSpeed = (rAnkleSpeed2);
            } else {
                rAnkleJ.m_motorSpeed = (rAnkleSpeed1);
            }


            // if left ankle joint is behind RIGHT hip joint (weird it's the right one here too)
            // Set its motor speed to 1;
            // else speed 2;
            if (LAnkleCur.x < RHipCur.x) {
                lAnkleJ.m_motorSpeed = (lAnkleSpeed2);
            } else {
                lAnkleJ.m_motorSpeed = (lAnkleSpeed1);
            }
        }

        /* OP Keypress Stuff */
        if (o) {
            //Set speed 1 for knees
            // set l hip limits(-1 1)
            //set right hip limits (-1.3,0.7)
            rKneeJ.m_motorSpeed = (rKneeSpeed2);
            lKneeJ.m_motorSpeed = (lKneeSpeed2);

            rHipJ.m_lowerAngle = (oRHipLimLo);
            rHipJ.m_upperAngle = (oRHipLimHi);

            lHipJ.m_lowerAngle = (oLHipLimLo);
            lHipJ.m_upperAngle = (oLHipLimHi);

        } else if (p) {
            //Set speed 2 for knees
            // set L hip limits(-1.5,0.5)
            // set R hip limits(-0.8,1.2)

            rKneeJ.m_motorSpeed = (rKneeSpeed1);
            lKneeJ.m_motorSpeed = (lKneeSpeed1);

            rHipJ.m_lowerAngle = (pRHipLimLo);
            rHipJ.m_upperAngle = (pRHipLimHi);
            lHipJ.m_lowerAngle = pLHipLimLo;
            lHipJ.m_upperAngle = pLHipLimHi;

        } else {

            // Set knee speeds to 0
            //Joint limits not changed!!
            rKneeJ.m_motorSpeed = (0f);
            lKneeJ.m_motorSpeed = (0f);
        }


        getWorld().step(timestep, physIterations);

        // Extra fail conditions besides contacts.
        float angle = torsoBody.getAngle();
        if (angle > torsoAngUpper || angle < torsoAngLower) { // Fail if torso angles get too far out of whack.
            isFailed = true;
        }

        timestepsSimulated++;
    }

    /**
     * Get the actual Box2D world.
     **/
    public World getWorld() {
        return world;
    }

    /**
     * QWOP initial condition. Good way to give the root node a state.
     **/
    public static State getInitialState() {
        return initState;
    }

    /**
     * Get the current full state of the runner.
     */
    public synchronized State getCurrentState() {
        return new State(
                getCurrentBodyState(torsoBody),
                getCurrentBodyState(headBody),
                getCurrentBodyState(rThighBody),
                getCurrentBodyState(lThighBody),
                getCurrentBodyState(rCalfBody),
                getCurrentBodyState(lCalfBody),
                noFeet ? new StateVariable(0, 0, 0, 0, 0, 0) : getCurrentBodyState(rFootBody),
                noFeet ? new StateVariable(0, 0, 0, 0, 0, 0) : getCurrentBodyState(lFootBody),
                getCurrentBodyState(rUArmBody),
                getCurrentBodyState(lUArmBody),
                getCurrentBodyState(rLArmBody),
                getCurrentBodyState(lLArmBody),
                getFailureStatus());
    }

    /**
     * Get a new StateVariable for a given body.
     */
    private StateVariable getCurrentBodyState(Body body) {
        Vec2 pos = body.getPosition();
        float x = pos.x;
        float y = pos.y;
        float th = body.getAngle();

        Vec2 vel = body.getLinearVelocity();
        float dx = vel.x;
        float dy = vel.y;
        float dth = body.getAngularVelocity();
        return new StateVariable(x, y, th, dx, dy, dth);
    }

    public Body[] getAllBodies() {
        return allBodies;
    }

    public RevoluteJoint[] getAllJoints() {
        return new RevoluteJoint[]{rHipJ, lHipJ, rKneeJ, lKneeJ, rAnkleJ, lAnkleJ, rShoulderJ, lShoulderJ, rElbowJ,
                lElbowJ, neckJ};
    }

    /**
     * Set an individual body to a specified {@link StateVariable}. This sets both positions and velocities.
     *
     * @param body          Body to set the state of.
     * @param stateVariable Full state to assign to that body.
     */
    private void setBodyToStateVariable(Body body, StateVariable stateVariable) {
        body.setXForm(new Vec2(stateVariable.getX(), stateVariable.getY()), stateVariable.getTh());
        body.setLinearVelocity(new Vec2(stateVariable.getDx(), stateVariable.getDy()));
        body.setAngularVelocity(stateVariable.getDth());
    }

    public void setState(State state) {
        if (invalidated) {
            throw new RuntimeException("Attempted to access an old copy of the game after a newer one was loaded from" +
                    " serialized game. Dispose of this copy.");
        }
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
     * Is this state in failure?
     **/
    public boolean getFailureStatus() {
        return isFailed;
    }

    /**
     * Get the number of timesteps simulated since the beginning of execution.
     **/
    public long getTimestepsSimulated() {
        return timestepsSimulated;
    }

    /**
     * Change world gravity.
     *
     * @param xGrav x component of gravity.
     * @param yGrav y component of gravity -- positive is down.
     */
    public void setGravity(float xGrav, float yGrav) {
        getWorld().setGravity(new Vec2(xGrav, yGrav));
    }

    public void setMaxTorqueMultiplier(float multiplier) {
        setMaxMotorTorque(multiplier);
    }

    public void setPointFeet(boolean usePointFeet) {
        noFeet = usePointFeet;
    }

    public void setBodyInertiaMultiplier(float multiplier) {
        MassData massData = new MassData();
        massData.mass = torsoMassData.mass;
        massData.I = torsoMassData.I * multiplier;
        torsoBody.setMass(massData);
    }

    /**
     * Apply a disturbance impulse to the body COM.
     */
    public void applyBodyImpulse(float xComp, float yComp) {
        Vec2 torsoCenter = torsoBody.getWorldCenter();
        torsoBody.applyImpulse(new Vec2(xComp, yComp), torsoCenter);
    }

    /**
     * Apply a disturbance torque to the body.
     */
    public void applyBodyTorque(float cwTorque) {
        torsoBody.applyTorque(cwTorque);
    }

    /**
     * Get vertices for debug drawing. Each array in the list will have:
     * 8 floats for rectangles (x1,y1,x2,y2,...).
     * 3 floats for circles (x,y,radius).
     * 1 float for ground (height).
     * <p>
     * This is primarily for drawing using external tools, e.g. in MATLAB.
     **/
    public VertHolder getDebugVertices() {

        VertHolder vertHolder = new VertHolder();

        vertHolder.groundHeight = XForm.mul(trackBody.getXForm(), trackShape.vertices.get(0)).y; // Never changes.
        vertHolder.torsoX = torsoBody.getPosition().x;

        for (int i = 0; i < allBodies.length; i++) {
            if (allBodies[i] != null && allBodies[i] != headBody) {
                XForm xf = allBodies[i].getXForm();
                PolygonShape shape = (PolygonShape) allBodies[i].getShapeList();
                Vec2[] shapeVerts = shape.m_vertices;
                for (int j = 0; j < shapeVerts.length; j++) {
                    Vec2 vert = XForm.mul(xf, shapeVerts[j]);
                    vertHolder.bodyVerts[i][2 * j] = vert.x;
                    vertHolder.bodyVerts[i][2 * j + 1] = vert.y;
                }
            }
        }

        vertHolder.headLocAndRadius[0] = headBody.getPosition().x;
        vertHolder.headLocAndRadius[1] = headBody.getPosition().y;
        vertHolder.headLocAndRadius[2] = headR;

        return vertHolder;
    }

    @SuppressWarnings("WeakerAccess")
    static class VertHolder {
        public float torsoX;
        public float groundHeight;
        public float[][] bodyVerts = new float[11][8];
        public float[] headLocAndRadius = new float[3];
    }

    /**
     * Draw this game's runner. Must provide scaling from game units to pixels, as well as pixel offsets in x and y.
     **/
    public void draw(Graphics g, float scaling, int xOffset, int yOffset) {

        Body newBody = getWorld().getBodyList();
        while (newBody != null) {
            int xOffsetPixels = -(int) (scaling * torsoBody.getPosition().x) + xOffset; // Basic offset, plus centering x on torso.
            Shape newfixture = newBody.getShapeList();

            while (newfixture != null) {

                // Most links are polygon shapes
                if (newfixture.getType() == ShapeType.POLYGON_SHAPE) {

                    PolygonShape newShape = (PolygonShape) newfixture;
                    Vec2[] shapeVerts = newShape.m_vertices;
                    for (int k = 0; k < newShape.m_vertexCount; k++) {

                        XForm xf = newBody.getXForm();
                        Vec2 ptA = XForm.mul(xf, shapeVerts[k]);
                        Vec2 ptB = XForm.mul(xf, shapeVerts[(k + 1) % (newShape.m_vertexCount)]);
                        g.drawLine((int) (scaling * ptA.x) + xOffsetPixels,
                                (int) (scaling * ptA.y) + yOffset,
                                (int) (scaling * ptB.x) + xOffsetPixels,
                                (int) (scaling * ptB.y) + yOffset);
                    }
                } else if (newfixture.getType() == ShapeType.CIRCLE_SHAPE) { // Basically just head
                    CircleShape newShape = (CircleShape) newfixture;
                    float radius = newShape.m_radius;
                    g.drawOval((int) (scaling * (newBody.getPosition().x - radius) + xOffsetPixels),
                            (int) (scaling * (newBody.getPosition().y - radius) + yOffset),
                            (int) (scaling * radius * 2),
                            (int) (scaling * radius * 2));

                } else if (newfixture.getType() == ShapeType.EDGE_SHAPE) { // The track.

                    EdgeShape newShape = (EdgeShape) newfixture;
                    XForm trans = newBody.getXForm();

                    Vec2 ptA = XForm.mul(trans, newShape.getVertex1());
                    Vec2 ptB = XForm.mul(trans, newShape.getVertex2());
                    Vec2 ptC = XForm.mul(trans, newShape.getVertex2());

                    g.drawLine((int) (scaling * ptA.x) + xOffsetPixels,
                            (int) (scaling * ptA.y) + yOffset,
                            (int) (scaling * ptB.x) + xOffsetPixels,
                            (int) (scaling * ptB.y) + yOffset);
                    g.drawLine((int) (scaling * ptA.x) + xOffsetPixels,
                            (int) (scaling * ptA.y) + yOffset,
                            (int) (scaling * ptC.x) + xOffsetPixels,
                            (int) (scaling * ptC.y) + yOffset);

                } else {
                    System.out.println("Not found: " + newfixture.m_type.name());
                }
                newfixture = newfixture.getNext();
            }
            newBody = newBody.getNext();
        }

        //This draws the "road" markings to show that the ground is moving relative to the dude.
        int markingWidth = 5000;
        for (int i = 0; i < markingWidth / 69; i++) {
            g.drawString("_", ((-(int) (scaling * torsoBody.getPosition().x) - i * 70) % markingWidth) + markingWidth, yOffset + 92);
        }
    }

    /**
     * Draw the runner at a specified set of transforms..
     **/
    public static void drawExtraRunner(Graphics2D g, XForm[] transforms, String label, float scaling, int xOffset, int yOffset, Color drawColor, Stroke stroke) {
        g.setColor(drawColor);
        g.drawString(label, xOffset + (int) (transforms[1].position.x * scaling) - 20, yOffset - 75);
        for (int i = 0; i < shapeList.length; i++) {
            g.setColor(drawColor);
            g.setStroke(stroke);
            switch (shapeList[i].getType()) {
                case CIRCLE_SHAPE:
                    CircleShape circleShape = (CircleShape) shapeList[i];
                    float radius = circleShape.getRadius();
                    Vec2 circleCenter = XForm.mul(transforms[i], circleShape.getLocalPosition());
                    g.drawOval((int) (scaling * (circleCenter.x - radius) + xOffset),
                            (int) (scaling * (circleCenter.y - radius) + yOffset),
                            (int) (scaling * radius * 2),
                            (int) (scaling * radius * 2));
                    break;
                case POLYGON_SHAPE:
                    //Get both the shape and its transform.
                    PolygonShape polygonShape = (PolygonShape) shapeList[i];
                    XForm transform = transforms[i];

                    // Ground is black regardless.
                    if (shapeList[i].m_filter.groupIndex == 1) {
                        g.setColor(Color.BLACK);
                        g.setStroke(normalStroke);
                    }
                    for (int j = 0; j < polygonShape.getVertexCount(); j++) { // Loop through polygon vertices and draw lines between them.
                        Vec2 ptA = XForm.mul(transform, polygonShape.m_vertices[j]);
                        Vec2 ptB = XForm.mul(transform, polygonShape.m_vertices[(j + 1) % (polygonShape.getVertexCount())]); //Makes sure that the last vertex is connected to the first one.
                        g.drawLine((int) (scaling * ptA.x) + xOffset,
                                (int) (scaling * ptA.y) + yOffset,
                                (int) (scaling * ptB.x) + xOffset,
                                (int) (scaling * ptB.y) + yOffset);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Listens for collisions involving lower arms and head (implicitly with the ground)
     **/
    private class CollisionListener implements ContactListener, Serializable {

        /**
         * Keep track of whether the right foot is on the ground.
         **/
        private boolean rFootDown = false;

        /**
         * Keep track of whether the left foot is on the ground.
         **/
        private boolean lFootDown = false;

        @Override
        public void add(ContactPoint point) {
            Shape fixtureA = point.shape1;
            Shape fixtureB = point.shape2;

            //Failure when head, arms, torso hit the ground. Note that failing on calf contact is a bad idea. The
            // ankles are loose enough that the calfs hit the ground sometimes during ok running.
            if (fixtureA.m_body.equals(headBody) ||
                    fixtureB.m_body.equals(headBody) ||
                    fixtureA.m_body.equals(lLArmBody) ||
                    fixtureB.m_body.equals(lLArmBody) ||
                    fixtureA.m_body.equals(rLArmBody) ||
                    fixtureB.m_body.equals(rLArmBody) ||
                    fixtureA.m_body.equals(torsoBody) ||
                    fixtureB.m_body.equals(torsoBody)) {
                isFailed = true;
            } else if (failOnThighContact && // Only fail on thigh contact if this is turned on. Cannot kneel without
                    // failure with this on.
                    (fixtureA.m_body.equals(lThighBody) ||
                    fixtureB.m_body.equals(lThighBody) ||
                    fixtureA.m_body.equals(rThighBody) ||
                    fixtureB.m_body.equals(rThighBody))) {
                isFailed = true;
            } else if (!noFeet && fixtureA.m_body.equals(rFootBody) || fixtureB.m_body.equals(rFootBody)) {//Track when
                // each foot hits the ground.
                rFootDown = true;
            } else if (!noFeet && fixtureA.m_body.equals(lFootBody) || fixtureB.m_body.equals(lFootBody)) {
                lFootDown = true;
            }
        }

        @Override
        public void persist(ContactPoint point) {}

        @Override
        public void remove(ContactPoint point) {
            //Track when each foot leaves the ground.
            Shape fixtureA = point.shape1;
            Shape fixtureB = point.shape2;
            if (!noFeet && fixtureA.m_body.equals(rFootBody) || fixtureB.m_body.equals(rFootBody)) {
                rFootDown = false;
            } else if (!noFeet && fixtureA.m_body.equals(lFootBody) || fixtureB.m_body.equals(lFootBody)) {
                lFootDown = false;
            }
        }

        @Override
        public void result(ContactResult point) {}

        /**
         * Check if the right foot is touching the ground.
         **/
        @SuppressWarnings("unused")
        public boolean isRightFootGrounded() {
            return rFootDown;
        }

        /**
         * Check if the left foot is touching the ground.
         **/
        @SuppressWarnings("unused")
        public boolean isLeftFootGrounded() {
            return lFootDown;
        }
    }

    public synchronized byte[] getFullState() {
//        ByteArrayOutputStream bout = new ByteArrayOutputStream();
//        try (ObjectOutputStream objOps = new ObjectOutputStream(bout)) {
//            objOps.writeObject(this);
//            objOps.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return bout.toByteArray();
        return fstConfiguration.asByteArray(this);
    }

    public synchronized GameSingleThread restoreFullState(byte[] fullState) {
//        GameSingleThread gameRestored = null;
//        try (ByteArrayInputStream bin = new ByteArrayInputStream(fullState);
//             ObjectInputStream objIs = new ObjectInputStream(bin)) {
//            gameRestored = (GameSingleThread) objIs.readObject();
//        } catch (ClassNotFoundException | IOException e) {
//            e.printStackTrace();
//        }

        GameSingleThread gameRestored = (GameSingleThread)fstConfiguration.asObject(fullState);
        // Replace all the relevant game fields which have been loaded.
        assert gameRestored != null;
        invalidated = true;
        GameSingleThread.instance = gameRestored;
        gameRestored.fstConfiguration = fstConfiguration;
        GameSingleThread.lock = new ReentrantLock();
        lock.lock();
        return gameRestored;
        // So far have not successfully set fields like below such that tests pass.
//        this.world = gameRestored.world;
//        this.rFootBody = gameRestored.rFootBody;
//        this.lFootBody = gameRestored.lFootBody;
//        this.rCalfBody = gameRestored.rCalfBody;
//        this.lCalfBody = gameRestored.lCalfBody;
//        this.rThighBody = gameRestored.rThighBody;
//        this.lThighBody = gameRestored.lThighBody;
//        this.torsoBody = gameRestored.torsoBody;
//        this.rUArmBody = gameRestored.rUArmBody;
//        this.lUArmBody = gameRestored.lUArmBody;
//        this.rLArmBody = gameRestored.rLArmBody;
//        this.lLArmBody = gameRestored.lLArmBody;
//        this.headBody = gameRestored.headBody;
//        this.trackBody = gameRestored.trackBody;
//
//        this.rHipJDef = gameRestored.rHipJDef;
//        this.lHipJDef = gameRestored.lHipJDef;
//        this.rKneeJDef = gameRestored.rKneeJDef;
//        this.lKneeJDef = gameRestored.lKneeJDef;
//        this.rAnkleJDef = gameRestored.rAnkleJDef;
//        this.lAnkleJDef = gameRestored.lAnkleJDef;
//        this.rShoulderJDef = gameRestored.rShoulderJDef;
//        this.lShoulderJDef = gameRestored.lShoulderJDef;
//        this.rElbowJDef = gameRestored.rElbowJDef;
//        this.lElbowJDef = gameRestored.lElbowJDef;
//
//        this.rHipJ = gameRestored.rHipJ;
//        this.lHipJ = gameRestored.lHipJ;
//        this.rKneeJ = gameRestored.rKneeJ;
//        this.lKneeJ = gameRestored.lKneeJ;
//        this.rAnkleJ = gameRestored.rAnkleJ;
//        this.lAnkleJ = gameRestored.lAnkleJ;
//        this.rShoulderJ = gameRestored.rShoulderJ;
//        this.lShoulderJ = gameRestored.lShoulderJ;
//        this.rElbowJ = gameRestored.rElbowJ;
//        this.lElbowJ = gameRestored.lElbowJ;
//
//        this.isFailed = gameRestored.isFailed;
//        this.collisionListener = gameRestored.collisionListener;
//        this.timestepsSimulated = gameRestored.timestepsSimulated;
//        this.initState = gameRestored.initState;
//        this.noFeet = gameRestored.noFeet;
//
    }
}
