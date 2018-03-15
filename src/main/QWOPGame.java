package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.shapes.CircleDef;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
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


public class QWOPGame{

	/** Keep track of sim stats since beginning of execution. **/
	private static long timestepsSimulated = 0;

	/** Physics engine stepping parameters. **/
	public static final float timestep = 0.04f;
	private final int iterations = 5;

	/** Angle failure limits. Fail if torso angle is too big or small to rule out stupid hopping that eventually falls. **/
	public static float torsoAngUpper = 1.2f;
	public static float torsoAngLower = -1.2f; // Negative is falling backwards. 0.4 is start angle.

	/** Normal stroke for line drawing. **/
	private static final Stroke normalStroke = new BasicStroke(0.5f);

	/** Box2D world to be populated for QWOP. **/
	public World m_world;

	/** Has this game reached failure conditions? **/
	private boolean isFailed = false;

	
	/** Filters collisions. Prevents body parts from hitting other body parts. **/
	private final int BODY_GROUP = -1;

	// Track
	private static final float trackPosY = 8.90813f;
	private static final float trackFric = 1f;
	private static final float trackRest = 0.2f;

	// Feet
	private static final Vec2 rFootPos = new Vec2(-0.96750f,7.77200f);
	private static final Vec2 lFootPos = new Vec2(3.763f,8.101f);
	private static final float rFootAng = 0.7498f, rFootMass = 11.630f, rFootInertia = 9.017f, rFootL = 2.68750f, rFootH = 1.44249f, rFootFric = 1.5f, rFootDensity = 3f;
	private static final float lFootAng = 0.1429f, lFootMass = 10.895f, lFootInertia = 8.242f, lFootL = 2.695f, lFootH = 1.34750f, lFootFric = 1.5f, lFootDensity = 3f;

	// Calves
	private static final Vec2 rCalfPos = new Vec2(0.0850f,5.381f);
	private static final Vec2 lCalfPos = new Vec2(2.986f,5.523f);
	private static final float rCalfAng = -0.821f;
	private static final float lCalfAng = -1.582f;
	private static final float rCalfAngAdj = 1.606188724f;
	private static final float lCalfAngAdj = 1.607108307f;
	private static final float rCalfMass = 7.407f;
	private static final float lCalfMass = 7.464f;
	private static final float rCalfInertia = 16.644f;
	private static final float lCalfInertia = 16.893f;

	//Length and width for the calves are just for collisions with the ground, so not very important.
	private static final float rCalfL = 4.21f;
	private static final float lCalfL = 4.43f;
	private static final float rCalfW = 0.4f;
	private static final float lCalfW = 0.4f;
	private static final float rCalfFric = 0.2f;
	private static final float lCalfFric = 0.2f;
	private static final float rCalfDensity = 1f;
	private static final float lCalfDensity = 1f;

	// Thighs
	private static final Vec2 rThighPos = new Vec2(1.659f,1.999f);
	private static final Vec2 lThighPos = new Vec2(2.52f,1.615f);
	private static final float rThighAng = 1.468f;
	private static final float lThighAng = -1.977f;
	private static final float rThighAngAdj = -1.544382589f;
	private static final float lThighAngAdj = 1.619256373f;
	private static final float rThighMass = 10.54f;
	private static final float lThighMass = 10.037f;
	private static final float rThighInertia = 28.067f;
	private static final float lThighInertia = 24.546f;

	//Length and width for the calves are just for collisions with the ground, so not very important.
	private static final float rThighL = 4.19f;
	private static final float lThighL = 3.56f;
	private static final float rThighW = 0.6f;
	private static final float lThighW = 0.6f;
	private static final float rThighFric = 0.2f;
	private static final float lThighFric = 0.2f;
	private static final float rThighDensity = 1f;
	private static final float lThighDensity = 1f;

	// Torso
	private static final Vec2 torsoPos = new Vec2(2.525f,-1.926f);
	private static final float torsoAng = -1.251f;
	private static final float torsoAngAdj = 1.651902129f;
	private static final float torsoMass = 18.668f;
	private static final float torsoInertia = 79.376f;

	//Length and width for the calves are just for collisions with the ground, so not very important.
	private static final float torsoL = 5f;
	private static final float torsoW = 1.5f;
	private static final float torsoFric = 0.2f;
	private static final float torsoDensity = 1f;

	// Head
	private static final Vec2 headPos = new Vec2(3.896f,-5.679f);
	private static final float headAng = 0.058f;
	private static final float headMass = 5.674f;
	private static final float headAngAdj = 0.201921414f;
	private static final float headInertia = 5.483f;

	//Radius is just for collision shape
	private static final float headR = 1.1f;
	private static final float headFric = 0.2f;
	private static final float headDensity = 1f;

	// Upper arms
	private static final Vec2 rUArmPos = new Vec2(1.165f,-3.616f);
	private static final Vec2 lUArmPos = new Vec2(4.475f,-2.911f);
	private static final float rUArmAng = -0.466f;
	private static final float lUArmAng = 0.843f;
	private static final float rUArmAngAdj = 1.571196588f;
	private static final float lUArmAngAdj = -1.690706418f;

	private static final float rUArmMass = 5.837f;
	private static final float lUArmMass = 4.6065f;
	private static final float rUArmInertia = 8.479f;
	private static final float lUArmInertia = 5.85f;

