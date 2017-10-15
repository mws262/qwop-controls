package main;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.shapes.CircleDef;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;


public class QWOPGame{// extends JFrame{

	/* World object */
	public World m_world;

	/* Individual body objects */
	public Body RFootBody;
	public Body LFootBody;
	public Body RCalfBody;
	public Body LCalfBody;
	public Body RThighBody;
	public Body LThighBody;
	public Body TorsoBody;
	public Body RUArmBody;
	public Body LUArmBody;
	public Body RLArmBody;
	public Body LLArmBody;
	public Body HeadBody;
	public Body TrackBody;

	/* Joint Definitions */
	public RevoluteJointDef RHipJDef;
	public RevoluteJointDef LHipJDef;
	public RevoluteJointDef RKneeJDef;
	public RevoluteJointDef LKneeJDef;
	public RevoluteJointDef RAnkleJDef;
	public RevoluteJointDef LAnkleJDef;
	public RevoluteJointDef RShoulderJDef;
	public RevoluteJointDef LShoulderJDef;
	public RevoluteJointDef RElbowJDef;
	public RevoluteJointDef LElbowJDef;
	public RevoluteJointDef NeckJDef;

	/* Joint objects */
	public RevoluteJoint RHipJ;
	public RevoluteJoint LHipJ;
	public RevoluteJoint RKneeJ;
	public RevoluteJoint LKneeJ;
	public RevoluteJoint RAnkleJ;
	public RevoluteJoint LAnkleJ;
	public RevoluteJoint RShoulderJ;
	public RevoluteJoint LShoulderJ;
	public RevoluteJoint RElbowJ;
	public RevoluteJoint LElbowJ;
	public RevoluteJoint NeckJ;

	final int BODY_GROUP = -1;

	// Track
	float TrackPosY = 8.90813f;
	float TrackFric = 1f;
	float TrackRest = 0.2f;

	// Feet
	Vec2 RFootPos = new Vec2(-0.96750f,7.77200f);
	Vec2 LFootPos = new Vec2(3.763f,8.101f);
	float RFootAng = 0.7498f;
	float LFootAng = 0.1429f;
	float RFootMass = 11.630f;
	float LFootMass = 10.895f;
	float RFootInertia = 9.017f;
	float LFootInertia = 8.242f;
	float RFootL = 2.68750f;
	float LFootL = 2.695f;
	float RFootH = 1.44249f;
	float LFootH = 1.34750f;
	float RFootFric = 1.5f;
	float LFootFric = 1.5f;
	float RFootDensity = 3f;
	float LFootDensity = 3f;

	// Calves
	Vec2 RCalfPos = new Vec2(0.0850f,5.381f);
	Vec2 LCalfPos = new Vec2(2.986f,5.523f);
	float RCalfAng = -0.821f;
	float LCalfAng = -1.582f;
	static float RCalfAngAdj = 1.606188724f;
	static float LCalfAngAdj = 1.607108307f;
	float RCalfMass = 7.407f;
	float LCalfMass = 7.464f;
	float RCalfInertia = 16.644f;
	float LCalfInertia = 16.893f;

	//Length and width for the calves are just for collisions with the ground, so not very important.
	float RCalfL = 4.21f;
	float LCalfL = 4.43f;
	float RCalfW = 0.4f;
	float LCalfW = 0.4f;
	float RCalfFric = 0.2f;
	float LCalfFric = 0.2f;
	float RCalfDensity = 1f;
	float LCalfDensity = 1f;

	// Thighs
	Vec2 RThighPos = new Vec2(1.659f,1.999f);
	Vec2 LThighPos = new Vec2(2.52f,1.615f);
	float RThighAng = 1.468f;
	float LThighAng = -1.977f;
	static float RThighAngAdj = -1.544382589f;
	static float LThighAngAdj = 1.619256373f;
	float RThighMass = 10.54f;
	float LThighMass = 10.037f;
	float RThighInertia = 28.067f;
	float LThighInertia = 24.546f;

	//Length and width for the calves are just for collisions with the ground, so not very important.
	float RThighL = 4.19f;
	float LThighL = 3.56f;
	float RThighW = 0.6f;
	float LThighW = 0.6f;
	float RThighFric = 0.2f;
	float LThighFric = 0.2f;
	float RThighDensity = 1f;
	float LThighDensity = 1f;

	// Torso
	Vec2 TorsoPos = new Vec2(2.525f,-1.926f);
	float TorsoAng = -1.251f;
	static float TorsoAngAdj = 1.651902129f;
	float TorsoMass = 18.668f;
	float TorsoInertia = 79.376f;

	//Length and width for the calves are just for collisions with the ground, so not very important.
	float TorsoL = 5f;
	float TorsoW = 1.5f;
	float TorsoFric = 0.2f;
	float TorsoDensity = 1f;

