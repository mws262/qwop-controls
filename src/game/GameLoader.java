package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

//	/** Initial runner state. **/
//	private static final State initState = new QWOPGame().getCurrentGameState(); // Make sure this stays below all the other static assignments to avoid null pointers.

/**
 * This loads all the Box2D classes needed on a unique ClassLoader. This means that World.class from one instance
 * of this loader is different from the World.class of another loader. This means that they will have their
 * own static variables and should not interfere with each other.
 * @author matt
 *
 */
public class GameLoader extends ClassLoader {
	/** Keep track of sim stats since beginning of execution. **/
	private static long timestepsSimulated = 0;

	/** Physics engine stepping parameters. **/
	public static final float timestep = 0.04f;
	private final int iterations = 5;

	/** Angle failure limits. Fail if torso angle is too big or small to rule out stupid hopping that eventually falls. **/
	public static float torsoAngUpper = 1.2f, torsoAngLower = -1.2f; // Negative is falling backwards. 0.4 is start angle.

	/** Normal stroke for line drawing. **/
	private static final Stroke normalStroke = new BasicStroke(0.5f);

	/** Has this game reached failure conditions? **/
	private boolean isFailed = false;

	/** Filters collisions. Prevents body parts from hitting other body parts. **/
	private final int BODY_GROUP = -1;

	/** List of shapes for use by graphics stuff. Making it static -- IE, assuming that in multiple games, the runner doesn't change shape. **/
	private List<Object> shapeList;

	// Track
	private static final float 	trackPosY = 8.90813f, 	trackFric = 1f, 		trackRest = 0.2f;
	// Feet
	private static final float 	rFootPosX = -0.96750f, 	rFootPosY = 7.77200f,
			lFootPosX = 3.763f, 	lFootPosY = 8.101f;
	private static final float 	rFootAng = 0.7498f, 	rFootMass = 11.630f, 	rFootInertia = 9.017f, 			rFootL = 2.68750f, 	rFootH = 1.44249f, 	rFootFric = 1.5f, 	rFootDensity = 3f,
			lFootAng = 0.1429f, 	lFootMass = 10.895f, 	lFootInertia = 8.242f, 			lFootL = 2.695f, 	lFootH = 1.34750f, 	lFootFric = 1.5f, 	lFootDensity = 3f;
	// Calves
	private static final float 	rCalfPosX = 0.0850f, 	rCalfPosY = 5.381f,
			lCalfPosX = 2.986f, 	lCalfPosY = 5.523f;
	private static final float 	rCalfAng = -0.821f, 	lCalfAng = -1.582f, 	rCalfAngAdj = 1.606188724f, 	lCalfAngAdj = 1.607108307f,
			rCalfMass = 7.407f, 	lCalfMass = 7.464f, 	rCalfInertia = 16.644f, 		lCalfInertia = 16.893f;
	// Length and width for the calves are just for collisions with the ground, so not very important.
	private static final float 	rCalfL = 4.21f, 		lCalfL = 4.43f, 		rCalfW = 0.4f, 					lCalfW = 0.4f,
			rCalfFric = 0.2f, 		lCalfFric = 0.2f, 		rCalfDensity = 1f, 				lCalfDensity = 1f;
	// Thighs
	private static final float 	rThighPosX = 1.659f, 	rThighPosY = 1.999f, 	lThighPosX = 2.52f, 			lThighPosY = 1.615f,
			rThighAng = 1.468f, 	lThighAng = -1.977f, 	rThighAngAdj = -1.544382589f, 	lThighAngAdj = 1.619256373f,
			rThighMass = 10.54f, 	lThighMass = 10.037f, 	rThighInertia = 28.067f, 		lThighInertia = 24.546f;
	// Length and width for the calves are just for collisions with the ground, so not very important.
	private static final float 	rThighL = 4.19f, 		lThighL = 3.56f, 		rThighW = 0.6f, 				lThighW = 0.6f,
			rThighFric = 0.2f, 		lThighFric = 0.2f, 		rThighDensity = 1f, 			lThighDensity = 1f;
	// Torso
	private static final float 	torsoPosX = 2.525f, 	torsoPosY = -1.926f, 
			torsoAng = -1.251f, 	torsoAngAdj = 1.651902129f, 
			torsoMass = 18.668f, 	torsoInertia = 79.376f;
	// Length and width for the calves are just for collisions with the ground, so not very important.
	private static final float 	torsoL = 5f, 			torsoW = 1.5f, 			torsoFric = 0.2f, 				torsoDensity = 1f;
	// Head
	private static final float 	headPosX = 3.896f, 		headPosY = -5.679f,
			headAng = 0.058f, 		headAngAdj = 0.201921414f,
			headMass = 5.674f, 		headInertia = 5.483f;
	// Radius is just for collision shape
	private static final float 	headR = 1.1f, 			headFric = 0.2f, 		headDensity = 1f;
	// Upper arms
	private static final float 	rUArmPosX = 1.165f, 	rUArmPosY = -3.616f, 	lUArmPosX = 4.475f, 			lUArmPosY = -2.911f,
			rUArmAng = -0.466f, 	lUArmAng = 0.843f, 		rUArmAngAdj = 1.571196588f, 	lUArmAngAdj = -1.690706418f,
			rUArmMass = 5.837f,		lUArmMass = 4.6065f, 	rUArmInertia = 8.479f, 			lUArmInertia = 5.85f;
	// Dimensions for collision shapes
	private static final float 	rUArmL = 2.58f, 		lUArmL = 2.68f, 		rUArmW = 0.2f, 					lUArmW = 0.15f, 
			rUArmFric = 0.2f, 		lUArmFric = 0.2f, 		rUArmDensity = 1f, 				lUArmDensity = 1f;
	// Lower Arms
	private static final float 	rLArmPosX = 0.3662f, 	rLArmPosY = -1.248f, 	lLArmPosX = 5.899f, 			lLArmPosY = -3.06f,
			rLArmAng = -1.762f, 	lLArmAng = -1.251f, 	rLArmAngAdj = 1.521319096f, 	lLArmAngAdj = 1.447045854f,
			rLArmMass = 5.99f, 		lLArmMass = 3.8445f, 	rLArmInertia = 10.768f, 		lLArmInertia = 4.301f;
	// For collision shapes
	private static final float 	rLArmL = 3.56f, 		lLArmL = 2.54f, 		rLArmW = 0.15f, 				lLArmW = 0.12f,
			rLArmFric = 0.2f, 		lLArmFric = 0.2f, 		rLArmDensity = 1f, 				lLArmDensity = 1f;
	// Ankle speeds setpoints:
	private static final float 	rAnkleSpeed1 = 2f, 		rAnkleSpeed2 = -2f, 	lAnkleSpeed1 = -2f, 			lAnkleSpeed2 = 2f,
			rKneeSpeed1 = -2.5f, 	rKneeSpeed2 = 2.5f, 	lKneeSpeed1 = 2.5f, 			lKneeSpeed2 = -2.5f,
			rHipSpeed1 = -2.5f, 	rHipSpeed2 = 2.5f, 		lHipSpeed1 = 2.5f, 				lHipSpeed2 = -2.5f,
			rShoulderSpeed1 = 2f, 	rShoulderSpeed2 = -2f, 	lShoulderSpeed1 = -2f, 			lShoulderSpeed2 = 2f;
	// Hip limits
	private static final float 	oRHipLimLo = -1.3f, 	oRHipLimHi = 0.7f, 		oLHipLimLo = -1f, 				oLHipLimHi = 1f, //O Hip limits (changed to this when o is pressed):
			pRHipLimLo = -0.8f, 	pRHipLimHi = 1.2f, 		pLHipLimLo = -1.5f, 			pLHipLimHi = 0.5f; //P Hip limits:
	// Springs and things:
	private static final float 	neckStiff = 15f, 		neckDamp = 5f,
			rElbowStiff = 1f, 		lElbowStiff = 1f,
			rElbowDamp = 0f, 		lElbowDamp = 0f;
	/* Joints Positions*/
	private static final float 	rAnklePosX = -0.96750f, rAnklePosY = 7.77200f, 	lAnklePosX = 3.763f, 			lAnklePosY = 8.101f, 
			rKneePosX = 1.58f, 		rKneePosY = 4.11375f, 	lKneePosX = 3.26250f, 			lKneePosY = 3.51625f,
			rHipPosX = 1.260f, 		rHipPosY = -0.06750f, 	lHipPosX = 2.01625f, 			lHipPosY = 0.18125f,
			rShoulderPosX = 2.24375f, rShoulderPosY = -4.14250f, lShoulderPosX = 3.63875f, 	lShoulderPosY = -3.58875f,
			rElbowPosX = -0.06f,	rElbowPosY = -2.985f,	lElbowPosX = 5.65125f, 			lElbowPosY = -1.8125f,
			neckPosX = 3.60400f, 	neckPosY = -4.581f;