	//for collision shapes
	private static final float rUArmL = 2.58f;
	private static final float lUArmL = 2.68f;
	private static final float rUArmW = 0.2f;
	private static final float lUArmW = 0.15f;
	private static final float rUArmFric = 0.2f;
	private static final float lUArmFric = 0.2f;
	private static final float rUArmDensity = 1f;
	private static final float lUArmDensity = 1f;

	// Lower Arms
	private static final Vec2 rLArmPos = new Vec2(0.3662f,-1.248f);
	private static final Vec2 lLArmPos = new Vec2(5.899f,-3.06f);
	private static final float rLArmAng = -1.762f;
	private static final float lLArmAng = -1.251f;
	private static final float rLArmAngAdj = 1.521319096f;
	private static final float lLArmAngAdj = 1.447045854f;
	private static final float rLArmMass = 5.99f;
	private static final float lLArmMass = 3.8445f;
	private static final float rLArmInertia = 10.768f;
	private static final float lLArmInertia = 4.301f;

	// For collision shapes
	private static final float rLArmL = 3.56f, lLArmL = 2.54f, rLArmW = 0.15f, lLArmW = 0.12f,
			rLArmFric = 0.2f, lLArmFric = 0.2f, rLArmDensity = 1f, lLArmDensity = 1f;


	// Ankle speeds setpoints:
	private static final float 	rAnkleSpeed1 = 2f, 		rAnkleSpeed2 = -2f, 		lAnkleSpeed1 = -2f, 		lAnkleSpeed2 = 2f,
								rKneeSpeed1 = -2.5f, 	rKneeSpeed2 = 2.5f, 		lKneeSpeed1 = 2.5f, 		lKneeSpeed2 = -2.5f,
								rHipSpeed1 = -2.5f, 		rHipSpeed2 = 2.5f, 		lHipSpeed1 = 2.5f, 		lHipSpeed2 = -2.5f,
								rShoulderSpeed1 = 2f, 	rShoulderSpeed2 = -2f, 	lShoulderSpeed1 = -2f, 	lShoulderSpeed2 = 2f;

	//O Hip limits (changed to this when o is pressed):
	private static final float oRHipLimLo = -1.3f, oRHipLimHi = 0.7f, oLHipLimLo = -1f, oLHipLimHi = 1f;
	//P Hip limits:
	private static final float pRHipLimLo = -0.8f, pRHipLimHi = 1.2f, pLHipLimLo = -1.5f, pLHipLimHi = 0.5f;

	//Springs and things:
	private static final float neckStiff = 15f, neckDamp = 5f, rElbowStiff = 1f, lElbowStiff = 1f, rElbowDamp = 0f, lElbowDamp = 0f;

	/* Joints Positions*/
	private static final float rAnklePosX = -0.96750f, 	rAnklePosY = 7.77200f, 		lAnklePosX = 3.763f, 		lAnklePosY = 8.101f, 
							  rKneePosX = 1.58f, 		rKneePosY = 4.11375f, 		lKneePosX = 3.26250f, 		lKneePosY = 3.51625f,
							  rHipPosX = 1.260f, 		rHipPosY = -0.06750f, 		lHipPosX = 2.01625f, 		lHipPosY = 0.18125f,
							  rShoulderPosX = 2.24375f, 	rShoulderPosY = -4.14250f,	lShoulderPosX = 3.63875f, 	lShoulderPosY = -3.58875f,
							  rElbowPosX = -0.06f,		rElbowPosY = -2.985f,		lElbowPosX = 5.65125f, 		lElbowPosY = -1.8125f,
							  neckPosX = 3.60400f, 		neckPosY = -4.581f;

	/** List of shapes for use by graphics stuff. Making it static -- IE, assuming that in multiple games, the runner doesn't change shape. **/
	public static Shape[] shapeList = new Shape[13];

	/** Initial runner state. **/
	private static final State initState = new QWOPGame().getCurrentGameState(); // Make sure this stays below all the other static assignments to avoid null pointers.





	/**
	 * This loads all the Box2D classes needed on a unique ClassLoader. This means that World.class from one instance
	 * of this loader is different from the World.class of another loader. This means that they will have their
	 * own static variables and should not interfere with each other.
	 * @author matt
	 *
	 */
	public class GameLoader extends ClassLoader {
		
		private Class<?> _World, _MassData, _BodyDef, _Vec2, _PolygonDef, _CircleDef, _AABB, _RevoluteJointDef, _Body, _RevoluteJoint;
		
		// Body/shape definitions:
		private Object trackDef, rFootDef, lFootDef, rCalfDef, lCalfDef, rThighDef, lThighDef, torsoDef, headDef, rUArmDef, lUArmDef, rLArmDef, lLArmDef;
		private Object trackShape, rFootShape, lFootShape, rCalfShape, lCalfShape, rThighShape, lThighShape, torsoShape, headShape, rUArmShape, lUArmShape, rLArmShape, lLArmShape;

		// Actual bodies:
		private Object trackBody, rFootBody, lFootBody, rCalfBody, lCalfBody, rThighBody, lThighBody, torsoBody, headBody, rUArmBody, lUArmBody, rLArmBody, lLArmBody;
		
		// Joint definitions
		public Object rHipJDef, lHipJDef, rKneeJDef, lKneeJDef, rAnkleJDef, lAnkleJDef, rShoulderJDef, lShoulderJDef, rElbowJDef, lElbowJDef, neckJDef;