	// Head
	Vec2 HeadPos = new Vec2(3.896f,-5.679f);
	float HeadAng = 0.058f;
	float HeadMass = 5.674f;
	static float HeadAngAdj = 0.201921414f;
	float HeadInertia = 5.483f;

	//Radius is just for collision shape
	float HeadR = 1.1f;
	float HeadFric = 0.2f;
	float HeadDensity = 1f;

	// Upper arms
	Vec2 RUArmPos = new Vec2(1.165f,-3.616f);
	Vec2 LUArmPos = new Vec2(4.475f,-2.911f);
	float RUArmAng = -0.466f;
	float LUArmAng = 0.843f;
	static float RUArmAngAdj = 1.571196588f;
	static float LUArmAngAdj = -1.690706418f;

	float RUArmMass = 5.837f;
	float LUArmMass = 4.6065f;
	float RUArmInertia = 8.479f;
	float LUArmInertia = 5.85f;

	//for collision shapes
	float RUArmL = 2.58f;
	float LUArmL = 2.68f;
	float RUArmW = 0.2f;
	float LUArmW = 0.15f;
	float RUArmFric = 0.2f;
	float LUArmFric = 0.2f;
	float RUArmDensity = 1f;
	float LUArmDensity = 1f;

	// Lower Arms
	Vec2 RLArmPos = new Vec2(0.3662f,-1.248f);
	Vec2 LLArmPos = new Vec2(5.899f,-3.06f);
	float RLArmAng = -1.762f;
	float LLArmAng = -1.251f;
	static float RLArmAngAdj = 1.521319096f;
	static float LLArmAngAdj = 1.447045854f;
	float RLArmMass = 5.99f;
	float LLArmMass = 3.8445f;
	float RLArmInertia = 10.768f;
	float LLArmInertia = 4.301f;

	//for collision shapes
	float RLArmL = 3.56f;
	float LLArmL = 2.54f;
	float RLArmW = 0.15f;
	float LLArmW = 0.12f;
	float RLArmFric = 0.2f;
	float LLArmFric = 0.2f;
	float RLArmDensity = 1f;
	float LLArmDensity = 1f;

	
	/** List of shapes for use by graphics stuff. Making it static -- IE, assuming that in multiple games, the runner doesn't change shape. **/
	public static Shape[] shapeList = new Shape[13];
	
	public QWOPGame(){}

