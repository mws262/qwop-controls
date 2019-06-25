package game;

import game.action.Action;
import game.state.IState;
import game.state.IState.ObjectName;
import game.state.State;
import game.state.StateVariable;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.*;
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

import java.awt.*;
import java.io.Serializable;

import static game.GameConstants.*;

/**
 * @author matt
 */
@SuppressWarnings("Duplicates")
public class GameUnified implements IGameInternal, IGameSerializable {

    public static final int STATE_SIZE = 72;

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

    private static final Vec2
            rFootPos = new Vec2(rFootPosX, rFootPosY), lFootPos = new Vec2(lFootPosX, lFootPosY),
            rCalfPos = new Vec2(rCalfPosX, rCalfPosY), lCalfPos = new Vec2(lCalfPosX, lCalfPosY),
            rThighPos = new Vec2(rThighPosX, rThighPosY), lThighPos = new Vec2(lThighPosX, lThighPosY),
            torsoPos = new Vec2(torsoPosX, torsoPosY), headPos = new Vec2(headPosX, headPosY),
            rUArmPos = new Vec2(rUArmPosX, rUArmPosY), lUArmPos = new Vec2(lUArmPosX, lUArmPosY),
            rLArmPos = new Vec2(rLArmPosX, rLArmPosY), lLArmPos = new Vec2(lLArmPosX, lLArmPosY);

    /* Joints Positions*/
    private static final Vec2
            rAnklePos = new Vec2(rAnklePosX, rAnklePosY),
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

    private static final BodyDef
            trackDef = new BodyDef(),
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

    private static final PolygonDef
            trackShape = new PolygonDef(),
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
            lLArmShape = new PolygonDef(),
            headShape = new PolygonDef();

    private static final MassData
            rFootMassData = new MassData(),
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
    private static final IState initState;

    /** Can turn off feet (just leg stumps) for trying stuff out. **/
    private static boolean noFeet = false;

    /**
     * Is Box2D warmstarting used?
     */
    public boolean useWarmStarting = true;

    public boolean usePositionCorrection = true;

    /**
     * Number of constraint-solving steps.
     */
    public int iterations = physIterations;

    /** Listens for collisions between any body part and the ground. **/
    private CollisionListener collisionListener = new CollisionListener();

    /** Should the game be marked as failed if the thighs touch the ground? (happens with knees touching the ground. **/
    public static boolean failOnThighContact = true;

    /**
     * Normal stroke for line drawing.
     **/
    private static final Stroke normalStroke = new BasicStroke(0.5f);

    /**
     * For faster serialization.
     */
    private static FSTConfiguration fstConfiguration = FSTConfiguration.createDefaultConfiguration();

    //private Random rand = new Random();
    public GameUnified() {
        makeNewWorld();
        //rand.setSeed(55555);
    }

    /*
     * Call once to initialize a lot of shape definitions which only need to be created once.
     */
    static {
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
        rFootShape.restitution = 0f;
        lFootShape.restitution = 0f;
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
        rCalfShape.restitution = 0f;
        lCalfShape.restitution = 0f;
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
        rThighShape.restitution = 0f;
        lThighShape.restitution = 0f;
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
        torsoShape.restitution = 0f;
        torsoShape.filter.groupIndex = BODY_GROUP;

        torsoDef.position.set(torsoPos);
        torsoDef.angle = torsoAng + torsoAngAdj;

        torsoMassData.I = torsoInertia;
        torsoMassData.mass = torsoMass;
        torsoDef.massData = torsoMassData;

        /* HEAD */
//        headShape.radius = (headR);
        headShape.setAsBox(headW, headL);
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
        rUArmShape.restitution = 0f;
        lUArmShape.restitution = 0f;
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
        rLArmShape.restitution = 0f;
        lLArmShape.restitution = 0f;
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

        initState = new GameUnified().getCurrentState();
    }