	private Class<?> 			_World, _MassData, _BodyDef, _Vec2, _PolygonDef, _CircleDef, _AABB, _RevoluteJointDef,
	_Body, _RevoluteJoint, _ContactPoint, _ContactListener, _ShapeType, _PolygonShape, _CircleShape, 
	_EdgeShape, _XForm, _ShapeDef, _Shape, _JointDef;
	// World definition:
	private Object 				world;
	// Body/shape definitions:
	private Object 				trackDef, rFootDef, lFootDef, rCalfDef, lCalfDef, rThighDef, lThighDef, torsoDef, headDef, rUArmDef,
	lUArmDef, rLArmDef, lLArmDef;
	private Object 				trackShape, rFootShape, lFootShape, rCalfShape, lCalfShape, rThighShape, lThighShape, torsoShape,
	headShape, rUArmShape, lUArmShape, rLArmShape, lLArmShape;
	// Actual bodies:
	private Object 				trackBody, rFootBody, lFootBody, rCalfBody, lCalfBody, rThighBody, lThighBody, torsoBody, headBody,
	rUArmBody, lUArmBody, rLArmBody, lLArmBody;
	// Joint definitions
	public Object 				rHipJDef, lHipJDef, rKneeJDef, lKneeJDef, rAnkleJDef, lAnkleJDef, rShoulderJDef, lShoulderJDef,
	rElbowJDef, lElbowJDef, neckJDef;
	// Joint objects
	public Object 				rHipJ, lHipJ, rKneeJ, lKneeJ, rAnkleJ, lAnkleJ, rShoulderJ, lShoulderJ, rElbowJ, lElbowJ, neckJ;
	// Contact listener
	private Object 				contactListenerProxy;

	private boolean rFootDown = false;
	private boolean lFootDown = false;

	/** How far out to mark road dashes. **/
	private final int markingWidth = 2000;

