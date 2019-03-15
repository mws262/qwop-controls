package game;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class GameClassLoader extends ClassLoader implements Serializable {
    protected Class<?> _World, _MassData, _BodyDef, _Vec2, _PolygonDef, _CircleDef, _AABB, _RevoluteJointDef, _Body,
            _ContactListener, _ShapeType, _PolygonShape, _CircleShape, _EdgeShape, _XForm, _ShapeDef, _JointDef,
            _GameThreadSafeContactListener;

    public GameClassLoader() {
        registerAsParallelCapable();
        loadClasses();
    }

    @Override
    public Class<?> findClass(String name) {

        byte[] bt = loadClassData(name);
        return defineClass(name, bt, 0, bt.length);
    }

    /**
     * Loads individual classes.
     *
     * @param className Name of a class to load. Does not need .class at the end.
     * @return Byte array containing that class's data.
     */
    private byte[] loadClassData(String className) {
        // Read class
        InputStream is = getClass().getClassLoader().getResourceAsStream(className.replace(".", "/") + ".class");
        ByteArrayOutputStream byteSt = new ByteArrayOutputStream();

        // Write into byte stream.
        int len;
        try {
            while ((len = is.read()) != -1) {
                byteSt.write(len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Convert into byte array.
        return byteSt.toByteArray();
    }

    /**
     * Loads all the classes we need to create a new game.
     * <p>
     * Some of this importing could be refined further (e.g. ordering, maybe some redundant or unnecessary classes).
     */
    private void loadClasses() {

        try {
            // I think the order of these DOES matter since if a subclass needs to load its parent, the default
            // classloader, rather than this one will be used.
            // I.e. put the most basic stuff further up the list.
            _Vec2 = findClass("org.jbox2d.common.Vec2");
            findClass("org.jbox2d.common.Sweep");
            findClass("org.jbox2d.common.Mat22");
            findClass("org.jbox2d.common.MathUtils");
            findClass("org.jbox2d.common.Settings");

            _ShapeType = findClass("org.jbox2d.collision.shapes.ShapeType");
            findClass("org.jbox2d.dynamics.contacts.Contact");
            findClass("org.jbox2d.dynamics.contacts.ContactCreateFcn");
            _ShapeDef = findClass("org.jbox2d.collision.shapes.ShapeDef");
            _AABB = findClass("org.jbox2d.collision.AABB");
            _World = findClass("org.jbox2d.dynamics.World");
            findClass("org.jbox2d.dynamics.contacts.ContactPoint");
            _ContactListener = findClass("org.jbox2d.dynamics.ContactListener");
            findClass("org.jbox2d.collision.shapes.Shape");

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
            findClass("org.jbox2d.dynamics.contacts.ContactRegister");

            findClass("org.jbox2d.collision.BroadPhase");
            findClass("org.jbox2d.collision.ContactID");
            findClass("org.jbox2d.collision.ContactID$Features");
            findClass("org.jbox2d.collision.PairManager");
            findClass("org.jbox2d.collision.BufferedPair");
            findClass("org.jbox2d.collision.Proxy");
            findClass("org.jbox2d.collision.ManifoldPoint");
            findClass("org.jbox2d.collision.Manifold");
            findClass("org.jbox2d.collision.SupportsGenericDistance");
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
            findClass("org.jbox2d.dynamics.joints.RevoluteJoint");
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

            _GameThreadSafeContactListener = findClass("game.GameThreadSafeContactListener");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