		// Joint objects
		public Object rHipJ, lHipJ, rKneeJ, lKneeJ, rAnkleJ, lAnkleJ, rShoulderJ, lShoulderJ, rElbowJ, lElbowJ, neckJ;
	
		
		public GameLoader() {
			loadClasses(); // Load all the necessary classes on a new class loader (this).
			
			try {
				oneTimeSetup(); // Create all the shape and body definitions that never need changing.
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
					| IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public Class<?> findClass(String name) {
			byte[] bt = loadClassData(name);
			return defineClass(name, bt, 0, bt.length);
		}
		
		/** Loads individual classes.**/
		private byte[] loadClassData(String className) {
			//read class
			InputStream is = getClass().getClassLoader().getResourceAsStream(className.replace(".", "/")+".class");
			ByteArrayOutputStream byteSt = new ByteArrayOutputStream();
			//write into byte
			int len = 0;
			try {
				while((len=is.read())!=-1){
					byteSt.write(len);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//convert into byte array
			return byteSt.toByteArray();
		}
		
		/** Loads all the classes we need. **/
		private void loadClasses() {
			
			try {
				_Vec2 = findClass("org.jbox2d.common.Vec2");
				_AABB = findClass("org.jbox2d.collision.AABB");
				_World = findClass("org.jbox2d.dynamics.World");
				
				Class<?> world1 = findClass("org.jbox2d.dynamics.World$1");
				Class<?> cm = findClass("org.jbox2d.dynamics.ContactManager");
				Class<?> bp = findClass("org.jbox2d.collision.BroadPhase");
				Class<?> pm = findClass("org.jbox2d.collision.PairManager");
				Class<?> p = findClass("org.jbox2d.collision.Proxy");			
				
				_Body = findClass("org.jbox2d.dynamics.Body");
				Class<?> xf = findClass("org.jbox2d.common.XForm");
				_BodyDef = findClass("org.jbox2d.dynamics.BodyDef");
				_MassData = findClass("org.jbox2d.collision.MassData");
				_PolygonDef = findClass("org.jbox2d.collision.shapes.PolygonDef");
				_CircleDef = findClass("org.jbox2d.collision.shapes.CircleDef");
				Class<?> sw = findClass("org.jbox2d.common.Sweep");
				Class<?> m22 = findClass("org.jbox2d.common.Mat22");
				
				_RevoluteJointDef = findClass("org.jbox2d.joint.RevoluteJointDef");
				_RevoluteJoint = findClass("org.jbox2d.joint.RevoluteJoint");

			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws InstantiationException 
		 * @throws SecurityException 
		 * @throws NoSuchFieldException 
		 * @throws IllegalAccessException 
		 * @throws IllegalArgumentException 
		 * @throws InvocationTargetException 
		 * @throws NoSuchMethodException **/
		private Object makeBodyDef(float positionX, float positionY, float angle, float mass, float inertia) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException, NoSuchMethodException, InvocationTargetException {

			Object bodyDef = _BodyDef.newInstance();
			Object massData = makeMassData(mass, inertia);
			Object position = makeVec2(positionX, positionY);
			
			bodyDef.getClass().getField("massData").set(null, massData);
			bodyDef.getClass().getField("position").set(null, position);
			bodyDef.getClass().getField("angle").setFloat(null, angle);
			
			return bodyDef;
		}
		
		/** Make a joint definition. 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws NoSuchFieldException **/
		private Object makeJointDef(Object body1, Object body2, float jointPosX, float jointPosY, float lowerAngle, 
				float upperAngle, float maxTorque, float motorSpeed, boolean enableLimit, boolean enableMotor, boolean collideConnected) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			
			Object jDef = _RevoluteJointDef.newInstance();
			Object posVec = makeVec2(jointPosX, jointPosY);
			
			jDef.getClass().getMethod("initialize", _Body, _Body, _Vec2);
			jDef.getClass().getField("lowerAngle").setFloat(null, lowerAngle);
			jDef.getClass().getField("upperAngle").setFloat(null, upperAngle);
			jDef.getClass().getField("maxMotorTorque").setFloat(null, maxTorque);
			jDef.getClass().getField("motorSpeed").setFloat(null, motorSpeed);
			
			jDef.getClass().getField("motorSpeed").setBoolean(null, enableLimit);
			jDef.getClass().getField("motorSpeed").setBoolean(null, enableMotor);
			jDef.getClass().getField("motorSpeed").setBoolean(null, collideConnected);
			
			return jDef;
		}
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws NoSuchFieldException **/
		private Object makeBoxShapeDef(float boxX, float boxY, float restitution, float friction, float density, int groupIdx) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			Object boxShapeDef = _PolygonDef.newInstance();
			
			boxShapeDef.getClass().getMethod("setAsBox", float.class, float.class).invoke(null, boxX, boxY);
			boxShapeDef.getClass().getField("friction").setFloat(null, friction);
			boxShapeDef.getClass().getField("density").setFloat(null, density);
			boxShapeDef.getClass().getField("restitution").setFloat(null, restitution);
			boxShapeDef.getClass().getField("groupIndex").setInt(null, groupIdx);
			
			return boxShapeDef;
		}
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws NoSuchFieldException **/
		private Object makeBoxShapeDef(float boxX, float boxY, float friction, float density, int groupIdx) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			Object boxShapeDef = _PolygonDef.newInstance();
			
			boxShapeDef.getClass().getMethod("setAsBox", float.class, float.class).invoke(null, boxX, boxY);
			boxShapeDef.getClass().getField("friction").setFloat(null, friction);
			boxShapeDef.getClass().getField("density").setFloat(null, density);
			boxShapeDef.getClass().getField("groupIndex").setInt(null, groupIdx);
			
			return boxShapeDef;
		}
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws NoSuchFieldException **/
		private Object makeCircleShapeDef(float radius, float restitution, float friction, float density, int groupIdx) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			Object circleShapeDef = _CircleDef.newInstance();
			
			circleShapeDef.getClass().getField("radius").setFloat(null, radius);
			circleShapeDef.getClass().getField("friction").setFloat(null, friction);
			circleShapeDef.getClass().getField("density").setFloat(null, density);
			circleShapeDef.getClass().getField("restitution").setFloat(null, restitution);
			circleShapeDef.getClass().getField("groupIndex").setInt(null, groupIdx);
			
			return circleShapeDef;
		}
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws NoSuchFieldException **/
		private Object makeCircleShapeDef(float radius, float friction, float density, int groupIdx) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			Object circleShapeDef = _CircleDef.newInstance();
			
			circleShapeDef.getClass().getField("radius").setFloat(null, radius);
			circleShapeDef.getClass().getField("friction").setFloat(null, friction);
			circleShapeDef.getClass().getField("density").setFloat(null, density);
			circleShapeDef.getClass().getField("groupIndex").setInt(null, groupIdx);
			
			return circleShapeDef;
		}
		
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws SecurityException 
		 * @throws NoSuchFieldException 
		 * @throws IllegalAccessException 
		 * @throws IllegalArgumentException 
		 * @throws InstantiationException **/
		private Object makeMassData(float mass, float inertia) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
			Object md = _MassData.newInstance();
			md.getClass().getField("mass").setFloat(null, mass);
			md.getClass().getField("I").setFloat(null, inertia);
			
			return md;
		}
		
		
		/** Convenience method to avoid dealing with reflection in the code constantly. 
		 * @throws SecurityException 
		 * @throws NoSuchFieldException 
		 * @throws IllegalAccessException 
		 * @throws IllegalArgumentException 
		 * @throws InstantiationException **/
		private Object makeVec2(float x, float y) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			Constructor<?> cons = _Vec2.getConstructor(float.class, float.class);
			Object vec = cons.newInstance(x, y);
			