	public static void main(String[] args) {
		GameLoader gl = new GameLoader();
		try {
			gl.makeNewWorld();
			for (int i = 0; i < 50; i++) {
				gl.stepGame(true, false, false, true);
				System.out.println(gl.lKneeJ.getClass().getMethod("getJointAngle").invoke(gl.lKneeJ));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}		
	}

	public GameLoader() {
		loadClasses(); // Load all the necessary classes on a new class loader (this).
		try {
			oneTimeSetup(); // Create all the shape and body definitions that never need changing.
			makeNewWorld();
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
			// I think the order of these DOES matter since if a subcless needs to load its parent, the default classloader, rather than this one will be used.
			// I.e. put the most basic stuff further up the list.

			_Vec2 = findClass("org.jbox2d.common.Vec2");
			findClass("org.jbox2d.common.Sweep");
			findClass("org.jbox2d.common.Mat22");
			findClass("org.jbox2d.common.MathUtils");
			findClass("org.jbox2d.common.Settings");

			_ShapeType = findClass("org.jbox2d.collision.shapes.ShapeType");
			findClass("org.jbox2d.dynamics.contacts.Contact");
			_ShapeDef = findClass("org.jbox2d.collision.shapes.ShapeDef");
			_AABB = findClass("org.jbox2d.collision.AABB");
			_World = findClass("org.jbox2d.dynamics.World");
			_ContactPoint = findClass("org.jbox2d.dynamics.contacts.ContactPoint");
			_ContactListener = findClass("org.jbox2d.dynamics.ContactListener");
			_Shape = findClass("org.jbox2d.collision.shapes.Shape");

			findClass("org.jbox2d.dynamics.World$1");
			findClass("org.jbox2d.dynamics.Island");
			findClass("org.jbox2d.dynamics.joints.JointEdge");
			findClass("org.jbox2d.dynamics.contacts.CircleContact");
			findClass("org.jbox2d.dynamics.contacts.ContactEdge");
			findClass("org.jbox2d.dynamics.ContactFilter");
			findClass("org.jbox2d.dynamics.DefaultContactFilter");
			findClass("org.jbox2d.dynamics.ContactManager");
			findClass("org.jbox2d.dynamics.contacts.ContactSolver");
			findClass("org.jbox2d.dynamics.contacts.NullContact");
			findClass("org.jbox2d.dynamics.contacts.PolyAndCircleContact");
			findClass("org.jbox2d.dynamics.contacts.PolyContact");
			findClass("org.jbox2d.dynamics.contacts.PolyAndEdgeContact");
			findClass("org.jbox2d.dynamics.contacts.EdgeAndCircleContact");
			findClass("org.jbox2d.dynamics.contacts.PointAndCircleContact");
			findClass("org.jbox2d.dynamics.contacts.PointAndPolyContact");
			findClass("org.jbox2d.dynamics.contacts.ContactConstraintPoint");
			findClass("org.jbox2d.dynamics.contacts.ContactConstraint");
			findClass("org.jbox2d.dynamics.contacts.ContactResult");

			findClass("org.jbox2d.collision.BroadPhase");
			findClass("org.jbox2d.collision.PairManager");
			findClass("org.jbox2d.collision.BufferedPair");
			findClass("org.jbox2d.collision.Proxy");
			findClass("org.jbox2d.collision.ManifoldPoint");		
			findClass("org.jbox2d.collision.Manifold");
			findClass("org.jbox2d.collision.ContactID");
			findClass("org.jbox2d.collision.OBB");
			findClass("org.jbox2d.collision.TOI");
			findClass("org.jbox2d.collision.Point");
			findClass("org.jbox2d.collision.Distance");
			findClass("org.jbox2d.collision.shapes.PointShape");
			findClass("org.jbox2d.collision.shapes.CollideCircle");
			findClass("org.jbox2d.collision.shapes.CollidePoly");
			findClass("org.jbox2d.collision.shapes.MaxSeparation");
			findClass("org.jbox2d.collision.shapes.CollidePoly$ClipVertex");

			_Body = findClass("org.jbox2d.dynamics.Body");
			_XForm = findClass("org.jbox2d.common.XForm");
			_BodyDef = findClass("org.jbox2d.dynamics.BodyDef");
			_MassData = findClass("org.jbox2d.collision.MassData");
			_PolygonDef = findClass("org.jbox2d.collision.shapes.PolygonDef");
			_CircleDef = findClass("org.jbox2d.collision.shapes.CircleDef");
			_PolygonShape = findClass("org.jbox2d.collision.shapes.PolygonShape");
			_CircleShape = findClass("org.jbox2d.collision.shapes.CircleShape");
			_EdgeShape = findClass("org.jbox2d.collision.shapes.EdgeShape");
			findClass("org.jbox2d.collision.shapes.EdgeChainDef");

			findClass("org.jbox2d.pooling.TLVec2");
			findClass("org.jbox2d.pooling.TLMat22");
			findClass("org.jbox2d.pooling.TLManifold");
			findClass("org.jbox2d.pooling.TLContactPoint");
			findClass("org.jbox2d.pooling.TLXForm");
			findClass("org.jbox2d.pooling.TLAABB");
			findClass("org.jbox2d.pooling.SingletonPool");
			findClass("org.jbox2d.pooling.TLBoundValues");
			findClass("org.jbox2d.pooling.arrays.IntegerArray");
			findClass("org.jbox2d.pooling.SingletonPool$Singletons");
			findClass("org.jbox2d.pooling.SingletonPool$Pool");
			findClass("org.jbox2d.pooling.stacks.IslandStack");
			findClass("org.jbox2d.pooling.stacks.ContactSolverStack");

			findClass("org.jbox2d.dynamics.joints.JointType");
			_JointDef = findClass("org.jbox2d.dynamics.joints.JointDef");
			findClass("org.jbox2d.dynamics.joints.Joint");
			_RevoluteJointDef = findClass("org.jbox2d.dynamics.joints.RevoluteJointDef");
			_RevoluteJoint = findClass("org.jbox2d.dynamics.joints.RevoluteJoint");
			findClass("org.jbox2d.dynamics.joints.DistanceJointDef");
			findClass("org.jbox2d.dynamics.joints.DistanceJoint");
			findClass("org.jbox2d.dynamics.joints.MouseJointDef");
			findClass("org.jbox2d.dynamics.joints.MouseJoint");
			findClass("org.jbox2d.dynamics.joints.PrismaticJointDef");
			findClass("org.jbox2d.dynamics.joints.PrismaticJoint");
			findClass("org.jbox2d.dynamics.joints.PulleyJointDef");
			findClass("org.jbox2d.dynamics.joints.PulleyJoint");
			findClass("org.jbox2d.dynamics.joints.GearJointDef");
			findClass("org.jbox2d.dynamics.joints.GearJoint");
			findClass("org.jbox2d.dynamics.joints.ConstantVolumeJointDef");
			findClass("org.jbox2d.dynamics.joints.ConstantVolumeJoint");
		} catch (Exception e) {
			e.printStackTrace();
		}	

		contactListenerProxy = Proxy.newProxyInstance(_ContactListener.getClassLoader(), new Class[] {_ContactListener}, 
				new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String method_name = method.getName();
				//Class<?>[] classes = method.getParameterTypes();
				Object fixtureAShape;
				Object fixtureBShape;
				Object fixtureABody;
				Object fixtureBBody;

				switch (method_name) {
				case "add":	
					fixtureAShape = args[0].getClass().getField("shape1").get(args[0]);
					fixtureABody = fixtureAShape.getClass().getField("m_body").get(fixtureAShape);
					fixtureBShape = args[0].getClass().getField("shape2").get(args[0]);
					fixtureBBody = fixtureBShape.getClass().getField("m_body").get(fixtureBShape);
					//Failure when head, arms, or thighs hit the ground.
					if(fixtureABody.equals(headBody) ||
							fixtureBBody.equals(headBody) ||
							fixtureABody.equals(lLArmBody) ||
							fixtureBBody.equals(lLArmBody) ||
							fixtureABody.equals(rLArmBody) ||
							fixtureBBody.equals(rLArmBody)) {
						isFailed = true;
					}else if(fixtureABody.equals(lThighBody)||
							fixtureBBody.equals(lThighBody)||
							fixtureABody.equals(rThighBody)||
							fixtureBBody.equals(rThighBody)){

						isFailed = true;
					}else if(fixtureABody.equals(rFootBody) || fixtureBBody.equals(rFootBody)){//Track when each foot hits the ground.
						rFootDown = true;		
					}else if(fixtureABody.equals(lFootBody) || fixtureBBody.equals(lFootBody)){
						lFootDown = true;
					}	
					break;
				case "persist":
					break;
				case "remove":
					//Track when each foot leaves the ground.
					fixtureAShape = args[0].getClass().getField("shape1").get(args[0]);
					fixtureABody = fixtureAShape.getClass().getField("m_body").get(fixtureAShape);
					fixtureBShape = args[0].getClass().getField("shape2").get(args[0]);
					fixtureBBody = fixtureBShape.getClass().getField("m_body").get(fixtureBShape);
					if(fixtureABody.equals(rFootBody) || fixtureBBody.equals(rFootBody)){
						rFootDown = false;
					}else if(fixtureABody.equals(lFootBody) || fixtureBBody.equals(lFootBody)){
						lFootDown = false;
					}	
					break;
				case "result":
					break;
				}
				return null;
			}
		});
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