	public void Setup() {
		/*
		 * World Settings
		 */
		Vec2 gravity = new Vec2(0, 10f);
		m_world = new World(new AABB(new Vec2(-100, -30f), new Vec2(5000f,80f)), gravity, true);
		m_world.setWarmStarting(true);
		m_world.setPositionCorrection(true);
		m_world.setContinuousPhysics(true);
		/* 
		 * Make the bodies and collision shapes
		 */

		/* TRACK */
		BodyDef TrackDef = new BodyDef();
		TrackDef.position = new Vec2(-30,TrackPosY + 20);
		PolygonDef TrackShape = new PolygonDef();
		TrackShape.setAsBox(1000, 20);
		TrackShape.restitution = TrackRest;
		TrackShape.friction = TrackFric;

		TrackShape.filter.groupIndex = 1;

		TrackBody = m_world.createBody(TrackDef);
		TrackBody.createShape(TrackShape);


		/* FEET */
		//Create the fixture shapes, IE collision shapes.
		PolygonDef RFootShape = new PolygonDef();
		PolygonDef LFootShape = new PolygonDef();

		RFootShape.setAsBox(RFootL/2f, RFootH/2f);
		LFootShape.setAsBox(LFootL/2f, LFootH/2f);

		RFootShape.friction = (RFootFric);
		LFootShape.friction = (LFootFric);
		RFootShape.density = RFootDensity;
		LFootShape.density = LFootDensity;

		RFootShape.filter.groupIndex = BODY_GROUP;
		LFootShape.filter.groupIndex = BODY_GROUP;

		//Dynamics body definitions
		BodyDef RFootDef = new BodyDef();
		BodyDef LFootDef = new BodyDef();

		RFootDef.position = RFootPos;
		RFootDef.angle = RFootAng;

		LFootDef.position.set(LFootPos);
		LFootDef.angle = LFootAng;

		MassData LFootMassData = new MassData();
		LFootMassData.mass = LFootMass;
		LFootMassData.I = LFootInertia;
		LFootDef.massData = LFootMassData;
		
		MassData RFootMassData = new MassData();
		RFootMassData.mass = RFootMass;
		RFootMassData.I = RFootInertia;
		RFootDef.massData = RFootMassData;

		RFootBody = getWorld().createBody(RFootDef);
		LFootBody = getWorld().createBody(LFootDef);

		RFootBody.createShape(RFootShape);
		LFootBody.createShape(LFootShape);


		/* CALVES */  
		PolygonDef RCalfShape = new PolygonDef();
		PolygonDef LCalfShape = new PolygonDef();

		RCalfShape.setAsBox(RCalfW/2f, RCalfL/2f);
		LCalfShape.setAsBox(LCalfW/2f, LCalfL/2f);

		RCalfShape.friction = RCalfFric;
		LCalfShape.friction = LCalfFric;
		RCalfShape.density = RCalfDensity;
		LCalfShape.density = LCalfDensity;

		RCalfShape.filter.groupIndex = BODY_GROUP;
		LCalfShape.filter.groupIndex = BODY_GROUP;

		BodyDef RCalfDef = new BodyDef();
		BodyDef LCalfDef = new BodyDef();

		RCalfDef.position = (RCalfPos);
		RCalfDef.angle = RCalfAng + RCalfAngAdj;

		LCalfDef.position = (LCalfPos);
		LCalfDef.angle = LCalfAng + LCalfAngAdj;

		MassData RCalfMassData = new MassData();
		MassData LCalfMassData = new MassData();

		RCalfMassData.I = RCalfInertia;
		RCalfMassData.mass = RCalfMass;

		LCalfMassData.I = LCalfInertia;
		LCalfMassData.mass = LCalfMass;

		RCalfDef.massData = RCalfMassData;
		LCalfDef.massData = LCalfMassData;


		RCalfBody = getWorld().createBody(RCalfDef);
		LCalfBody = getWorld().createBody(LCalfDef);

		RCalfBody.createShape(RCalfShape);
		LCalfBody.createShape(LCalfShape);


		/* THIGHS */
		PolygonDef RThighShape = new PolygonDef();
		PolygonDef LThighShape = new PolygonDef();

		RThighShape.setAsBox(RThighW/2f, RThighL/2f);
		LThighShape.setAsBox(LThighW/2f, LThighL/2f);

		RThighShape.friction = RThighFric;
		LThighShape.friction = LThighFric;
		RThighShape.density = RThighDensity;
		LThighShape.density = LThighDensity;

		RThighShape.filter.groupIndex = BODY_GROUP;
		LThighShape.filter.groupIndex = BODY_GROUP;

		BodyDef RThighDef = new BodyDef();
		BodyDef LThighDef = new BodyDef();

		RThighDef.position.set(RThighPos);
		LThighDef.position.set(LThighPos);

		RThighDef.angle = RThighAng + RThighAngAdj;
		LThighDef.angle = LThighAng + LThighAngAdj;

		MassData RThighMassData = new MassData();
		MassData LThighMassData = new MassData();

		RThighMassData.I = RThighInertia;
		RThighMassData.mass = RThighMass;

		LThighMassData.I = LThighInertia;
		LThighMassData.mass = LThighMass;

		RThighDef.massData = RThighMassData;
		LThighDef.massData = LThighMassData;

		RThighBody = getWorld().createBody(RThighDef);
		LThighBody = getWorld().createBody(LThighDef);

		RThighBody.createShape(RThighShape);
		LThighBody.createShape(LThighShape);

		/* TORSO */
		PolygonDef TorsoShape = new PolygonDef();
		TorsoShape.setAsBox(TorsoW/2f, TorsoL/2f);
		TorsoShape.friction = TorsoFric;
		TorsoShape.density = TorsoDensity;

		TorsoShape.filter.groupIndex = BODY_GROUP;

		BodyDef TorsoDef = new BodyDef();
		TorsoDef.position.set(TorsoPos);
		TorsoDef.angle = TorsoAng + TorsoAngAdj;

		MassData TorsoMassData = new MassData();

		TorsoMassData.I = TorsoInertia;
		TorsoMassData.mass = TorsoMass;

		TorsoDef.massData = TorsoMassData;

		TorsoBody = getWorld().createBody(TorsoDef);
		TorsoBody.createShape(TorsoShape);

		/* HEAD */      
		CircleDef HeadShape = new CircleDef();
		HeadShape.radius = (HeadR);
		HeadShape.friction = HeadFric;
		HeadShape.density = HeadDensity;
		HeadShape.restitution = 0f;

		HeadShape.filter.groupIndex = BODY_GROUP;

		BodyDef HeadDef = new BodyDef();
		HeadDef.position.set(HeadPos);
		HeadDef.angle = HeadAng + HeadAngAdj;

		MassData HeadMassData = new MassData();

		HeadMassData.I = HeadInertia;
		HeadMassData.mass = HeadMass;
		HeadDef.massData = HeadMassData;

		HeadBody = getWorld().createBody(HeadDef);
		HeadBody.createShape(HeadShape);


		/* UPPER ARMS */
		PolygonDef RUArmShape = new PolygonDef();
		PolygonDef LUArmShape = new PolygonDef();

		RUArmShape.setAsBox(RUArmW/2f,RUArmL/2f);
		LUArmShape.setAsBox(LUArmW/2f,LUArmL/2f);

		RUArmShape.friction = RUArmFric;
		LUArmShape.friction = LUArmFric;
		RUArmShape.density = RUArmDensity;
		LUArmShape.density = LUArmDensity;

		RUArmShape.filter.groupIndex = BODY_GROUP;
		LUArmShape.filter.groupIndex = BODY_GROUP;

		BodyDef RUArmDef = new BodyDef();
		BodyDef LUArmDef = new BodyDef();

		RUArmDef.position.set(RUArmPos);
		LUArmDef.position.set(LUArmPos);

		RUArmDef.angle = RUArmAng + RUArmAngAdj;
		LUArmDef.angle = LUArmAng + LUArmAngAdj; 

		MassData RUArmMassData = new MassData();
		MassData LUArmMassData = new MassData();

		RUArmMassData.I = RUArmInertia;
		RUArmMassData.mass = RUArmMass;

		LUArmMassData.I = LUArmInertia;
		LUArmMassData.mass = LUArmMass;

		RUArmDef.massData = RUArmMassData;
		LUArmDef.massData = LUArmMassData;

		RUArmBody = getWorld().createBody(RUArmDef);
		LUArmBody = getWorld().createBody(LUArmDef);

		RUArmBody.createShape(RUArmShape);
		LUArmBody.createShape(LUArmShape);    

		/* LOWER ARMS */  
		PolygonDef RLArmShape = new PolygonDef();
		PolygonDef LLArmShape = new PolygonDef();

		RLArmShape.setAsBox(RLArmW/2f, RLArmL/2f);
		LLArmShape.setAsBox(LLArmW/2f, LLArmL/2f);

		RLArmShape.friction = RLArmFric;
		LLArmShape.friction = LLArmFric;
		RLArmShape.density = RLArmDensity;
		LLArmShape.density = LLArmDensity;

		RLArmShape.filter.groupIndex = BODY_GROUP;
		LLArmShape.filter.groupIndex = BODY_GROUP;

		BodyDef RLArmDef = new BodyDef();
		BodyDef LLArmDef = new BodyDef();

		RLArmDef.position.set(RLArmPos);
		LLArmDef.position.set(LLArmPos);

		RLArmDef.angle = RLArmAng + RLArmAngAdj;
		LLArmDef.angle = LLArmAng + LLArmAngAdj;

		MassData RLArmMassData = new MassData();
		MassData LLArmMassData = new MassData();

		RLArmMassData.I = RLArmInertia;
		RLArmMassData.mass = RLArmMass;

		LLArmMassData.I = LLArmInertia;
		LLArmMassData.mass = LLArmMass;

		RLArmDef.massData = RLArmMassData;
		LLArmDef.massData = LLArmMassData;

		RLArmBody = getWorld().createBody(RLArmDef);
		LLArmBody = getWorld().createBody(LLArmDef);

		RLArmBody.createShape(RLArmShape);
		LLArmBody.createShape(LLArmShape);

		/*
		 *  Joints
		 */

		/* Joints Positions*/

		Vec2 RAnklePos = new Vec2(-0.96750f,7.77200f);
		Vec2 LAnklePos = new Vec2(3.763f,8.101f);

		Vec2 RKneePos = new Vec2(1.58f,4.11375f);
		Vec2 LKneePos = new Vec2(3.26250f,3.51625f);

		Vec2 RHipPos = new Vec2(1.260f,-0.06750f);
		Vec2 LHipPos = new Vec2(2.01625f,0.18125f);

		Vec2 RShoulderPos = new Vec2(2.24375f,-4.14250f);
		Vec2 LShoulderPos = new Vec2(3.63875f,-3.58875f);

		Vec2 RElbowPos = new Vec2(-0.06f,-2.985f);
		Vec2 LElbowPos = new Vec2(5.65125f,-1.8125f);

		Vec2 NeckPos = new Vec2(3.60400f,-4.581f);

		//Right Ankle:

		RAnkleJDef = new RevoluteJointDef(); 
		RAnkleJDef.initialize(RFootBody,RCalfBody, RAnklePos); //Body1, body2, anchor in world coords
		RAnkleJDef.enableLimit = true;
		RAnkleJDef.upperAngle = 0.5f;
		RAnkleJDef.lowerAngle = -0.5f;
		RAnkleJDef.enableMotor = false;
		RAnkleJDef.maxMotorTorque = 2000f;
		RAnkleJDef.motorSpeed = 0f; // Speed1,2: -2,2
		RAnkleJDef.collideConnected = false;

		RAnkleJ = (RevoluteJoint)getWorld().createJoint(RAnkleJDef);

		//Left Ankle:
		LAnkleJDef = new RevoluteJointDef();
		LAnkleJDef.initialize(LFootBody,LCalfBody, LAnklePos);
		LAnkleJDef.enableLimit = true;
		LAnkleJDef.upperAngle = 0.5f;
		LAnkleJDef.lowerAngle = -0.5f;
		LAnkleJDef.enableMotor = false;
		LAnkleJDef.maxMotorTorque = 2000;
		LAnkleJDef.motorSpeed = 0f;// Speed1,2: 2,-2
		LAnkleJDef.collideConnected = false;

		LAnkleJ = (RevoluteJoint)getWorld().createJoint(LAnkleJDef);

		/* Knee joints */

		//Right Knee:
		RKneeJDef = new RevoluteJointDef();
		RKneeJDef.initialize(RCalfBody,RThighBody, RKneePos);
		RKneeJDef.enableLimit = true;
		RKneeJDef.upperAngle = 0.3f;
		RKneeJDef.lowerAngle = -1.3f;
		RKneeJDef.enableMotor = true;//?
		RKneeJDef.maxMotorTorque = 3000;
		RKneeJDef.motorSpeed = 0f; //Speeds 1,2: -2.5,2.5
		RKneeJDef.collideConnected = false;

		RKneeJ = (RevoluteJoint)getWorld().createJoint(RKneeJDef);

		//Left Knee:
		LKneeJDef = new RevoluteJointDef();
		LKneeJDef.initialize(LCalfBody,LThighBody, LKneePos);
		LKneeJDef.enableLimit = true;
		LKneeJDef.upperAngle = 0f;
		LKneeJDef.lowerAngle = -1.6f;
		LKneeJDef.enableMotor = true;
		LKneeJDef.maxMotorTorque = 3000;
		LKneeJDef.motorSpeed = 0f;// Speed1,2: -2.5,2.5
		LKneeJDef.collideConnected = false;

		LKneeJ = (RevoluteJoint)getWorld().createJoint(LKneeJDef);

		/* Hip Joints */

		//Right Hip:
		RHipJDef = new RevoluteJointDef();
		RHipJDef.initialize(RThighBody,TorsoBody, RHipPos);
		RHipJDef.enableLimit = true;
		RHipJDef.upperAngle = 0.7f;
		RHipJDef.lowerAngle = -1.3f;
		RHipJDef.enableMotor = true;
		RHipJDef.motorSpeed = 0f;
		RHipJDef.maxMotorTorque = 6000f;
		RHipJDef.collideConnected = false;

		RHipJ = (RevoluteJoint)getWorld().createJoint(RHipJDef);

		//Left Hip:
		LHipJDef = new RevoluteJointDef();
		LHipJDef.initialize(LThighBody,TorsoBody, LHipPos);
		LHipJDef.enableLimit = true;
		LHipJDef.upperAngle = 0.5f;
		LHipJDef.lowerAngle = -1.5f;
		LHipJDef.enableMotor = true;
		LHipJDef.motorSpeed = 0f;
		LHipJDef.maxMotorTorque = 6000f;
		LHipJDef.collideConnected = false;
		LHipJ = (RevoluteJoint)getWorld().createJoint(LHipJDef);


		//Neck Joint
		NeckJDef = new RevoluteJointDef();
		NeckJDef.initialize(HeadBody,TorsoBody, NeckPos);
		NeckJDef.enableLimit = true;
		NeckJDef.upperAngle = 0f;
		NeckJDef.lowerAngle = -0.5f;
		NeckJDef.enableMotor = true;
		NeckJDef.maxMotorTorque = 0f;
		NeckJDef.motorSpeed = 1000f; //Arbitrarily large to allow for torque control.  
		NeckJDef.collideConnected = false;
		NeckJ = (RevoluteJoint)getWorld().createJoint(NeckJDef);

		/* Arm Joints */
		//Right shoulder
		RShoulderJDef = new RevoluteJointDef();
		RShoulderJDef.initialize(RUArmBody,TorsoBody, RShoulderPos);
		RShoulderJDef.enableLimit = true;
		RShoulderJDef.upperAngle = 1.5f;
		RShoulderJDef.lowerAngle = -0.5f;
		RShoulderJDef.enableMotor = true;
		RShoulderJDef.maxMotorTorque = 1000f;
		RShoulderJDef.motorSpeed = 0f; // Speed 1,2: 2,-2
		RShoulderJDef.collideConnected = false;
		RShoulderJ = (RevoluteJoint)getWorld().createJoint(RShoulderJDef);

		//Left shoulder
		LShoulderJDef = new RevoluteJointDef();
		LShoulderJDef.initialize(LUArmBody,TorsoBody, LShoulderPos);
		LShoulderJDef.enableLimit = true;
		LShoulderJDef.upperAngle = 0f;
		LShoulderJDef.lowerAngle = -2f;
		LShoulderJDef.enableMotor = true;
		LShoulderJDef.maxMotorTorque = 1000f;
		LShoulderJDef.motorSpeed = 0f; // Speed 1,2: -2,2
		LShoulderJDef.collideConnected = false;
		LShoulderJ = (RevoluteJoint)getWorld().createJoint(LShoulderJDef);

		//Right elbow
		RElbowJDef = new RevoluteJointDef();
		RElbowJDef.initialize(RLArmBody,RUArmBody, RElbowPos);
		RElbowJDef.enableLimit = true;
		RElbowJDef.upperAngle = 0.5f;
		RElbowJDef.lowerAngle = -0.1f;
		RElbowJDef.enableMotor = true;
		RElbowJDef.maxMotorTorque = 0f;
		RElbowJDef.motorSpeed = 10f; //TODO: investigate further 
		RElbowJDef.collideConnected = false;
		RElbowJ = (RevoluteJoint)getWorld().createJoint(RElbowJDef);

		//Left elbow
		LElbowJDef = new RevoluteJointDef();
		LElbowJDef.initialize(LLArmBody,LUArmBody, LElbowPos);
		LElbowJDef.enableLimit = true;
		LElbowJDef.upperAngle = 0.5f;
		LElbowJDef.lowerAngle = -0.1f;
		LElbowJDef.enableMotor = true;
		LElbowJDef.maxMotorTorque = 0f;
		LElbowJDef.motorSpeed = 10f; //TODO: investigate further  
		LElbowJDef.collideConnected = false;
		LElbowJ = (RevoluteJoint)getWorld().createJoint(LElbowJDef);

		//My current understanding is that the shapes never change. Only the transforms. Hence, this is now static and we only capture the states once.
		if(shapeList[0] == null){
			shapeList[0] = (PolygonShape)TorsoBody.getShapeList();
			shapeList[1] = (CircleShape)HeadBody.getShapeList();
			shapeList[2] = (PolygonShape)RFootBody.getShapeList();
			shapeList[3] = (PolygonShape)LFootBody.getShapeList();
			shapeList[4] = (PolygonShape)RCalfBody.getShapeList();
			shapeList[5] = (PolygonShape)LCalfBody.getShapeList();
			shapeList[6] = (PolygonShape)RThighBody.getShapeList();
			shapeList[7] = (PolygonShape)LThighBody.getShapeList();
			shapeList[8] = (PolygonShape)RUArmBody.getShapeList();
			shapeList[9] = (PolygonShape)LUArmBody.getShapeList();
			shapeList[10] = (PolygonShape)RLArmBody.getShapeList();
			shapeList[11] = (PolygonShape)LLArmBody.getShapeList();
			shapeList[12] = (PolygonShape)TrackBody.getShapeList();
		}
	} 