			return vec;
		}
		
		
		/******* SET UP THINGS *********/
		
		/** Call once to initialize a lot of shape definitions which only need to be created once. 
		 * @throws NoSuchFieldException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException **/
		private void oneTimeSetup() throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			/* 
			 * Make the bodies and collision shapes
			 */

			/* TRACK */
			trackDef = makeBodyDef(-30, trackPosY + 20, 0, 0, 0);
			trackShape = makeBoxShapeDef(1000f, 20f, trackRest, trackFric, 0, 1);
			
			/* FEET */
			//Create the fixture shapes, IE collision shapes
			rFootDef = makeBodyDef(rFootPos.x, rFootPos.y, rFootAng, rFootMass, rFootInertia);
			rFootShape = makeBoxShapeDef(rFootL/2f, rFootH/2f, rFootFric, rFootDensity, BODY_GROUP);
			lFootDef = makeBodyDef(lFootPos.x, lFootPos.y, lFootAng, lFootMass, lFootInertia);
			lFootShape = makeBoxShapeDef(lFootL/2f, lFootH/2f, lFootFric, lFootDensity, BODY_GROUP);

			/* CALVES */
			rCalfDef = makeBodyDef(rCalfPos.x, rCalfPos.y, rCalfAng + rCalfAngAdj, rCalfMass, rCalfInertia);
			rCalfShape = makeBoxShapeDef(rCalfW/2f, rCalfL/2f, rCalfFric, rCalfDensity, BODY_GROUP);
			lCalfDef = makeBodyDef(lCalfPos.x, lCalfPos.y, lCalfAng + lCalfAngAdj, lCalfMass, lCalfInertia);
			lCalfShape = makeBoxShapeDef(lCalfW/2f, lCalfL/2f, lCalfFric, lCalfDensity, BODY_GROUP);

			/* THIGHS */
			rThighDef = makeBodyDef(rThighPos.x, rThighPos.y, rThighAng + rThighAngAdj, rThighMass, rThighInertia);
			rThighShape = makeBoxShapeDef(rThighW/2f, rThighL/2f, rThighFric, rThighDensity, BODY_GROUP);
			lThighDef = makeBodyDef(lThighPos.x, lThighPos.y, lThighAng + lThighAngAdj, lThighMass, lThighInertia);
			lThighShape = makeBoxShapeDef(lThighW/2f, lThighL/2f, lThighFric, lThighDensity, BODY_GROUP);
			

			/* TORSO */
			torsoDef = makeBodyDef(torsoPos.x, torsoPos.y, torsoAng + torsoAngAdj, torsoMass, torsoInertia);
			torsoShape = makeBoxShapeDef(torsoW/2f, torsoL/2f, torsoFric, torsoDensity, BODY_GROUP);