		bodyDef.getClass().getField("massData").set(bodyDef, massData);
		bodyDef.getClass().getField("position").set(bodyDef, position);
		bodyDef.getClass().getField("angle").setFloat(bodyDef, angle);

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

		jDef.getClass().getMethod("initialize", _Body, _Body, _Vec2).invoke(jDef, body1, body2, posVec);
		jDef.getClass().getField("lowerAngle").setFloat(jDef, lowerAngle);
		jDef.getClass().getField("upperAngle").setFloat(jDef, upperAngle);
		jDef.getClass().getField("maxMotorTorque").setFloat(jDef, maxTorque);
		jDef.getClass().getField("motorSpeed").setFloat(jDef, motorSpeed);

		jDef.getClass().getField("enableLimit").setBoolean(jDef, enableLimit);
		jDef.getClass().getField("enableMotor").setBoolean(jDef, enableMotor);
		jDef.getClass().getField("collideConnected").setBoolean(jDef, collideConnected);

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

		boxShapeDef.getClass().getMethod("setAsBox", float.class, float.class).invoke(boxShapeDef, boxX, boxY);
		boxShapeDef.getClass().getField("friction").setFloat(boxShapeDef, friction);
		boxShapeDef.getClass().getField("density").setFloat(boxShapeDef, density);
		boxShapeDef.getClass().getField("restitution").setFloat(boxShapeDef, restitution);
		Object shapeFilter = boxShapeDef.getClass().getField("filter").get(boxShapeDef);
		shapeFilter.getClass().getField("groupIndex").setInt(shapeFilter, groupIdx);

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

		boxShapeDef.getClass().getMethod("setAsBox", float.class, float.class).invoke(boxShapeDef, boxX, boxY);
		boxShapeDef.getClass().getField("friction").setFloat(boxShapeDef, friction);
		boxShapeDef.getClass().getField("density").setFloat(boxShapeDef, density);
		Object shapeFilter = boxShapeDef.getClass().getField("filter").get(boxShapeDef);
		shapeFilter.getClass().getField("groupIndex").setInt(shapeFilter, groupIdx);

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

		circleShapeDef.getClass().getField("radius").setFloat(circleShapeDef, radius);
		circleShapeDef.getClass().getField("friction").setFloat(circleShapeDef, friction);
		circleShapeDef.getClass().getField("density").setFloat(circleShapeDef, density);
		Object shapeFilter = circleShapeDef.getClass().getField("filter").get(circleShapeDef);
		shapeFilter.getClass().getField("groupIndex").setInt(shapeFilter, groupIdx);

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
		md.getClass().getField("mass").setFloat(md, mass);
		md.getClass().getField("I").setFloat(md, inertia);
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
		rFootDef = makeBodyDef(rFootPosX, rFootPosY, rFootAng, rFootMass, rFootInertia);
		rFootShape = makeBoxShapeDef(rFootL/2f, rFootH/2f, rFootFric, rFootDensity, BODY_GROUP);
		lFootDef = makeBodyDef(lFootPosX, lFootPosY, lFootAng, lFootMass, lFootInertia);
		lFootShape = makeBoxShapeDef(lFootL/2f, lFootH/2f, lFootFric, lFootDensity, BODY_GROUP);

		/* CALVES */
		rCalfDef = makeBodyDef(rCalfPosX, rCalfPosY, rCalfAng + rCalfAngAdj, rCalfMass, rCalfInertia);
		rCalfShape = makeBoxShapeDef(rCalfW/2f, rCalfL/2f, rCalfFric, rCalfDensity, BODY_GROUP);
		lCalfDef = makeBodyDef(lCalfPosX, lCalfPosY, lCalfAng + lCalfAngAdj, lCalfMass, lCalfInertia);
		lCalfShape = makeBoxShapeDef(lCalfW/2f, lCalfL/2f, lCalfFric, lCalfDensity, BODY_GROUP);