    public void makeNewWorld() {

        isFailed = false;
        timestepsSimulated = 0;

        /* World Settings */
        world = new World(worldAABB, gravity, true);
        world.setWarmStarting(useWarmStarting);
        world.setPositionCorrection(usePositionCorrection);
        world.setContinuousPhysics(true);

        // NOTE: The order of creating bodies actually changes the answers slightly!! This is really dumb, but will
        // affect us if we are trying to match the single and multithreaded version.

        /* L UPPER ARM */
        lUArmBody = getWorld().createBody(lUArmDef);
        lUArmBody.createShape(lUArmShape);

        /* L LOWER ARM */
        lLArmBody = getWorld().createBody(lLArmDef);
        lLArmBody.createShape(lLArmShape);

        /* L CALF */
        lCalfBody = getWorld().createBody(lCalfDef);
        lCalfBody.createShape(lCalfShape);

        /* LFOOT */
        if (!noFeet) {
            lFootBody = getWorld().createBody(lFootDef);
            lFootBody.createShape(lFootShape);
        }

        /* L THIGH */
        lThighBody = getWorld().createBody(lThighDef);
        lThighBody.createShape(lThighShape);

        /* TORSO */
        torsoBody = getWorld().createBody(torsoDef);
        torsoBody.createShape(torsoShape);

        /* R UPPER ARM */
        rUArmBody = getWorld().createBody(rUArmDef);
        rUArmBody.createShape(rUArmShape);

        /* R CALF */
        rCalfBody = getWorld().createBody(rCalfDef);
        rCalfBody.createShape(rCalfShape);

        /* R THIGH */
        rThighBody = getWorld().createBody(rThighDef);
        rThighBody.createShape(rThighShape);

        /* R LOWER ARM */
        rLArmBody = getWorld().createBody(rLArmDef);
        rLArmBody.createShape(rLArmShape);

        /* HEAD */
        headBody = getWorld().createBody(headDef);
        headBody.createShape(headShape);

        /* RFOOT */
        if (!noFeet) {
            rFootBody = getWorld().createBody(rFootDef);
            rFootBody.createShape(rFootShape);
        }

        /* TRACK */
        trackBody = world.createBody(trackDef);
        trackBody.createShape(trackShape);

        allBodies = new Body[]{rCalfBody, lCalfBody, rThighBody, lThighBody, torsoBody, rUArmBody,
                lUArmBody, rLArmBody, lLArmBody, rFootBody, lFootBody, headBody};


//        BodyDef blockBodyDef = new BodyDef();
//        PolygonDef blockShapeDef = new PolygonDef();
//        MassData blockMassData = new MassData();
//        blockShapeDef.setAsBox(5, 5);
//
//        blockShapeDef.friction = 1;
//        blockShapeDef.density = 1;
//        blockShapeDef.tree.node.filter.groupIndex = 1; // Same as track.
//
//        blockBodyDef.position = (torsoPos).add(new Vec2(20,-2));
//        blockBodyDef.angle = 0;
//
//        blockMassData.I = 75;
//        blockMassData.mass = 25;
//        blockBodyDef.massData = blockMassData;
//        Body blockBody = getWorld().createBody(blockBodyDef);
//        blockBody.createShape(blockShapeDef);

        /*
         *  Joints
         */

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

        if (!noFeet) {

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
        }

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

        /* Arm Joints */
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
//
//
//        if (getTimestepsThisGame() % 10 == 0) {
//            BodyDef blockBodyDef = new BodyDef();
//            PolygonDef blockShapeDef = new PolygonDef();
//            MassData blockMassData = new MassData();
//            float x = rand.nextFloat() * 2 + 0.2f;
//            float y = rand.nextFloat() * 0.5f + 0.2f;
//            blockShapeDef.setAsBox(x, y);
//
//            blockShapeDef.friction = 1;
//            blockShapeDef.density = 1;
//            blockShapeDef.tree.node.filter.groupIndex = 1; // Same as track.
//
//            blockBodyDef.position = (torsoBody.getPosition()).add(new Vec2(30,4));
//            blockBodyDef.angle = 2 * 3.14f * rand.nextFloat();
//
//            blockMassData.I = x * y * 4;
//            blockMassData.mass = x * y;
//            blockBodyDef.massData = blockMassData;
//            Body blockBody = getWorld().createBody(blockBodyDef);
//            blockBody.createShape(blockShapeDef);
//        }



        /* Involuntary Couplings (no QWOP presses) */
        //Neck spring torque
        float NeckTorque = -neckStiff * neckJ.getJointAngle(); //  + 0 * neckDamp * neckJ.getJointSpeed(); // These
        // *0 terms were in the real game, but I'm commenting out here.
        NeckTorque = NeckTorque + 0 * 400f * (neckJ.getJointAngle() + 0.2f); //This bizarre term is probably a roundabout way of adjust equilibrium position.

        //Elbow spring torque
        float RElbowTorque = -rElbowStiff * rElbowJ.getJointAngle(); // + 0 * rElbowDamp * rElbowJ.getJointSpeed();
        float LElbowTorque = -lElbowStiff * lElbowJ.getJointAngle(); // + 0 * lElbowDamp * lElbowJ.getJointSpeed();

        //For now, using motors with high speed settings and torque limits to simulate springs. I don't know a better way for now.

        neckJ.m_motorSpeed = (1000000f * Math.signum(NeckTorque)); //If torque is negative, make motor speed negative.
        rElbowJ.m_motorSpeed = (1000000f * Math.signum(RElbowTorque));
        lElbowJ.m_motorSpeed = (1000000f * Math.signum(LElbowTorque));

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
            float RAnkleCur = rAnkleJ.getAnchor1XCoord();
            float LAnkleCur = lAnkleJ.getAnchor1XCoord();

            float RHipCur = rHipJ.getAnchor1XCoord();


            // if right ankle joint is behind the right hip jiont
            // Set ankle motor speed to 1;
            // else speed 2
            if (RAnkleCur < RHipCur) {
                rAnkleJ.m_motorSpeed = (rAnkleSpeed2);
            } else {
                rAnkleJ.m_motorSpeed = (rAnkleSpeed1);
            }


            // if left ankle joint is behind RIGHT hip joint (weird it's the right one here too)
            // Set its motor speed to 1;
            // else speed 2;
            if (LAnkleCur < RHipCur) {
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


        getWorld().step(timestep, iterations);

        // Extra fail conditions besides contacts.
        float angle = torsoBody.getAngle();
        if (angle > torsoAngUpper || angle < torsoAngLower) { // Fail if torso angles get too far out of whack.
            isFailed = true;
        }

        timestepsSimulated++;
    }

    /**
     * Simple convenience method for calling {@link GameUnified#step(boolean, boolean, boolean, boolean)} but for
     * multiple timesteps.
     * @param timesteps Number of timesteps to simulate ahead while holding these keys.
     * @param q Whether q key is down.
     * @param w Whether w key is down.
     * @param o Whether o key is down.
     * @param p Whether p key is down.
     */
    public void holdKeysForTimesteps(int timesteps, boolean q, boolean w, boolean o, boolean p) {
        for (int i = 0; i < timesteps; i++) {
            step(q,w,o,p);
        }
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
    public static IState getInitialState() {
        return initState;
    }

    /**
     * Get the current full state of the runner.
     */
    public synchronized IState getCurrentState() {
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


    // Avoid new allocations for what amounts to a temporary variable.
    private final Vec2 setPos = new Vec2();
    private final Vec2 setLinVel = new Vec2();
    /**
     * Set an individual body to a specified {@link StateVariable}. This sets both positions and velocities.
     *
     * @param body          Body to set the state of.
     * @param stateVariable Full state to assign to that body.
     */
    private void setBodyToStateVariable(Body body, StateVariable stateVariable) {
        setPos.x = stateVariable.getX();
        setPos.y = stateVariable.getY();
        setLinVel.x = stateVariable.getDx();
        setLinVel.y = stateVariable.getDy();
        body.setXForm(setPos, stateVariable.getTh());
        body.setLinearVelocity(setLinVel);
        body.setAngularVelocity(stateVariable.getDth());
    }

    public void setState(IState state) {
        isFailed = false;
        setBodyToStateVariable(rFootBody, state.getStateVariableFromName(ObjectName.RFOOT));
        setBodyToStateVariable(lFootBody, state.getStateVariableFromName(ObjectName.LFOOT));

        setBodyToStateVariable(rThighBody, state.getStateVariableFromName(ObjectName.RTHIGH));
        setBodyToStateVariable(lThighBody, state.getStateVariableFromName(ObjectName.LTHIGH));

        setBodyToStateVariable(rCalfBody, state.getStateVariableFromName(ObjectName.RCALF));
        setBodyToStateVariable(lCalfBody, state.getStateVariableFromName(ObjectName.LCALF));

        setBodyToStateVariable(rUArmBody, state.getStateVariableFromName(ObjectName.RUARM));
        setBodyToStateVariable(lUArmBody, state.getStateVariableFromName(ObjectName.LUARM));

        setBodyToStateVariable(rLArmBody, state.getStateVariableFromName(ObjectName.RLARM));
        setBodyToStateVariable(lLArmBody, state.getStateVariableFromName(ObjectName.LLARM));

        setBodyToStateVariable(headBody, state.getStateVariableFromName(ObjectName.HEAD));
        setBodyToStateVariable(torsoBody, state.getStateVariableFromName(ObjectName.BODY));
    }

    public GameUnified getCopy() {
        return new GameUnified();
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
    @Override
    public long getTimestepsThisGame() {
        return timestepsSimulated;
    }

    @Override
    public int getStateDimension() {
        return STATE_SIZE;
    }

    public boolean isRightFootDown() {
        return collisionListener.isRightFootGrounded();
    }

    public boolean isLeftFootDown() {
        return collisionListener.isLeftFootGrounded();
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

    public static void setPointFeet(boolean usePointFeet) {
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


//    public void fullStatePDController(State targetState) {
//        State currentState = getCurrentState();
//        pdForce(targetState.body, currentState.body, torsoBody);
//        pdTorque(targetState.body, currentState.body, torsoBody);
//
//        pdForce(targetState.head, currentState.head, torsoBody);
//        pdTorque(targetState.head, currentState.head, torsoBody);
//
//        pdForce(targetState.rthigh, currentState.rthigh, torsoBody);
//        pdTorque(targetState.rthigh, currentState.rthigh, torsoBody);
//
//        pdForce(targetState.lthigh, currentState.lthigh, torsoBody);
//        pdTorque(targetState.lthigh, currentState.lthigh, torsoBody);
//
//        pdForce(targetState.rcalf, currentState.rcalf, torsoBody);
//        pdTorque(targetState.rcalf, currentState.rcalf, torsoBody);
//
//        pdForce(targetState.lcalf, currentState.lcalf, torsoBody);
//        pdTorque(targetState.lcalf, currentState.lcalf, torsoBody);
//
//        pdForce(targetState.rfoot, currentState.rfoot, torsoBody);
//        pdTorque(targetState.rfoot, currentState.rfoot, torsoBody);
//
//        pdForce(targetState.lfoot, currentState.lfoot, torsoBody);
//        pdTorque(targetState.lfoot, currentState.lfoot, torsoBody);
//
//        pdForce(targetState.ruarm, currentState.ruarm, torsoBody);
//        pdTorque(targetState.ruarm, currentState.ruarm, torsoBody);
//
//        pdForce(targetState.luarm, currentState.luarm, torsoBody);
//        pdTorque(targetState.luarm, currentState.luarm, torsoBody);
//
//        pdForce(targetState.rlarm, currentState.rlarm, torsoBody);
//        pdTorque(targetState.rlarm, currentState.rlarm, torsoBody);
//
//        pdForce(targetState.llarm, currentState.llarm, torsoBody);
//        pdTorque(targetState.llarm, currentState.llarm, torsoBody);
//    }

    private void pdForce(StateVariable targetSV, StateVariable currentSV, Body b) {
        float kp = 10;
        float kd = kp/10;

        b.applyForce(new Vec2(kp * (targetSV.getX() - currentSV.getX()) + kd * (targetSV.getDx() - currentSV.getDx()),
                        kp * (targetSV.getY() - currentSV.getY()) + kd * (targetSV.getDy() - currentSV.getDy())),
                b.getWorldCenter());
    }

    private void pdTorque(StateVariable targetSV, StateVariable currentSV, Body b) {
        float kp = 10f;
        float kd = kp/10f;
        b.applyTorque(kp * (targetSV.getTh() - currentSV.getTh()) +
                kd * (targetSV.getDth() - currentSV.getDth()));

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

    @Override
    public void command(boolean q, boolean w, boolean o, boolean p) {
        step(q, w, o, p);
    }

    @Override
    public void command(boolean[] commands) {
        step(commands);
    }

    @SuppressWarnings("WeakerAccess")
    static class VertHolder {
        public float torsoX;
        public float groundHeight;
        public float[][] bodyVerts = new float[11][8];
        public float[] headLocAndRadius = new float[3];
    }

    public void doAction(Action action) {
        Action a = action.getCopy();
        while (a.hasNext()) {
            command(a.poll());
        }
    }

    /**
     * Draw this game's runner. Must provide scaling from game units to pixels, as well as pixel offsets in x and y.
     **/
    public void draw(Graphics g, float scaling, int xOffset, int yOffset) {

        Body newBody = getWorld().getBodyList();
        while (newBody != null) {
            int xOffsetPixels = -(int) (scaling * torsoBody.getPosition().x) + xOffset; // Basic offset, plus centering x on torso.
            Shape newfixture = newBody.getShapeList();


            g.setColor(Color.RED);
            for (Body body : getAllBodies()) {
                if (newBody == body || newBody == trackBody) {
                    g.setColor(Color.BLACK);
                    break;
                }
            }

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
            g.setColor(Color.BLACK);
            g.drawString("_", ((-(int) (scaling * torsoBody.getPosition().x) - i * 70) % markingWidth) + markingWidth
                    , (int) (yOffset + 9.2 * scaling));
        }
    }

    /**
     * Draw the runner at a specified set of transforms..
     **/
    public static void drawExtraRunner(Graphics2D g, IState state, String label, float scaling, int xOffset,
                                       int yOffset, Color drawColor, Stroke stroke) {

        XForm[] transforms = getXForms(state);
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
     * Get the transform associated with this State. Note that these transforms can ONLY be used with this instance
     * of GameThreadSafe.
     */
    public static XForm[] getXForms(IState st) {
        XForm[] transforms = new XForm[13];
        transforms[0] = getXForm(st.getStateVariableFromName(ObjectName.BODY));
        transforms[1] = getXForm(st.getStateVariableFromName(ObjectName.HEAD));
        transforms[2] = getXForm(st.getStateVariableFromName(ObjectName.RFOOT));
        transforms[3] = getXForm(st.getStateVariableFromName(ObjectName.LFOOT));
        transforms[4] = getXForm(st.getStateVariableFromName(ObjectName.RCALF));
        transforms[5] = getXForm(st.getStateVariableFromName(ObjectName.LCALF));
        transforms[6] = getXForm(st.getStateVariableFromName(ObjectName.RTHIGH));
        transforms[7] = getXForm(st.getStateVariableFromName(ObjectName.LTHIGH));
        transforms[8] = getXForm(st.getStateVariableFromName(ObjectName.RUARM));
        transforms[9] = getXForm(st.getStateVariableFromName(ObjectName.LUARM));
        transforms[10] = getXForm(st.getStateVariableFromName(ObjectName.RLARM));
        transforms[11] = getXForm(st.getStateVariableFromName(ObjectName.LLARM));
        transforms[12] = getXForm(new StateVariable(0, trackPosY, 0, 0, 0, 0)); // Hardcoded for track.
        // Offset by 20 because its now a box.
        return transforms;
    }

    /**
     * Get the transform associated with this body's state variables. Note that these transforms can ONLY be used
     * with this instance of GameThreadSafe.
     */
    public static XForm getXForm(StateVariable sv) {
        XForm xf = new XForm();
        xf.position.x = sv.getX();
        xf.position.y = sv.getY();
        xf.R.set(sv.getTh());
        return xf;
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

    @Override
    public synchronized byte[] getSerializedState() {
        return fstConfiguration.asByteArray(this);
    }

    @Override
    public GameUnified restoreSerializedState(byte[] fullState) {
        GameUnified gameRestored = (GameUnified) fstConfiguration.asObject(fullState);
        // Replace all the relevant game fields which have been loaded.
        assert gameRestored != null;
        return gameRestored;
    }
}