			/* HEAD */    
			headDef = makeBodyDef(headPos.x, headPos.y, headAng + headAngAdj, headMass, headInertia);
			headShape = makeCircleShapeDef(headR, headFric, headDensity, BODY_GROUP);


			/* UPPER ARMS */
			rUArmDef = makeBodyDef(rUArmPos.x, rUArmPos.y, rUArmAng + rUArmAngAdj, rUArmMass, rUArmInertia);
			rUArmShape = makeBoxShapeDef(rUArmW/2f, rUArmL/2f, rUArmFric, rUArmDensity, BODY_GROUP);
			lUArmDef = makeBodyDef(lUArmPos.x, lUArmPos.y, lUArmAng + lUArmAngAdj, lUArmMass, lUArmInertia);
			lUArmShape = makeBoxShapeDef(lUArmW/2f, lUArmL/2f, lUArmFric, lUArmDensity, BODY_GROUP);


			/* LOWER ARMS */  
			rLArmDef = makeBodyDef(rLArmPos.x, rLArmPos.y, rLArmAng + rLArmAngAdj, rLArmMass, rLArmInertia);
			rLArmShape = makeBoxShapeDef(rLArmW/2f, rLArmL/2f, rLArmFric, rLArmDensity, BODY_GROUP);
			lLArmDef = makeBodyDef(lLArmPos.x, lLArmPos.y, lLArmAng + lLArmAngAdj, lLArmMass, lLArmInertia);
			lLArmShape = makeBoxShapeDef(lLArmW/2f, lLArmL/2f, lLArmFric, lLArmDensity, BODY_GROUP);
		}
		
		/** Make (or remake) the world with all the body parts at their initial locations. 
		 * @throws SecurityException 
		 * @throws NoSuchMethodException 
		 * @throws InvocationTargetException 
		 * @throws IllegalArgumentException 
		 * @throws IllegalAccessException 
		 * @throws InstantiationException 
		 * @throws NoSuchFieldException **/
		private void makeNewWorld() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
			
			/******* WORLD ********/
			// Make the world object:
			Constructor<?> aabbCons = _AABB.getConstructor(_Vec2, _Vec2);
			Object vecAABBLower = makeVec2(-100f,-30f);
			Object vecAABBUpper = makeVec2(5000f,80f);
			Object aabbWorld = aabbCons.newInstance(vecAABBLower, vecAABBUpper);

			Object vecGrav = makeVec2(0f,10f);

			Constructor<?> worldCons = _World.getConstructor(_AABB, _Vec2, boolean.class);
			
			Object world = worldCons.newInstance(aabbWorld, vecGrav, true);
			
			// World settings:
			world.getClass().getMethod("setWarmStarting", boolean.class).invoke(null, true);
			world.getClass().getMethod("setPositionCorrection", boolean.class).invoke(null, true);
			world.getClass().getMethod("setContinuousPhysics", boolean.class).invoke(null, true);
			
			
			/******* BODIES ********/
			// Add bodies:
			trackBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, trackDef);
			trackBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, trackShape);
			
			rFootBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, rFootDef);
			rFootBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, rFootShape);
			lFootBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, lFootDef);
			lFootBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, lFootShape);
			
			rCalfBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, rCalfDef);
			rCalfBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, rCalfShape);
			lCalfBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, lCalfDef);
			lCalfBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, lCalfShape);
			
			rThighBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, rThighDef);
			rThighBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, rThighShape);
			lThighBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, lThighDef);
			lThighBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, lThighShape);
			
			rUArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, rUArmDef);
			rUArmBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, rUArmShape);
			lUArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, lUArmDef);
			lUArmBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, lUArmShape);
			
			rLArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, rLArmDef);
			rLArmBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, rLArmShape);
			lLArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, lLArmDef);
			lLArmBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, lLArmShape);
			
			torsoBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, torsoDef);
			torsoBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, torsoShape);
			
			headBody = world.getClass().getMethod("createBody", _BodyDef).invoke(null, headDef);
			headBody.getClass().getMethod("createShape", _PolygonDef).invoke(null, headShape);
			
			
			/******* JOINTS ********/