		/* THIGHS */
		rThighDef = makeBodyDef(rThighPosX, rThighPosY, rThighAng + rThighAngAdj, rThighMass, rThighInertia);
		rThighShape = makeBoxShapeDef(rThighW/2f, rThighL/2f, rThighFric, rThighDensity, BODY_GROUP);
		lThighDef = makeBodyDef(lThighPosX, lThighPosY, lThighAng + lThighAngAdj, lThighMass, lThighInertia);
		lThighShape = makeBoxShapeDef(lThighW/2f, lThighL/2f, lThighFric, lThighDensity, BODY_GROUP);

		/* TORSO */
		torsoDef = makeBodyDef(torsoPosX, torsoPosY, torsoAng + torsoAngAdj, torsoMass, torsoInertia);
		torsoShape = makeBoxShapeDef(torsoW/2f, torsoL/2f, torsoFric, torsoDensity, BODY_GROUP);

		/* HEAD */    
		headDef = makeBodyDef(headPosX, headPosY, headAng + headAngAdj, headMass, headInertia);
		headShape = makeCircleShapeDef(headR, headFric, headDensity, BODY_GROUP);

		/* UPPER ARMS */
		rUArmDef = makeBodyDef(rUArmPosX, rUArmPosY, rUArmAng + rUArmAngAdj, rUArmMass, rUArmInertia);
		rUArmShape = makeBoxShapeDef(rUArmW/2f, rUArmL/2f, rUArmFric, rUArmDensity, BODY_GROUP);
		lUArmDef = makeBodyDef(lUArmPosX, lUArmPosY, lUArmAng + lUArmAngAdj, lUArmMass, lUArmInertia);
		lUArmShape = makeBoxShapeDef(lUArmW/2f, lUArmL/2f, lUArmFric, lUArmDensity, BODY_GROUP);

		/* LOWER ARMS */  
		rLArmDef = makeBodyDef(rLArmPosX, rLArmPosY, rLArmAng + rLArmAngAdj, rLArmMass, rLArmInertia);
		rLArmShape = makeBoxShapeDef(rLArmW/2f, rLArmL/2f, rLArmFric, rLArmDensity, BODY_GROUP);
		lLArmDef = makeBodyDef(lLArmPosX, lLArmPosY, lLArmAng + lLArmAngAdj, lLArmMass, lLArmInertia);
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
	public void makeNewWorld() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {

		/******* WORLD ********/
		// Make the world object:
		Constructor<?> aabbCons = _AABB.getConstructor(_Vec2, _Vec2);
		Object vecAABBLower = makeVec2(-100f,-30f);
		Object vecAABBUpper = makeVec2(5000f,80f);
		Object aabbWorld = aabbCons.newInstance(vecAABBLower, vecAABBUpper);
		Object vecGrav = makeVec2(0f,10f);

		Constructor<?> worldCons = _World.getConstructor(_AABB, _Vec2, boolean.class);
		world = worldCons.newInstance(aabbWorld, vecGrav, true);

		// World settings:
		world.getClass().getMethod("setWarmStarting", boolean.class).invoke(world, true);
		world.getClass().getMethod("setPositionCorrection", boolean.class).invoke(world, true);
		world.getClass().getMethod("setContinuousPhysics", boolean.class).invoke(world, true);


		/******* BODIES ********/
		// Add bodies:
		trackBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, trackDef);
		trackBody.getClass().getMethod("createShape", _ShapeDef).invoke(trackBody, trackShape);

		rFootBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, rFootDef);
		rFootBody.getClass().getMethod("createShape", _ShapeDef).invoke(rFootBody, rFootShape);
		lFootBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, lFootDef);
		lFootBody.getClass().getMethod("createShape", _ShapeDef).invoke(lFootBody, lFootShape);

		rCalfBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, rCalfDef);
		rCalfBody.getClass().getMethod("createShape", _ShapeDef).invoke(rCalfBody, rCalfShape);
		lCalfBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, lCalfDef);
		lCalfBody.getClass().getMethod("createShape", _ShapeDef).invoke(lCalfBody, lCalfShape);

		rThighBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, rThighDef);
		rThighBody.getClass().getMethod("createShape", _ShapeDef).invoke(rThighBody, rThighShape);
		lThighBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, lThighDef);
		lThighBody.getClass().getMethod("createShape", _ShapeDef).invoke(lThighBody, lThighShape);

		rUArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, rUArmDef);
		rUArmBody.getClass().getMethod("createShape", _ShapeDef).invoke(rUArmBody, rUArmShape);
		lUArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, lUArmDef);
		lUArmBody.getClass().getMethod("createShape", _ShapeDef).invoke(lUArmBody, lUArmShape);

		rLArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, rLArmDef);
		rLArmBody.getClass().getMethod("createShape", _ShapeDef).invoke(rLArmBody, rLArmShape);
		lLArmBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, lLArmDef);
		lLArmBody.getClass().getMethod("createShape", _ShapeDef).invoke(lLArmBody, lLArmShape);

		torsoBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, torsoDef);
		torsoBody.getClass().getMethod("createShape", _ShapeDef).invoke(torsoBody, torsoShape);

		headBody = world.getClass().getMethod("createBody", _BodyDef).invoke(world, headDef);
		headBody.getClass().getMethod("createShape", _ShapeDef).invoke(headBody, headShape);

		if(shapeList == null){ // Grab shapes one time for the sake of plotting later.
			shapeList = new ArrayList<Object>();
			shapeList.add(torsoBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(headBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(rFootBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(lFootBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(rCalfBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(lCalfBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(rThighBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(lThighBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(rUArmBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(lUArmBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(rLArmBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(lLArmBody.getClass().getMethod("getShapeList").invoke(torsoBody));
			shapeList.add(trackBody.getClass().getMethod("getShapeList").invoke(torsoBody));
		}

		/******* JOINTS ********/
		//			makeJointDef(Object body1, Object body2, float jointPosX, float jointPosY, float lowerAngle, 
		//					float upperAngle, float maxTorque, float motorSpeed, boolean enableLimit, boolean enableMotor, boolean collideConnected)


		Object rAnkleJDef = makeJointDef(rFootBody, rCalfBody, rAnklePosX, rAnklePosY, -0.5f, 0.5f, 2000f, 0f, true, false, false);
		rAnkleJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, rAnkleJDef);
		Object lAnkleJDef = makeJointDef(lFootBody, lCalfBody, lAnklePosX, lAnklePosY, -0.5f, 0.5f, 2000f, 0f, true, false, false);
		lAnkleJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, lAnkleJDef);

		Object rKneeJDef = makeJointDef(rCalfBody, rThighBody, rKneePosX, rKneePosY, -1.3f, 0.3f, 3000f, 0f, true, true, false);
		rKneeJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, rKneeJDef);
		Object lKneeJDef = makeJointDef(lCalfBody, lThighBody, lKneePosX, lKneePosY, -1.6f, 0.0f, 3000f, 0f, true, true, false);
		lKneeJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, lKneeJDef);

		Object rHipJDef = makeJointDef(rThighBody, torsoBody, rHipPosX, rHipPosY, -1.3f, 0.7f, 6000f, 0f, true, true, false);
		rHipJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, rHipJDef);
		Object lHipJDef = makeJointDef(lThighBody, torsoBody, lHipPosX, lHipPosY, -1.5f, 0.5f, 6000f, 0f, true, true, false);
		lHipJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, lHipJDef);

		Object neckJDef = makeJointDef(headBody, torsoBody, neckPosX, neckPosY, -0.5f, 0.0f, 1000f, 0f, true, true, false);
		neckJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, neckJDef);


		Object rShoulderJDef = makeJointDef(rUArmBody, torsoBody, rShoulderPosX, rShoulderPosY, -0.5f, 1.5f, 1000f, 0f, true, true, false);
		rShoulderJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, rShoulderJDef);
		Object lShoulderJDef = makeJointDef(lUArmBody, torsoBody, lShoulderPosX, lShoulderPosY, -2f, 0.0f, 1000f, 0f, true, true, false);
		lShoulderJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, lShoulderJDef);

		Object rElbowJDef = makeJointDef(rLArmBody, rUArmBody, rElbowPosX, rElbowPosY, -0.1f, 0.5f, 0f, 10f, true, true, false);
		rElbowJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, rElbowJDef);
		Object lElbowJDef = makeJointDef(lLArmBody, lUArmBody, lElbowPosX, lElbowPosY, -0.1f, 0.5f, 0f, 10f, true, true, false);
		lElbowJ = world.getClass().getMethod("createJoint", _JointDef).invoke(world, lElbowJDef);

		world.getClass().getMethod("setContactListener", _ContactListener).invoke(world, contactListenerProxy);
	}

	/** Convenience method to avoid the verbose reflection stuff every time. 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException **/
	private void setMotorSpeed(Object joint, float motorSpeed) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		joint.getClass().getField("m_motorSpeed").setFloat(joint, motorSpeed);
	}

	/** Convenience method to avoid the verbose reflection stuff every time. 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException **/
	private void setMaxMotorTorque(Object joint, float motorTorque) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		joint.getClass().getField("m_maxMotorTorque").setFloat(joint, motorTorque);
	}

	/** Convenience method to avoid the verbose reflection stuff every time. 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException **/
	private void setJointLowerBound(Object joint, float lowerBound) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		joint.getClass().getField("m_lowerAngle").setFloat(joint, lowerBound);
	}

	/** Convenience method to avoid the verbose reflection stuff every time. 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException **/
	private void setJointUpperBound(Object joint, float upperBound) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		joint.getClass().getField("m_upperAngle").setFloat(joint, upperBound);
	}

	/** Step the game forward 1 timestep with the specified keys pressed. 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException **/
	public void stepGame(boolean q, boolean w, boolean o, boolean p) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InvocationTargetException, NoSuchMethodException{
		/* Involuntary Couplings (no QWOP presses) */
		//Neck spring torque
		//		for (Method m : rAnkleJ.getClass().getMethods()) {
		//			System.out.println(m.getName());
		//		}

		float neckAngle = (float) neckJ.getClass().getMethod("getJointAngle").invoke(neckJ);
		float neckAngularRate = (float) neckJ.getClass().getMethod("getJointSpeed").invoke(neckJ);
		float neckTorque = -neckStiff*neckAngle + 0*neckDamp*neckAngularRate;
		neckTorque = neckTorque + 0*400f*(neckAngle + 0.2f); //This bizarre term is probably a roundabout way of adjust equilibrium position.

		//Elbow spring torque

		float rElbowAngle = (float) rElbowJ.getClass().getMethod("getJointAngle").invoke(rElbowJ);
		float rElbowAngularRate = (float) rElbowJ.getClass().getMethod("getJointSpeed").invoke(rElbowJ);
		float rElbowTorque = -rElbowStiff*rElbowAngle + 0*rElbowDamp*rElbowAngularRate;

		float lElbowAngle = (float) lElbowJ.getClass().getMethod("getJointAngle").invoke(lElbowJ);
		float lElbowAngularRate = (float) lElbowJ.getClass().getMethod("getJointSpeed").invoke(lElbowJ);
		float lElbowTorque = -lElbowStiff*lElbowAngle + 0*lElbowDamp*lElbowAngularRate;

		//For now, using motors with high speed settings and torque limits to simulate springs. I don't know a better way for now.

		setMotorSpeed(neckJ, 1000*Math.signum(neckTorque));
		setMotorSpeed(rElbowJ, 1000*Math.signum(rElbowTorque));
		setMotorSpeed(lElbowJ, 1000*Math.signum(lElbowTorque));

		setMaxMotorTorque(neckJ, Math.abs(neckTorque));
		setMaxMotorTorque(rElbowJ, Math.abs(rElbowTorque));
		setMaxMotorTorque(lElbowJ, Math.abs(lElbowTorque));

		/* QW Press Stuff */
		//See spreadsheet for complete rules and priority explanations.
		if (q){
			//Set speed 1 for hips:
			setMotorSpeed(lHipJ, lHipSpeed2);
			setMotorSpeed(rHipJ, rHipSpeed2);

			//Set speed 1 for shoulders:
			setMotorSpeed(lShoulderJ, lShoulderSpeed2);
			setMotorSpeed(rShoulderJ, rShoulderSpeed2);
		}else if(w){
			//Set speed 2 for hips:
			setMotorSpeed(lHipJ, lHipSpeed1);
			setMotorSpeed(rHipJ, rHipSpeed1);

			//set speed 2 for shoulders:
			setMotorSpeed(lShoulderJ, lShoulderSpeed1);
			setMotorSpeed(rShoulderJ, rShoulderSpeed1);
		}else{
			//Set hip and ankle speeds to 0:
			setMotorSpeed(lHipJ, 0f);
			setMotorSpeed(rHipJ, 0f);

			setMotorSpeed(lShoulderJ, 0f);
			setMotorSpeed(rShoulderJ, 0f);
		}

		//Ankle/Hip Coupling -+ 0*Requires either Q or W pressed.
		if (q || w){
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
			if (rAnkleCurrX < rHipCurrX){
				setMotorSpeed(rAnkleJ, rAnkleSpeed2);
			}else{
				setMotorSpeed(rAnkleJ, rAnkleSpeed1);
			}
			// if left ankle joint is behind RIGHT hip joint (weird it's the right one here too)
			// Set its motor speed to 1;
			// else speed 2;  
			if (lAnkleCurrX < rHipCurrX){
				setMotorSpeed(lAnkleJ, lAnkleSpeed2);
			}else{
				setMotorSpeed(lAnkleJ, lAnkleSpeed1);
			}
		}

		/* OP Keypress Stuff */
		if (o){
			//Set speed 1 for knees
			// set l hip limits(-1 1)
			//set right hip limits (-1.3,0.7)
			setMotorSpeed(rKneeJ, rKneeSpeed2);
			setMotorSpeed(lKneeJ, lKneeSpeed2);

			setJointLowerBound(rHipJ, oRHipLimLo);
			setJointUpperBound(rHipJ, oRHipLimHi);
			setJointLowerBound(lHipJ, oLHipLimLo);
			setJointUpperBound(lHipJ, oLHipLimHi);

		}else if(p){
			//Set speed 2 for knees
			// set L hip limits(-1.5,0.5)
			// set R hip limits(-0.8,1.2)
			setMotorSpeed(rKneeJ, rKneeSpeed1);
			setMotorSpeed(lKneeJ, lKneeSpeed1);

			setJointLowerBound(rHipJ, pRHipLimLo);
			setJointUpperBound(rHipJ, pRHipLimHi);
			setJointLowerBound(lHipJ, pLHipLimLo);
			setJointUpperBound(lHipJ, pLHipLimHi);
		}else{
			// Set knee speeds to 0
			//Joint limits not changed!!
			setMotorSpeed(rKneeJ, 0f);
			setMotorSpeed(lKneeJ, 0f);
		}

		// Step the world forward one timestep:
		world.getClass().getMethod("step", float.class, int.class).invoke(world, timestep, iterations);

		// Extra fail conditions besides contacts. 
		float angle = (float) torsoBody.getClass().getMethod("getAngle").invoke(torsoBody);
		if (angle > torsoAngUpper || angle < torsoAngLower) { // Fail if torso angles get too far out of whack.
			isFailed = true;
		}
		timestepsSimulated++;
	}

	/** Draw this game's runner. Must provide scaling from game units to pixels, as well as pixel offsets in x and y. 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws NoSuchFieldException **/
	public void draw(Graphics g, float scaling, int xOffset, int yOffset) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {

		Object newBody = world.getClass().getMethod("getBodyList").invoke(world);

		while (newBody != null) {
			Object torsoPos = torsoBody.getClass().getMethod("getPosition").invoke(torsoBody);
			float torsoPosX = torsoPos.getClass().getField("x").getFloat(torsoPos);
			int xOffsetPixels = -(int)(scaling * torsoPosX) + xOffset; // Basic offset, plus centering x on torso.

			Object newFixture = newBody.getClass().getMethod("getShapeList").invoke(newBody);

			while(newFixture != null) {

				// Most links are polygon shapes
				Object fixtureType = newFixture.getClass().getMethod("getType").invoke(newFixture);
				if(fixtureType == _ShapeType.getField("POLYGON_SHAPE").get(null)) {
					Object newShape = _PolygonShape.cast(newFixture); // PolygonShape
					Object[] shapeVerts = (Object[]) newShape.getClass().getField("m_vertices").get(newShape); // Vec2[]

					for (int k = 0; k < newShape.getClass().getField("m_vertexCount").getInt(newShape); k++) {
						Object xf = newBody.getClass().getMethod("getXForm").invoke(newBody); // XForm

						Object ptA = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, xf, shapeVerts[k]); // Vec2
						Object ptB = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, xf, shapeVerts[(k + 1) % newShape.getClass().getField("m_vertexCount").getInt(newShape)]); // Vec2
						g.drawLine((int)(scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffsetPixels,
								(int)(scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
								(int)(scaling * ptB.getClass().getField("x").getFloat(ptB)) + xOffsetPixels,
								(int)(scaling * ptB.getClass().getField("y").getFloat(ptB)) + yOffset);			    		
					}
				}else if (fixtureType == _ShapeType.getField("CIRCLE_SHAPE").get(null)) { // Basically just head
					Object newShape = _CircleShape.cast(newFixture); // CircleShape
					float radius = newShape.getClass().getField("m_radius").getFloat(newShape);
					Object pos = newBody.getClass().getMethod("getPosition").invoke(newBody);

					g.drawOval((int)(scaling * (pos.getClass().getField("x").getFloat(pos) - radius) + xOffsetPixels),
							(int)(scaling * (pos.getClass().getField("y").getFloat(pos) - radius) + yOffset),
							(int)(scaling * radius * 2),
							(int)(scaling * radius * 2));		

				}else if(fixtureType == _ShapeType.getField("EDGE_SHAPE").get(null)) { // The track.

					Object newShape = newFixture.getClass().cast(_EdgeShape); // EdgeShape
					Object trans = newBody.getClass().getMethod("getXForm").invoke(newBody); // XForm

					Object vert1 = newShape.getClass().getMethod("getVertex1").invoke(newShape); // Vec2
					Object vert2 = newShape.getClass().getMethod("getVertex2").invoke(newShape);
					Object vert3 = newShape.getClass().getMethod("getVertex3").invoke(newShape);


					Object ptA = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, trans, vert1); // Vec2 
					Object ptB = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, trans, vert2);
					Object ptC = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, trans, vert3);

					g.drawLine((int)(scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffsetPixels,
							(int)(scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
							(int)(scaling * ptB.getClass().getField("x").getFloat(ptB)) + xOffsetPixels,
							(int)(scaling * ptB.getClass().getField("y").getFloat(ptB)) + yOffset);			    		
					g.drawLine((int)(scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffsetPixels,
							(int)(scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
							(int)(scaling * ptC.getClass().getField("x").getFloat(ptC)) + xOffsetPixels,
							(int)(scaling * ptC.getClass().getField("y").getFloat(ptC)) + yOffset);			    		

				}else{
					System.out.println("Shape type unknown.");
				}
				newFixture = newFixture.getClass().getMethod("getNext").invoke(newFixture);
			}
			newBody = newBody.getClass().getMethod("getNext").invoke(newBody);
		}

		//This draws the "road" markings to show that the ground is moving relative to the dude.
		for (int i = 0; i < markingWidth/69; i++) {
			g.drawString("_", ((-(int)(scaling * torsoPosX) - i * 70) % markingWidth) + markingWidth, yOffset + 92);
		}
	}

	/** Draw the runner at a specified set of transforms.. **/
	public void drawExtraRunner(Graphics2D g, Object[] transforms, String label, float scaling, int xOffset, int yOffset, Color drawColor, Stroke stroke) {			
		try {
			g.setColor(drawColor);
			Object bodPos = transforms[1].getClass().getField("position").get(transforms[1]);
			g.drawString(label, xOffset + (int)(bodPos.getClass().getField("x").getFloat(bodPos) * scaling) - 20, yOffset - 75);

			for (int i = 0; i < shapeList.size(); i++) {
				g.setColor(drawColor);
				g.setStroke(stroke);

				// Most links are polygon shapes
				Object shape = shapeList.get(i);
				Object fixtureType = (int)shape.getClass().getMethod("getType").invoke(shape);

				if(fixtureType == _ShapeType.getClass().getField("POLYGON_SHAPE").get(null)) {
					// Ground is black regardless.
					Object filter = shape.getClass().getField("m_filter").get(shape);
					if (filter.getClass().getField("grouIndex").getInt(filter) == 1) {
						g.setColor(Color.BLACK);
						g.setStroke(normalStroke);
					}

					Object polygonShape = _PolygonShape.cast(shape);
					Object[] polyVerts = (Object[]) polygonShape.getClass().getField("m_vertices").get(polygonShape);
					int vertexCount = (int)polygonShape.getClass().getMethod("getVertexCount").invoke(polygonShape);
					for (int j = 0; j < vertexCount; j++) { // Loop through polygon vertices and draw lines between them.

						Object ptA = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, transforms[i], polyVerts[j]); // Vec2
						Object ptB = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, transforms[i], polyVerts[(j+1) % vertexCount]); // Vec2 
						g.drawLine((int)(scaling * ptA.getClass().getField("x").getFloat(ptA)) + xOffset,
								(int)(scaling * ptA.getClass().getField("y").getFloat(ptA)) + yOffset,
								(int)(scaling * ptB.getClass().getField("x").getFloat(ptB)) + xOffset,
								(int)(scaling * ptB.getClass().getField("y").getFloat(ptB)) + yOffset);		
					}		

				}else if (fixtureType == _ShapeType.getClass().getField("CIRCLE_SHAPE").get(null)) { // Basically just head

					Object circleShape = _CircleShape.cast(shape);
					float radius = (float)circleShape.getClass().getMethod("getRadius").invoke(circleShape);
					Object circleCenter = _XForm.getMethod("mul", _XForm, _Vec2).invoke(null, transforms[i], circleShape.getClass().getMethod("getLocalPosition").invoke(circleShape)); // Vec2 	
					g.drawOval((int)(scaling * (circleCenter.getClass().getField("x").getFloat(circleCenter) - radius) + xOffset),
							(int)(scaling * (circleCenter.getClass().getField("y").getFloat(circleCenter) - radius) + yOffset),
							(int)(scaling * radius * 2),
							(int)(scaling * radius * 2));

				}else if(fixtureType == _ShapeType.getClass().getField("EDGE_SHAPE").get(null)) { // The track.

				}else{
					System.out.println("Shape type unknown.");
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	/** Print methods and fields of the object for debugging all the reflected crap in here. **/
	private void debugPrintObjectInfo(Object obj) {
		for (Field m : obj.getClass().getFields()) {
			System.out.println(m.getName());
		}
		for (Method m : obj.getClass().getMethods()) {
			System.out.println(m.getName());
		}
	}
}

//
//				isFailed = false;

//		/** QWOP initial condition. Good way to give the root node a state. **/
//		public static State getInitialState(){
//			return initState;
//		}
//
//		/** Get the runner's current state. **/
//		public State getCurrentGameState() {
//			return new State(this);
//		}
//
//		/** Is this state in failure? **/
//		public boolean getFailureStatus() {
//			return isFailed;
//		}
//
//		/** Get the number of timesteps simulated since the beginning of execution. **/
//		public long getTimestepsSimulated() {
//			return timestepsSimulated;
//		}
//
//