	public void everyStep(boolean q, boolean w, boolean o, boolean p){
		/* Involuntary Couplings (no QWOP presses) */

		//Springs:
		float NeckStiff = 15f;
		float NeckDamp = 5f;

		float RElbowStiff = 1f;
		float LElbowStiff = 1f;

		float RElbowDamp = 0f;
		float LElbowDamp = 0f;

		//Neck spring torque
		float NeckTorque = -NeckStiff*NeckJ.getJointAngle() + 0*NeckDamp*NeckJ.getJointSpeed();
		NeckTorque = NeckTorque + 0*400f*(NeckJ.getJointAngle() + 0.2f); //This bizarre term is probably a roundabout way of adjust equilibrium position.

		//Elbow spring torque
		float RElbowTorque = -RElbowStiff*RElbowJ.getJointAngle() + 0*RElbowDamp*RElbowJ.getJointSpeed();
		float LElbowTorque = -LElbowStiff*LElbowJ.getJointAngle() + 0*LElbowDamp*LElbowJ.getJointSpeed();

		//For now, using motors with high speed settings and torque limits to simulate springs. I don't know a better way for now.

		NeckJ.m_motorSpeed = (1000*Math.signum(NeckTorque)); //If torque is negative, make motor speed negative.
		RElbowJ.m_motorSpeed = (1000*Math.signum(RElbowTorque));
		LElbowJ.m_motorSpeed = (1000*Math.signum(LElbowTorque));	

		NeckJ.m_maxMotorTorque = (Math.abs(NeckTorque));
		RElbowJ.m_maxMotorTorque = (Math.abs(RElbowTorque));
		LElbowJ.m_maxMotorTorque = (Math.abs(LElbowTorque));


		/* QWOP Keypress actions */

		//Ankle speeds setpoints:
		float RAnkleSpeed1 = 2f;
		float RAnkleSpeed2 = -2f;

		float LAnkleSpeed1 = -2f;
		float LAnkleSpeed2 = 2f;

		float RKneeSpeed1 = -2.5f;
		float RKneeSpeed2 = 2.5f;

		float LKneeSpeed1 = 2.5f;
		float LKneeSpeed2 = -2.5f;

		float RHipSpeed1 = -2.5f;
		float RHipSpeed2 = 2.5f;

		float LHipSpeed1 = 2.5f;
		float LHipSpeed2 = -2.5f;

		float RShoulderSpeed1 = 2f;
		float RShoulderSpeed2 = -2f;

		float LShoulderSpeed1 = -2f;
		float LShoulderSpeed2 = 2f;

		//O Hip limits (changed to this when o is pressed):
		float ORHipLimLo = -1.3f;
		float ORHipLimHi = 0.7f;

		float OLHipLimLo = -1f;
		float OLHipLimHi = 1f;


		//P Hip limits:
		float PRHipLimLo = -0.8f;
		float PRHipLimHi = 1.2f;

		float PLHipLimLo = -1.5f;
		float PLHipLimHi = 0.5f;


		/* QW Press Stuff */
		//See spreadsheet for complete rules and priority explanations.
		if (q){
			//Set speed 1 for hips:
			LHipJ.m_motorSpeed = (LHipSpeed2);
			RHipJ.m_motorSpeed = (RHipSpeed2);

			//Set speed 1 for shoulders:
			LShoulderJ.m_motorSpeed = (LShoulderSpeed2);
			RShoulderJ.m_motorSpeed = (RShoulderSpeed2);

		}else if(w){
			//Set speed 2 for hips:
			LHipJ.m_motorSpeed = (LHipSpeed1);
			RHipJ.m_motorSpeed = (RHipSpeed1);

			//set speed 2 for shoulders:
			LShoulderJ.m_motorSpeed = (LShoulderSpeed1);
			RShoulderJ.m_motorSpeed = (RShoulderSpeed1);

		}else{
			//Set hip and ankle speeds to 0:
			LHipJ.m_motorSpeed = (0f);
			RHipJ.m_motorSpeed = (0f);

			LShoulderJ.m_motorSpeed = (0f);
			RShoulderJ.m_motorSpeed = (0f);
		}

		//Ankle/Hip Coupling -+ 0*Requires either Q or W pressed.
		if (q || w){
			//Get world ankle positions (using foot and torso anchors -+ 0*TODO: see if this is correct)
			//		  Vec2 RAnkleCur = new Vec2(0,0);
			//		  Vec2 LAnkleCur = new Vec2(0,0);
			//		  Vec2 RHipCur = new Vec2(0,0);		  

			Vec2 RAnkleCur = RAnkleJ.getAnchor1();
			Vec2 LAnkleCur = LAnkleJ.getAnchor1();

			Vec2 RHipCur = RHipJ.getAnchor1();


			// if right ankle joint is behind the right hip jiont
			// Set ankle motor speed to 1;
			// else speed 2
			if (RAnkleCur.x<RHipCur.x){
				RAnkleJ.m_motorSpeed = (RAnkleSpeed2);
			}else{
				RAnkleJ.m_motorSpeed = (RAnkleSpeed1);
			}


			// if left ankle joint is behind RIGHT hip joint (weird it's the right one here too)
			// Set its motor speed to 1;
			// else speed 2;  
			if (LAnkleCur.x<RHipCur.x){
				LAnkleJ.m_motorSpeed = (LAnkleSpeed2);
			}else{
				LAnkleJ.m_motorSpeed = (LAnkleSpeed1);
			}

		}

		/* OP Keypress Stuff */
		if (o){
			//Set speed 1 for knees
			// set l hip limits(-1 1)
			//set right hip limits (-1.3,0.7)
			RKneeJ.m_motorSpeed = (RKneeSpeed2);
			LKneeJ.m_motorSpeed = (LKneeSpeed2);

			RHipJ.m_lowerAngle = (ORHipLimLo);
			RHipJ.m_upperAngle = (ORHipLimHi);

			LHipJ.m_lowerAngle = (OLHipLimLo);
			LHipJ.m_upperAngle = (OLHipLimHi);

		}else if(p){
			//Set speed 2 for knees
			// set L hip limits(-1.5,0.5)
			// set R hip limits(-0.8,1.2)

			RKneeJ.m_motorSpeed = (RKneeSpeed1);
			LKneeJ.m_motorSpeed = (LKneeSpeed1);

			RHipJ.m_lowerAngle = (PRHipLimLo);
			RHipJ.m_upperAngle = (PRHipLimHi);
			LHipJ.m_lowerAngle = PLHipLimLo;
			LHipJ.m_upperAngle = PLHipLimHi;

		}else{

			// Set knee speeds to 0
			//Joint limits not changed!!
			RKneeJ.m_motorSpeed = (0f);
			LKneeJ.m_motorSpeed = (0f);  
		}

	}
	public World getWorld() {
		return m_world;
	}
	
// I'm removing support for importing games from the old versions of the software.
//
//	/**
//	/* Sets all the state parameters but does not assign them. Could be useful to call before creating a game. */
//	public void setParametersToState(QWOP_stateHolder sh) {
//		// Feet
//		 RFootPos = sh.rfoot.getPvec();
//		 LFootPos = sh.lfoot.getPvec();
//		 RFootAng = (float)sh.rfoot.theta;
//		 LFootAng = (float)sh.lfoot.theta;
//		// Calves
//		 RCalfPos = sh.rcalf.getPvec();
//		 LCalfPos = sh.lcalf.getPvec();
//		 RCalfAng = (float)sh.rcalf.theta;
//		 LCalfAng = (float)sh.rcalf.theta;
//		// Thighs
//		 RThighPos = sh.rthigh.getPvec();
//		 LThighPos = sh.lthigh.getPvec();
//		 RThighAng = (float)sh.rthigh.theta;
//		 LThighAng = (float)sh.lthigh.theta;
//		// Torso
//		 TorsoPos = sh.body.getPvec();
//		 TorsoAng = (float)sh.body.theta;
//		// Head
//		 HeadPos = sh.head.getPvec();
//		 HeadAng = (float)sh.head.theta;
//		// Upper arms
//		 RUArmPos = sh.ruarm.getPvec();
//		 LUArmPos = sh.luarm.getPvec();
//		 RUArmAng = (float)sh.ruarm.theta;
//		 LUArmAng = (float)sh.luarm.theta;
//		// Lower Arms
//		 RLArmPos = sh.rlarm.getPvec();
//		 LLArmPos = sh.llarm.getPvec();
//		 RLArmAng = (float)sh.rlarm.theta;
//		 LLArmAng = (float)sh.llarm.theta;
//	}
//	public void setState(QWOP_stateHolder sh){
//
//		// Torso
//		TorsoBody.setXForm(sh.body.getPvec(), (float)sh.body.theta + 0*TorsoAngAdj);
//		TorsoBody.setLinearVelocity(sh.body.getVvec());
//		TorsoBody.setAngularVelocity((float)sh.body.dtheta);
//
//		// rthigh
//		RThighBody.setXForm(sh.rthigh.getPvec(), (float)sh.rthigh.theta + 0*RThighAngAdj);
//		RThighBody.setLinearVelocity(sh.rthigh.getVvec());
//		RThighBody.setAngularVelocity((float)sh.rthigh.dtheta);
//
//
//		// lthigh
//		LThighBody.setXForm(sh.lthigh.getPvec(), (float)sh.lthigh.theta + 0*LThighAngAdj);
//		LThighBody.setLinearVelocity(sh.lthigh.getVvec());
//		LThighBody.setAngularVelocity((float)sh.lthigh.dtheta);
//
//
//		// rcalf
//		RCalfBody.setXForm(sh.rcalf.getPvec(), (float)sh.rcalf.theta + 0*RCalfAngAdj);
//		RCalfBody.setLinearVelocity(sh.rcalf.getVvec());
//		RCalfBody.setAngularVelocity((float)sh.rcalf.dtheta);
//
//		// lcalf
//		LCalfBody.setXForm(sh.lcalf.getPvec(), (float)sh.lcalf.theta + 0*LCalfAngAdj);
//		LCalfBody.setLinearVelocity(sh.lcalf.getVvec());
//		LCalfBody.setAngularVelocity((float)sh.lcalf.dtheta);
//
//		// rfoot
//		RFootBody.setXForm(sh.rfoot.getPvec(), (float)sh.rfoot.theta);
//		RFootBody.setLinearVelocity(sh.rfoot.getVvec());
//		RFootBody.setAngularVelocity((float)sh.rfoot.dtheta);
//
//		// lfoot
//		LFootBody.setXForm(sh.lfoot.getPvec(), (float)sh.lfoot.theta);
//		LFootBody.setLinearVelocity(sh.lfoot.getVvec());
//		LFootBody.setAngularVelocity((float)sh.lfoot.dtheta);
//
//		// ruarm
//		RUArmBody.setXForm(sh.ruarm.getPvec(), (float)sh.ruarm.theta + 0*RUArmAngAdj);
//		RUArmBody.setLinearVelocity(sh.ruarm.getVvec());
//		RUArmBody.setAngularVelocity((float)sh.ruarm.dtheta);
//
//		// luarm
//		LUArmBody.setXForm(sh.luarm.getPvec(), (float)sh.luarm.theta + 0*LUArmAngAdj);
//		LUArmBody.setLinearVelocity(sh.luarm.getVvec());
//		LUArmBody.setAngularVelocity((float)sh.luarm.dtheta);
//
//		// rlarm
//		RLArmBody.setXForm(sh.rlarm.getPvec(), (float)sh.rlarm.theta + 0*RLArmAngAdj);
//		RLArmBody.setLinearVelocity(sh.rlarm.getVvec());
//		RLArmBody.setAngularVelocity((float)sh.rlarm.dtheta);
//
//		// llarm
//		LLArmBody.setXForm(sh.llarm.getPvec(), (float)sh.llarm.theta + 0*LLArmAngAdj);
//		LLArmBody.setLinearVelocity(sh.llarm.getVvec());
//		LLArmBody.setAngularVelocity((float)sh.llarm.dtheta);
//
//		// head
//		HeadBody.setXForm(sh.head.getPvec(), (float)sh.head.theta + 0*HeadAngAdj);
//		HeadBody.setLinearVelocity(sh.head.getVvec());
//		HeadBody.setAngularVelocity((float)sh.head.dtheta);
//	}
}