//			makeJointDef(Object body1, Object body2, float jointPosX, float jointPosY, float lowerAngle, 
//					float upperAngle, float maxTorque, float motorSpeed, boolean enableLimit, boolean enableMotor, boolean collideConnected)
				
			
			Object rAnkleJDef = makeJointDef(rFootBody, rCalfBody, rAnklePosX, rAnklePosY, -0.5f, 0.5f, 2000f, 0f, true, false, false);
			rAnkleJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, rAnkleJDef).getClass().asSubclass(_RevoluteJoint);
			Object lAnkleJDef = makeJointDef(lFootBody, lCalfBody, lAnklePosX, lAnklePosY, -0.5f, 0.5f, 2000f, 0f, true, false, false);
			lAnkleJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, lAnkleJDef).getClass().asSubclass(_RevoluteJoint);
			
			Object rKneeJDef = makeJointDef(rCalfBody, rThighBody, rKneePosX, rKneePosY, -1.3f, 0.3f, 3000f, 0f, true, true, false);
			rKneeJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, rKneeJDef).getClass().asSubclass(_RevoluteJoint);
			Object lKneeJDef = makeJointDef(lCalfBody, lThighBody, lKneePosX, lKneePosY, -1.6f, 0.0f, 3000f, 0f, true, true, false);
			lKneeJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, lKneeJDef).getClass().asSubclass(_RevoluteJoint);
			
			Object rHipJDef = makeJointDef(lCalfBody, lThighBody, rHipPosX, rHipPosY, -1.3f, 0.7f, 6000f, 0f, true, true, false);
			rHipJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, rHipJDef).getClass().asSubclass(_RevoluteJoint);
			Object lHipJDef = makeJointDef(lCalfBody, lThighBody, lHipPosX, lHipPosY, -1.5f, 0.5f, 6000f, 0f, true, true, false);
			lHipJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, lHipJDef).getClass().asSubclass(_RevoluteJoint);

			Object neckJDef = makeJointDef(lCalfBody, lThighBody, neckPosX, neckPosY, -0.5f, 0.0f, 1000f, 0f, true, true, false);
			neckJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, neckJDef).getClass().asSubclass(_RevoluteJoint);


			Object rShoulderJDef = makeJointDef(lCalfBody, lThighBody, rShoulderPosX, rShoulderPosY, -0.5f, 1.5f, 1000f, 0f, true, true, false);
			rShoulderJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, rShoulderJDef).getClass().asSubclass(_RevoluteJoint);
			Object lShoulderJDef = makeJointDef(lCalfBody, lThighBody, lShoulderPosX, lShoulderPosY, -2f, 0.0f, 1000f, 0f, true, true, false);
			lShoulderJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, lShoulderJDef).getClass().asSubclass(_RevoluteJoint);

			Object rElbowJDef = makeJointDef(lCalfBody, lThighBody, rElbowPosX, rElbowPosY, -0.1f, 0.5f, 0f, 10f, true, true, false);
			rElbowJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, rElbowJDef).getClass().asSubclass(_RevoluteJoint);
			Object lElbowJDef = makeJointDef(lCalfBody, lThighBody, lElbowPosX, lElbowPosY, -0.1f, 0.5f, 0f, 10f, true, true, false);
			lElbowJ = world.getClass().getMethod("createJoint", _RevoluteJointDef).invoke(null, lElbowJDef).getClass().asSubclass(_RevoluteJoint);

		}
	}
	


	public static void main(String[] args) {

		Object obj1 = loadClasses();
		Object obj2 = loadClasses();
		System.out.println(obj1);
		System.out.println(obj2.getClass());
		System.out.println(obj1.getClass() == obj2.getClass());



	}

	public QWOPGame(){
		setup();
		getWorld().setContactListener(new CollisionListener());
	}



	private void setup() {

		isFailed = false;


		/* Hip Joints */





		//My current understanding is that the shapes never change. Only the transforms. Hence, this is now static and we only capture the states once.
		if(shapeList[0] == null){
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
	} 

	/** Step the game forward 1 timestep with the specified keys pressed. **/
	public void stepGame(boolean q, boolean w, boolean o, boolean p){
		/* Involuntary Couplings (no QWOP presses) */

		//Neck spring torque
		float NeckTorque = -neckStiff*neckJ.getJointAngle() + 0*neckDamp*neckJ.getJointSpeed();
		NeckTorque = NeckTorque + 0*400f*(neckJ.getJointAngle() + 0.2f); //This bizarre term is probably a roundabout way of adjust equilibrium position.

		//Elbow spring torque
		float RElbowTorque = -rElbowStiff*rElbowJ.getJointAngle() + 0*rElbowDamp*rElbowJ.getJointSpeed();
		float LElbowTorque = -lElbowStiff*lElbowJ.getJointAngle() + 0*lElbowDamp*lElbowJ.getJointSpeed();

		//For now, using motors with high speed settings and torque limits to simulate springs. I don't know a better way for now.

		neckJ.m_motorSpeed = (1000*Math.signum(NeckTorque)); //If torque is negative, make motor speed negative.
		rElbowJ.m_motorSpeed = (1000*Math.signum(RElbowTorque));
		lElbowJ.m_motorSpeed = (1000*Math.signum(LElbowTorque));	

		neckJ.m_maxMotorTorque = (Math.abs(NeckTorque));
		rElbowJ.m_maxMotorTorque = (Math.abs(RElbowTorque));
		lElbowJ.m_maxMotorTorque = (Math.abs(LElbowTorque));

		/* QW Press Stuff */
		//See spreadsheet for complete rules and priority explanations.
		if (q){
			//Set speed 1 for hips:
			lHipJ.m_motorSpeed = (lHipSpeed2);
			rHipJ.m_motorSpeed = (rHipSpeed2);

			//Set speed 1 for shoulders:
			lShoulderJ.m_motorSpeed = (lShoulderSpeed2);
			rShoulderJ.m_motorSpeed = (rShoulderSpeed2);

		}else if(w){
			//Set speed 2 for hips:
			lHipJ.m_motorSpeed = (lHipSpeed1);
			rHipJ.m_motorSpeed = (rHipSpeed1);

			//set speed 2 for shoulders:
			lShoulderJ.m_motorSpeed = (lShoulderSpeed1);
			rShoulderJ.m_motorSpeed = (rShoulderSpeed1);

		}else{
			//Set hip and ankle speeds to 0:
			lHipJ.m_motorSpeed = (0f);
			rHipJ.m_motorSpeed = (0f);

			lShoulderJ.m_motorSpeed = (0f);
			rShoulderJ.m_motorSpeed = (0f);
		}

		//Ankle/Hip Coupling -+ 0*Requires either Q or W pressed.
		if (q || w){
			//Get world ankle positions (using foot and torso anchors -+ 0*TODO: see if this is correct) 
			Vec2 RAnkleCur = rAnkleJ.getAnchor1();
			Vec2 LAnkleCur = lAnkleJ.getAnchor1();

			Vec2 RHipCur = rHipJ.getAnchor1();


			// if right ankle joint is behind the right hip jiont
			// Set ankle motor speed to 1;
			// else speed 2
			if (RAnkleCur.x<RHipCur.x){
				rAnkleJ.m_motorSpeed = (rAnkleSpeed2);
			}else{
				rAnkleJ.m_motorSpeed = (rAnkleSpeed1);
			}


			// if left ankle joint is behind RIGHT hip joint (weird it's the right one here too)
			// Set its motor speed to 1;
			// else speed 2;  
			if (LAnkleCur.x<RHipCur.x){
				lAnkleJ.m_motorSpeed = (lAnkleSpeed2);
			}else{
				lAnkleJ.m_motorSpeed = (lAnkleSpeed1);
			}

		}

		/* OP Keypress Stuff */
		if (o){
			//Set speed 1 for knees
			// set l hip limits(-1 1)
			//set right hip limits (-1.3,0.7)
			rKneeJ.m_motorSpeed = (rKneeSpeed2);
			lKneeJ.m_motorSpeed = (lKneeSpeed2);

			rHipJ.m_lowerAngle = (oRHipLimLo);
			rHipJ.m_upperAngle = (oRHipLimHi);

			lHipJ.m_lowerAngle = (oLHipLimLo);
			lHipJ.m_upperAngle = (oLHipLimHi);

		}else if(p){
			//Set speed 2 for knees
			// set L hip limits(-1.5,0.5)
			// set R hip limits(-0.8,1.2)

			rKneeJ.m_motorSpeed = (rKneeSpeed1);
			lKneeJ.m_motorSpeed = (lKneeSpeed1);

			rHipJ.m_lowerAngle = (pRHipLimLo);
			rHipJ.m_upperAngle = (pRHipLimHi);
			lHipJ.m_lowerAngle = pLHipLimLo;
			lHipJ.m_upperAngle = pLHipLimHi;

		}else{

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

	/** Get the actual Box2D world. **/
	public World getWorld() {
		return m_world;
	}

	/** QWOP initial condition. Good way to give the root node a state. **/
	public static State getInitialState(){
		return initState;
	}

	/** Get the runner's current state. **/
	public State getCurrentGameState() {
		return new State(this);
	}

	/** Is this state in failure? **/
	public boolean getFailureStatus() {
		return isFailed;
	}

	/** Get the number of timesteps simulated since the beginning of execution. **/
	public long getTimestepsSimulated() {
		return timestepsSimulated;
	}


	/** How far out to mark road dashes. **/
	private final int markingWidth = 2000;

	/** Draw this game's runner. Must provide scaling from game units to pixels, as well as pixel offsets in x and y. **/
	public void draw(Graphics g, float scaling, int xOffset, int yOffset) {

		Body newBody = getWorld().getBodyList();
		while (newBody != null) {
			int xOffsetPixels = -(int)(scaling*torsoBody.getPosition().x) + xOffset; // Basic offset, plus centering x on torso.
			Shape newfixture = newBody.getShapeList();

			while(newfixture != null) {

				// Most links are polygon shapes
				if(newfixture.getType() == ShapeType.POLYGON_SHAPE) {

					PolygonShape newShape = (PolygonShape)newfixture;
					Vec2[] shapeVerts = newShape.m_vertices;
					for (int k = 0; k<newShape.m_vertexCount; k++) {

						XForm xf = newBody.getXForm();
						Vec2 ptA = XForm.mul(xf,shapeVerts[k]);
						Vec2 ptB = XForm.mul(xf, shapeVerts[(k+1) % (newShape.m_vertexCount)]);
						g.drawLine((int)(scaling * ptA.x) + xOffsetPixels,
								(int)(scaling * ptA.y) + yOffset,
								(int)(scaling * ptB.x) + xOffsetPixels,
								(int)(scaling * ptB.y) + yOffset);			    		
					}
				}else if (newfixture.getType() == ShapeType.CIRCLE_SHAPE) { // Basically just head
					CircleShape newShape = (CircleShape)newfixture;
					float radius = newShape.m_radius;
					g.drawOval((int)(scaling * (newBody.getPosition().x - radius) + xOffsetPixels),
							(int)(scaling * (newBody.getPosition().y - radius) + yOffset),
							(int)(scaling * radius * 2),
							(int)(scaling * radius * 2));		

				}else if(newfixture.getType() == ShapeType.EDGE_SHAPE) { // The track.

					EdgeShape newShape = (EdgeShape)newfixture;
					XForm trans = newBody.getXForm();

					Vec2 ptA = XForm.mul(trans, newShape.getVertex1());
					Vec2 ptB = XForm.mul(trans, newShape.getVertex2());
					Vec2 ptC = XForm.mul(trans, newShape.getVertex2());

					g.drawLine((int)(scaling * ptA.x) + xOffsetPixels,
							(int)(scaling * ptA.y) + yOffset,
							(int)(scaling * ptB.x) + xOffsetPixels,
							(int)(scaling * ptB.y) + yOffset);			    		
					g.drawLine((int)(scaling * ptA.x) + xOffsetPixels,
							(int)(scaling * ptA.y) + yOffset,
							(int)(scaling * ptC.x) + xOffsetPixels,
							(int)(scaling * ptC.y) + yOffset);			    		

				}else{
					System.out.println("Not found: " + newfixture.m_type.name());
				}
				newfixture = newfixture.getNext();
			}
			newBody = newBody.getNext();
		}

		//This draws the "road" markings to show that the ground is moving relative to the dude.
		for (int i = 0; i < markingWidth/69; i++) {
			g.drawString("_", ((-(int)(scaling * torsoBody.getPosition().x) - i * 70) % markingWidth) + markingWidth, yOffset + 92);
		}
	}

	/** Draw the runner at a specified set of transforms.. **/
	public static void drawExtraRunner(Graphics2D g, XForm[] transforms, String label, float scaling, int xOffset, int yOffset, Color drawColor, Stroke stroke) {
		g.setColor(drawColor);
		g.drawString(label, xOffset + (int)(transforms[1].position.x * scaling) - 20, yOffset - 75);
		for (int i = 0; i < shapeList.length; i++) {
			g.setColor(drawColor);
			g.setStroke(stroke);
			switch(shapeList[i].getType()) {
			case CIRCLE_SHAPE:
				CircleShape circleShape = (CircleShape)shapeList[i];
				float radius = circleShape.getRadius();
				Vec2 circleCenter = XForm.mul(transforms[i], circleShape.getLocalPosition());
				g.drawOval((int)(scaling * (circleCenter.x - radius) + xOffset),
						(int)(scaling * (circleCenter.y - radius) + yOffset),
						(int)(scaling * radius * 2),
						(int)(scaling * radius * 2));
				break;
			case POLYGON_SHAPE:
				//Get both the shape and its transform.
				PolygonShape polygonShape = (PolygonShape)shapeList[i];
				XForm transform = transforms[i];

				// Ground is black regardless.
				if (shapeList[i].m_filter.groupIndex == 1) {
					g.setColor(Color.BLACK);
					g.setStroke(normalStroke);
				}
				for (int j = 0; j < polygonShape.getVertexCount(); j++) { // Loop through polygon vertices and draw lines between them.
					Vec2 ptA = XForm.mul(transform, polygonShape.m_vertices[j]);
					Vec2 ptB = XForm.mul(transform, polygonShape.m_vertices[(j + 1) % (polygonShape.getVertexCount())]); //Makes sure that the last vertex is connected to the first one.
					g.drawLine((int)(scaling * ptA.x) + xOffset,
							(int)(scaling * ptA.y) + yOffset,
							(int)(scaling * ptB.x) + xOffset,
							(int)(scaling * ptB.y) + yOffset);		
				}
				break;
			default:
				break;
			}
		}
	}


	/** Listens for collisions involving lower arms and head (implicitly with the ground) **/
	private class CollisionListener implements ContactListener{

		/** Keep track of whether the right foot is on the ground. **/
		private boolean rFootDown = false;

		/** Keep track of whether the left foot is on the ground. **/
		private boolean lFootDown = false;

		public CollisionListener(){}

		@Override
		public void add(ContactPoint point) {
			Shape fixtureA = point.shape1;
			Shape fixtureB = point.shape2;

			//Failure when head, arms, or thighs hit the ground.
			if(fixtureA.m_body.equals(headBody) ||
					fixtureB.m_body.equals(headBody) ||
					fixtureA.m_body.equals(lLArmBody) ||
					fixtureB.m_body.equals(lLArmBody) ||
					fixtureA.m_body.equals(rLArmBody) ||
					fixtureB.m_body.equals(rLArmBody)) {
				isFailed = true;
			}else if(fixtureA.m_body.equals(lThighBody)||
					fixtureB.m_body.equals(lThighBody)||
					fixtureA.m_body.equals(rThighBody)||
					fixtureB.m_body.equals(rThighBody)){

				isFailed = true;
			}else if(fixtureA.m_body.equals(rFootBody) || fixtureB.m_body.equals(rFootBody)){//Track when each foot hits the ground.
				rFootDown = true;		
			}else if(fixtureA.m_body.equals(lFootBody) || fixtureB.m_body.equals(lFootBody)){
				lFootDown = true;
			}	
		}
		@Override
		public void persist(ContactPoint point){}
		@Override
		public void remove(ContactPoint point) {
			//Track when each foot leaves the ground.
			Shape fixtureA = point.shape1;
			Shape fixtureB = point.shape2;
			if(fixtureA.m_body.equals(rFootBody) || fixtureB.m_body.equals(rFootBody)){
				rFootDown = false;
			}else if(fixtureA.m_body.equals(lFootBody) || fixtureB.m_body.equals(lFootBody)){
				lFootDown = false;
			}	
		}
		@Override
		public void result(ContactResult point) {}

		/** Check if the right foot is touching the ground. **/
		@SuppressWarnings("unused")
		public boolean isRightFootGrounded(){
			return rFootDown;
		}

		/** Check if the left foot is touching the ground. **/
		@SuppressWarnings("unused")
		public boolean isLeftFootGrounded(){
			return lFootDown;
		}
	}
}

