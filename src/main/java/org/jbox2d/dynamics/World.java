/*
 * JBox2D - A Java Port of Erin Catto's Box2D
 * 
 * JBox2D homepage: http://jbox2d.sourceforge.net/
 * Box2D homepage: http://www.box2d.org
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package org.jbox2d.dynamics;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.BroadPhase;
import org.jbox2d.collision.TOI;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.contacts.ContactEdge;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.JointEdge;

import java.io.*;

//Updated to rev 56->118->142->150 of b2World.cpp/.h

/**
 * The world that physics takes place in.
 * <BR><BR>
 * To the extent that it is possible, avoid accessing members
 * directly, as in a future version their accessibility may
 * be rolled back - as un-Java as that is, we must follow
 * upstream C++ conventions, and for now everything is public
 * to speed development of Box2d, but it is subject to change.
 * You're warned!
 */
public class World implements Externalizable {
	boolean m_lock;

	BroadPhase m_broadPhase;

	private ContactManager m_contactManager;

	private Body m_bodyList;

	/** Do not access, won't be useful! */
	Contact m_contactList;

	private Joint m_jointList;

    private int m_bodyCount;
	int m_contactCount;
	private int m_jointCount;

	private Vec2 m_gravity;

	private boolean m_allowSleep;

	private Body m_groundBody;

    /** Should we apply position correction? */
	private boolean m_positionCorrection;
	/** Should we use warm-starting?  Improves stability in stacking scenarios. */
	private boolean m_warmStarting;
	/** Should we enable continuous collision detection? */
	private boolean m_continuousPhysics;

	ContactFilter m_contactFilter;
	ContactListener m_contactListener;

	private float m_inv_dt0;

	transient private TOI timeOfImpact = new TOI();
	// Allocating island once.
	transient private Island island = new Island();
	transient private Island toiIsland = new Island();
	transient private static final int highestContacts = 5; // TODO cause these to be remade if the highest number of
	// joints/bodies/contacts
	transient private static final int highestJoints = 11; // bodies and joints should be correct for qwop.
	transient private static final int highestBodies = 14;

	transient private Body[] stack = new Body[highestBodies];
	transient private Body[] queue = new Body[highestBodies];

	transient private TimeStep subStep = new TimeStep();
	transient private TimeStep step = new TimeStep();

	/** Get the number of bodies. */
	public int getBodyCount() {
		return m_bodyCount;
	}

	/** Get the number of joints. */
	public int getJointCount() {
		return m_jointCount;
	}

	/** Get the number of contacts (each may have 0 or more contact points). */
	public int getContactCount() {
		return m_contactCount;
	}

	/** Change the global gravity vector. */
	public void setGravity(final Vec2 gravity) {
		m_gravity = gravity;
	}

	/** Get a clone of the global gravity vector.
	 * @return Clone of gravity vector
	 */
	public Vec2 getGravity() {
		return m_gravity.clone();
	}

	/** The world provides a single static ground body with no collision shapes.
	 *	You can use this to simplify the creation of joints and static shapes.
	 */
	public Body getGroundBody() {
		return m_groundBody;
	}

	/**
	 * Get the world body list. With the returned body, use Body.getNext() to get
	 * the next body in the world list. A NULL body indicates the end of the list.
	 * @return the head of the world body list.
	 */
	public Body getBodyList() {
		return m_bodyList;
	}

	/**
	 * Get the world joint list. With the returned joint, use Joint.getNext() to get
	 * the next joint in the world list. A NULL joint indicates the end of the list.
	 * @return the head of the world joint list.
	 */
	public Joint getJointList() {
		return m_jointList;
	}

	// For deserializing only.
	public World() {}

	/**
	 * Construct a world object.
	 * @param worldAABB a bounding box that completely encompasses all your shapes.
	 * @param gravity the world gravity vector.
	 * @param doSleep improve performance by not simulating inactive bodies.
	 */
	public World(final AABB worldAABB, final Vec2 gravity, final boolean doSleep) {
		m_positionCorrection = true;
		m_warmStarting = true;
		m_continuousPhysics = true;
		m_contactFilter = ContactFilter.DEFAULT_FILTER;
		m_contactListener = null;

		m_inv_dt0 = 0.0f;
		m_bodyList = null;
		m_contactList = null;
		m_jointList = null;

		m_bodyCount = 0;
		m_contactCount = 0;
		m_jointCount = 0;

		m_lock = false;
		m_allowSleep = doSleep;
		m_gravity = gravity;

		m_contactManager = new ContactManager();
		m_contactManager.m_world = this;
		m_broadPhase = new BroadPhase(worldAABB, m_contactManager);

		final BodyDef bd = new BodyDef();
		m_groundBody = createBody(bd);

		island.init(highestBodies, highestContacts, highestJoints, null);
		toiIsland.init(highestBodies, Settings.maxTOIContactsPerIsland, Settings.maxTOIJointsPerIsland, null);
	}

	/** Register a contact event listener */
	public void setContactListener(final ContactListener listener) {
		m_contactListener = listener;
		island.m_listener = listener; // To avoid creating new objects - MWS
		toiIsland.m_listener = listener;
	}

	/**
	 *  Register a contact filter to provide specific control over collision.
	 *  Otherwise the default filter is used (b2_defaultFilter).
	 */
	public void setContactFilter(final ContactFilter filter) {
		m_contactFilter = filter;
	}

	/**
	 * Create a body given a definition. No reference to the definition
	 * is retained.  Body will be static unless mass is nonzero.
	 * <BR><em>Warning</em>: This function is locked during callbacks.
	 */
	public Body createBody(final BodyDef def) {
		assert(!m_lock);
		final Body b = new Body(def, this);

		// Add to world doubly linked list.
		b.m_prev = null;
		b.m_next = m_bodyList;
		if (m_bodyList != null) {
			m_bodyList.m_prev = b;
		}
		m_bodyList = b;
		++m_bodyCount;

		return b;
	}

	/**
	 * Destroy a rigid body given a definition. No reference to the definition
	 * is retained. This function is locked during callbacks.
	 * <BR><em>Warning</em>: This automatically deletes all associated shapes and joints.
	 * <BR><em>Warning</em>: This function is locked during callbacks.
	 */
	public void destroyBody(final Body b) {
		assert(m_bodyCount > 0);
		assert(!m_lock);

		// Delete the attached joints.
		JointEdge jn = b.m_jointList;
		while (jn != null) {
			final JointEdge jn0 = jn;
			jn = jn.next;
			destroyJoint(jn0.joint);
		}

		// Delete the attached shapes. This destroys broad-phase
		// proxies and pairs, leading to the destruction of contacts.
		Shape s = b.m_shapeList;
		while (s != null) {
			final Shape s0 = s;
			s = s.m_next;
			s0.destroyProxy(m_broadPhase);
			Shape.destroy(s0);
		}

		// Remove world body list.
		if (b.m_prev != null) {
			b.m_prev.m_next = b.m_next;
		}
		if (b.m_next != null) {
			b.m_next.m_prev = b.m_prev;
		}
		if (b == m_bodyList) {
			m_bodyList = b.m_next;
		}
		--m_bodyCount;
	}

	/**
	 * Create a joint to constrain bodies together. No reference to the definition
	 * is retained. This may cause the connected bodies to cease colliding.
	 * <BR><em>Warning</em> This function is locked during callbacks.
	 */
	public Joint createJoint(final JointDef def) {
		assert(!m_lock);

		final Joint j = Joint.create(def);

		// Connect to the world list.
		j.m_prev = null;
		j.m_next = m_jointList;
		if (m_jointList != null) {
			m_jointList.m_prev = j;
		}
		m_jointList = j;
		++m_jointCount;

		// Connect to the bodies' doubly linked lists
		j.m_node1.joint = j;
		j.m_node1.other = j.m_body2;
		j.m_node1.prev = null;
		j.m_node1.next = j.m_body1.m_jointList;
		if (j.m_body1.m_jointList != null) {
			j.m_body1.m_jointList.prev = j.m_node1;
		}
		j.m_body1.m_jointList = j.m_node1;

		j.m_node2.joint = j;
		j.m_node2.other = j.m_body1;
		j.m_node2.prev = null;
		j.m_node2.next = j.m_body2.m_jointList;
		if (j.m_body2.m_jointList != null) {
			j.m_body2.m_jointList.prev = j.m_node2;
		}
		j.m_body2.m_jointList = j.m_node2;

		// If the joint prevents collisions, then reset collision filtering
		if (!def.collideConnected) {
			// Reset the proxies on the body with the minimum number of shapes.
			final Body b = def.body1.m_shapeCount < def.body2.m_shapeCount ? def.body1
			                                                               : def.body2;
			for (Shape s = b.m_shapeList; s != null; s = s.m_next) {
				s.refilterProxy(m_broadPhase, b.getMemberXForm());
			}
		}
		return j;
	}

	/**
	 * Destroy a joint. This may cause the connected bodies to begin colliding.
	 * <BR><em>Warning</em>: This function is locked during callbacks.
	 */
	public void destroyJoint(final Joint j) {
		assert(!m_lock);

		final boolean collideConnected = j.m_collideConnected;

		// Remove from the doubly linked list.
		if (j.m_prev != null) {
			j.m_prev.m_next = j.m_next;
		}

		if (j.m_next != null) {
			j.m_next.m_prev = j.m_prev;
		}

		if (j == m_jointList) {
			m_jointList = j.m_next;
		}

		// Disconnect from island graph.
		final Body body1 = j.m_body1;
		final Body body2 = j.m_body2;

		// Wake up connected bodies.
		body1.wakeUp();
		body2.wakeUp();

		// Remove from body 1
		if (j.m_node1.prev != null) {
			j.m_node1.prev.next = j.m_node1.next;
		}

		if (j.m_node1.next != null) {
			j.m_node1.next.prev = j.m_node1.prev;
		}

		if (j.m_node1 == body1.m_jointList) {
			body1.m_jointList = j.m_node1.next;
		}

		j.m_node1.prev = null;
		j.m_node1.next = null;

		// Remove from body 2
		if (j.m_node2.prev != null) {
			j.m_node2.prev.next = j.m_node2.next;
		}

		if (j.m_node2.next != null) {
			j.m_node2.next.prev = j.m_node2.prev;
		}

		if (j.m_node2 == body2.m_jointList) {
			body2.m_jointList = j.m_node2.next;
		}

		j.m_node2.prev = null;
		j.m_node2.next = null;

		Joint.destroy(j);

		assert m_jointCount > 0;
		--m_jointCount;

		// If the joint prevents collisions, then reset collision filtering.
		if (!collideConnected) {
			// Reset the proxies on the body with the minimum number of shapes.
			final Body b = body1.m_shapeCount < body2.m_shapeCount ? body1 : body2;
			for (Shape s = b.m_shapeList; s != null; s = s.m_next) {
				s.refilterProxy(m_broadPhase, b.getMemberXForm());
			}
		}
	}

	/**
	 * Take a time step. This performs collision detection, integration,
	 * and constraint solution.
	 * @param dt the amount of time to simulate, this should not vary.
	 * @param iterations the number of iterations to be used by the constraint solver.
	 */
	public void step(final float dt, final int iterations) {
		m_lock = true;

		step.dt = dt;
		step.maxIterations	= iterations;
		if (dt > 0.0f) {
			step.inv_dt = 1.0f / dt;
		} else {
			step.inv_dt = 0.0f;
		}

		step.dtRatio = m_inv_dt0 * dt;

		step.positionCorrection = m_positionCorrection;
		step.warmStarting = m_warmStarting;

		// Update contacts.
		m_contactManager.collide();

		// Integrate velocities, solve velocity constraints, and integrate positions.
		if (step.dt > 0.0f) {
			solve(step);
		}

		// Handle TOI events.
		if (m_continuousPhysics && step.dt > 0.0f) {
			solveTOI(step);
		}

		m_inv_dt0 = step.inv_dt;
		m_lock = false;
	}

	/**
	 * Query the world for all shapes that potentially overlap the
	 * provided AABB up to max count.
	 * The number of shapes found is returned.
	 * @param aabb the query box.
	 * @param maxCount the capacity of the shapes array.
	 * @return array of shapes overlapped, up to maxCount in length
	 */
	public Shape[] query(final AABB aabb, final int maxCount) {
		final Object[] objs = m_broadPhase.query(aabb, maxCount);
		final Shape[] ret = new Shape[objs.length];
		System.arraycopy(objs, 0, ret, 0, objs.length);
		return ret;
	}


	//--------------- Internals Below -------------------
	// Internal yet public to make life easier.

	// Java note: sorry, guys, we have to keep this stuff public until
	// the C++ version does otherwise so that we can maintain the engine...


	/** For internal use */
	public void solve(final TimeStep step) {
        int m_positionIterationCount = 0;

		// Size the island for the worst case.
//		final Island island = new Island();
//		island.init(m_bodyCount, m_contactCount, m_jointCount, m_contactListener);

		island.clear();

		// Clear all the island flags.
		for (Body b = m_bodyList; b != null; b = b.m_next) {
			b.m_flags &= ~Body.e_islandFlag;
		}
		for (Contact c = m_contactList; c != null; c = c.m_next) {
			c.m_flags &= ~Contact.e_islandFlag;
		}
		for (Joint j = m_jointList; j != null; j = j.m_next) {
			j.m_islandFlag = false;
		}

		// Build and simulate all awake islands.
		final int stackSize = m_bodyCount;
		for (Body seed = m_bodyList; seed != null; seed = seed.m_next) {
			if ( (seed.m_flags & (Body.e_islandFlag | Body.e_sleepFlag | Body.e_frozenFlag)) > 0){
				continue;
			}

			if (seed.isStatic()) {
				continue;
			}

			// Reset island and stack.
			island.clear();
			int stackCount = 0; // Stack creation has been moved outside to avoid huge memory issues - MWS
			stack[stackCount++] = seed;
			seed.m_flags |= Body.e_islandFlag;

			// Perform a depth first search (DFS) on the constraint graph.
			while (stackCount > 0) {
				// Grab the next body off the stack and add it to the island.
				final Body b = stack[--stackCount];
				island.add(b);

				// Make sure the body is awake.
				b.m_flags &= ~Body.e_sleepFlag;

				// To keep islands as small as possible, we don't
				// propagate islands across static bodies.
				if (b.isStatic()) {
					continue;
				}

				// Search all contacts connected to this body.
				for ( ContactEdge cn = b.m_contactList; cn != null; cn = cn.next) {
					// Has this contact already been added to an island?
					if ( (cn.contact.m_flags & (Contact.e_islandFlag | Contact.e_nonSolidFlag)) > 0) {
						continue;
					}
					// Is this contact touching?
					if (cn.contact.getManifoldCount() == 0) {
						continue;
					}

					island.add(cn.contact);
					cn.contact.m_flags |= Contact.e_islandFlag;

					// Was the other body already added to this island?
					final Body other = cn.other;
					if ((other.m_flags & Body.e_islandFlag) > 0) {
						continue;
					}

					assert stackCount < stackSize;
					stack[stackCount++] = other;
					other.m_flags |= Body.e_islandFlag;
				}

				// Search all joints connect to this body.
				for ( JointEdge jn = b.m_jointList; jn != null; jn = jn.next) {
					if (jn.joint.m_islandFlag) {
						continue;
					}

					island.add(jn.joint);
					jn.joint.m_islandFlag = true;

					final Body other = jn.other;
					if ((other.m_flags & Body.e_islandFlag) > 0) {
						continue;
					}

					assert (stackCount < stackSize);
					stack[stackCount++] = other;
					other.m_flags |= Body.e_islandFlag;
				}
			}

			island.solve(step, m_gravity, m_positionCorrection, m_allowSleep);

			m_positionIterationCount = MathUtils.max(m_positionIterationCount, island.m_positionIterationCount);

			// Post solve cleanup.
			for (int i = 0; i < island.m_bodyCount; ++i) {
				// Allow static bodies to participate in other islands.
				final Body b = island.m_bodies[i];
				if (b.isStatic()) {
					b.m_flags &= ~Body.e_islandFlag;
				}
			}
		}
		// Synchronize shapes, check for out of range bodies.
		for (Body b = m_bodyList; b != null; b = b.getNext()) {
			if ( (b.m_flags & (Body.e_sleepFlag | Body.e_frozenFlag)) != 0) {
				continue;
			}
			if (b.isStatic()) {
				continue;
			}
			// Update shapes (for broad-phase). If the shapes go out of
			// the world AABB then shapes and contacts may be destroyed,
			// including contacts that are
			final boolean inRange = b.synchronizeShapes();
		}

		// Commit shape proxy movements to the broad-phase so that new contacts are created.
		// Also, some contacts can be destroyed.
		m_broadPhase.commit();
	}

	/** For internal use: find TOI contacts and solve them. */
	private void solveTOI(final TimeStep step) {
		// Reserve an toiIsland and a stack for TOI toiIsland solution.
		// djm do we always have to make a new toiIsland? or can we make
		// it static?
		
		// Size the toiIsland for the worst case.
//		final toiIsland toiIsland = new toiIsland();
//		toiIsland.init(m_bodyCount, Settings.maxTOIContactsPertoiIsland, Settings.maxTOIJointsPertoiIsland, m_contactListener);

		toiIsland.clear();
		//Simple one pass queue
		//Relies on the fact that we're only making one pass
		//through and each body can only be pushed/popped once.
		//To push:
		//  queue[queueStart+queueSize++] = newElement
		//To pop:
		//	poppedElement = queue[queueStart++];
		//  --queueSize;
		final int queueCapacity = m_bodyCount; // Queue creation moved outside for memory issues - MWS

		for (Body b = m_bodyList; b != null; b = b.m_next) {
			b.m_flags &= ~Body.e_islandFlag;
			b.m_sweep.t0 = 0.0f;
		}

		for (Contact c = m_contactList; c != null; c = c.m_next) {
			// Invalidate TOI
			c.m_flags &= ~(Contact.e_toiFlag | Contact.e_islandFlag);
		}

		for (Joint j = m_jointList; j != null; j = j.m_next) {
			j.m_islandFlag = false;
		}

		// Find TOI events and solve them.
		while (true) {
			// Find the first TOI.
			Contact minContact = null;
			float minTOI = 1.0f;

			for (Contact c = m_contactList; c != null; c = c.m_next) {
				if ((c.m_flags & (Contact.e_slowFlag | Contact.e_nonSolidFlag)) != 0) {
					continue;
				}

				// TODO_ERIN keep a counter on the contact, only respond to M TOIs per contact.
				float toi = 1.0f;
				if ((c.m_flags & Contact.e_toiFlag) != 0) {
					// This contact has a valid cached TOI.
					toi = c.m_toi;
				} else {
					// Compute the TOI for this contact.
					final Shape s1 = c.getShape1();
					final Shape s2 = c.getShape2();
					final Body b1 = s1.getBody();
					final Body b2 = s2.getBody();

					if ((b1.isStatic() || b1.isSleeping()) && (b2.isStatic() || b2.isSleeping())) {
						continue;
					}

					// Put the sweeps onto the same time interval.
					float t0 = b1.m_sweep.t0;

					if (b1.m_sweep.t0 < b2.m_sweep.t0) {
						t0 = b2.m_sweep.t0;
						b1.m_sweep.advance(t0);
					} else if (b2.m_sweep.t0 < b1.m_sweep.t0) {
						t0 = b1.m_sweep.t0;
						b2.m_sweep.advance(t0);
					}
					assert(t0 < 1.0f);

					// Compute the time of impact.
					toi = timeOfImpact.timeOfImpact(c.m_shape1, b1.m_sweep, c.m_shape2, b2.m_sweep);
					//System.out.println(toi);
					assert(0.0f <= toi && toi <= 1.0f);

					if (toi > 0.0f && toi < 1.0f) {
						toi = MathUtils.min((1.0f - toi) * t0 + toi, 1.0f);
					}

					c.m_toi = toi;
					c.m_flags |= Contact.e_toiFlag;
				}

				if (Settings.EPSILON < toi && toi < minTOI) {
					// This is the minimum TOI found so far.
					minContact = c;
					minTOI = toi;
				}
			}

			if (minContact == null || 1.0f - 100.0f * Settings.EPSILON < minTOI) {
				// No more TOI events. Done!
				break;
			}

			// Advance the bodies to the TOI.
			final Shape s1 = minContact.getShape1();
			final Shape s2 = minContact.getShape2();
			final Body b1 = s1.getBody();
			final Body b2 = s2.getBody();
			b1.advance(minTOI);
			b2.advance(minTOI);

			// The TOI contact likely has some new contact points.
			minContact.update(m_contactListener);
			minContact.m_flags &= ~Contact.e_toiFlag;

			if (minContact.getManifoldCount() == 0) {
				// This shouldn't happen. Numerical error?
				continue;
			}

			// Build the TOI toiIsland. We need a dynamic seed.
			Body seed = b1;
			if (seed.isStatic()) {
				seed = b2;
			}

			// Reset toiIsland and queue.
			toiIsland.clear();
			//int stackCount = 0;
			int queueStart = 0; //starting index for queue
			int queueSize = 0;  //elements in queue
			queue[queueStart+queueSize++] = seed;
			seed.m_flags |= Body.e_islandFlag;

			// Perform a breadth first search (BFS) on the contact/joint graph.
			while (queueSize > 0) {
				// Grab the head body off the queue and add it to the toiIsland.
				final Body b = queue[queueStart++];
				--queueSize;

				toiIsland.add(b);

				// Make sure the body is awake.
				b.m_flags &= ~Body.e_sleepFlag;

				// To keep toiIslands as small as possible, we don't
				// propagate toiIslands across static bodies.
				if (b.isStatic()) {
					continue;
				}

				// Search all contacts connected to this body.
				for (ContactEdge cn = b.m_contactList; cn != null; cn = cn.next) {
					// Does the TOI toiIsland still have space for contacts?
					if (toiIsland.m_contactCount == toiIsland.m_contactCapacity) {
						continue;
					}

					// Has this contact already been added to an toiIsland? Skip slow or non-solid contacts.
					if ( (cn.contact.m_flags & (Contact.e_islandFlag | Contact.e_slowFlag | Contact.e_nonSolidFlag)) != 0) {
						continue;
					}

					// Is this contact touching? For performance we are not updating this contact.
					if (cn.contact.getManifoldCount() == 0) {
						continue;
					}

					toiIsland.add(cn.contact);
					cn.contact.m_flags |= Contact.e_islandFlag;
					// Update other body.
					final Body other = cn.other;

					// Was the other body already added to this toiIsland?
					if ((other.m_flags & Body.e_islandFlag) != 0) {
						continue;
					}

					// March forward, this can do no harm since this is the min TOI.
					if (!other.isStatic()) {
						other.advance(minTOI);
						other.wakeUp();
					}

					//push to the queue
					assert(queueSize < queueCapacity);
					queue[queueStart+queueSize++] = other;
					other.m_flags |= Body.e_islandFlag;
				}

				// Search all joints connect to this body.
				for ( JointEdge jn = b.m_jointList; jn != null; jn = jn.next) {
					if (toiIsland.m_jointCount == toiIsland.m_jointCapacity) {
						continue;
					}

					if (jn.joint.m_islandFlag) {
						continue;
					}

					toiIsland.add(jn.joint);

					jn.joint.m_islandFlag = true;

					final Body other = jn.other;
					if ((other.m_flags & Body.e_islandFlag) > 0) {
						continue;
					}

					if (!other.isStatic()) {
						other.advance(minTOI);
						other.wakeUp();
					}

					assert (queueSize < queueCapacity);
					queue[queueStart+queueSize++] = other;
					other.m_flags |= Body.e_islandFlag;
				}
			}

			subStep.warmStarting = false;
			subStep.dt = (1.0f - minTOI) * step.dt;
			assert(subStep.dt > Settings.EPSILON);
			subStep.inv_dt = 1.0f / subStep.dt;
			subStep.maxIterations = step.maxIterations;

			toiIsland.solveTOI(subStep);

			// Post solve cleanup.
			for (int i = 0; i < toiIsland.m_bodyCount; ++i) {
				// Allow bodies to participate in future TOI toiIslands.
				final Body b = toiIsland.m_bodies[i];
				b.m_flags &= ~Body.e_islandFlag;

				if ( (b.m_flags & (Body.e_sleepFlag | Body.e_frozenFlag)) != 0) {
					continue;
				}
				if (b.isStatic()) {
					continue;
				}
				// Update shapes (for broad-phase). If the shapes go out of
				// the world AABB then shapes and contacts may be destroyed,
				// including contacts that are
				final boolean inRange = b.synchronizeShapes();

				// Invalidate all contact TOIs associated with this body. Some of these
				// may not be in the toiIsland because they were not touching.
				for (ContactEdge cn = b.m_contactList; cn != null; cn = cn.next) {
					cn.contact.m_flags &= ~Contact.e_toiFlag;
				}
			}
			for (int i = 0; i < toiIsland.m_contactCount; ++i) {
				// Allow contacts to participate in future TOI toiIslands.
				final Contact c = toiIsland.m_contacts[i];
				c.m_flags &= ~(Contact.e_toiFlag | Contact.e_islandFlag);
			}
			for (int i=0; i < toiIsland.m_jointCount; ++i) {
				final Joint j = toiIsland.m_joints[i];
				j.m_islandFlag = false;
			}
			// Commit shape proxy movements to the broad-phase so that new contacts are created.
			// Also, some contacts can be destroyed.
			m_broadPhase.commit();
		}
	}

	/** Enable/disable warm starting. For testing. */
	public void setWarmStarting(final boolean flag) { m_warmStarting = flag; }

	/** Enable/disable position correction. For testing. */
	public void setPositionCorrection(final boolean flag) { m_positionCorrection = flag; }

	/** Enable/disable continuous physics. For testing. */
	public void setContinuousPhysics(final boolean flag) { m_continuousPhysics = flag; }

	/** Perform validation of internal data structures. */
	public void validate() {
		m_broadPhase.validate();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		out.writeBoolean(m_lock);
		out.writeObject(m_broadPhase); // BroadPhase
		out.writeObject(m_contactManager); // ContactManager
		out.writeObject(m_bodyList); // Body
		out.writeObject(m_contactList); // Contact
		out.writeObject(m_jointList); // Joint

		out.writeInt(m_bodyCount);
		out.writeInt(m_contactCount);
		out.writeInt(m_jointCount);

		out.writeObject(m_gravity); // Vec2
		out.writeBoolean(m_allowSleep);
		out.writeObject(m_groundBody); // Body

		out.writeBoolean(m_positionCorrection);
		out.writeBoolean(m_warmStarting);
		out.writeBoolean(m_continuousPhysics);

		out.writeObject(m_contactFilter);
		out.writeObject(m_contactListener);

		out.writeFloat(m_inv_dt0);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

		m_lock = in.readBoolean();
		m_broadPhase = (BroadPhase) in.readObject();
		m_contactManager = (ContactManager) in.readObject();
		m_bodyList = (Body) in.readObject();
		m_contactList = (Contact) in.readObject();
		m_jointList = (Joint) in.readObject();

		m_bodyCount = in.readInt();
		m_contactCount = in.readInt();
		m_jointCount = in.readInt();

		m_gravity = (Vec2) in.readObject();
		m_allowSleep = in.readBoolean();
		m_groundBody = (Body) in.readObject();

		m_positionCorrection = in.readBoolean();
		m_warmStarting = in.readBoolean();
		m_continuousPhysics = in.readBoolean();

		m_contactFilter = (ContactFilter) in.readObject();
		m_contactListener = (ContactListener) in.readObject();

		m_inv_dt0 = in.readFloat();

		timeOfImpact = new TOI();

		island = new Island();
		toiIsland = new Island();
		island.init(highestBodies, highestContacts, highestJoints, null);
		toiIsland.init(highestBodies, Settings.maxTOIContactsPerIsland, Settings.maxTOIJointsPerIsland, null);
		stack = new Body[highestBodies];
		queue = new Body[highestBodies];
		subStep = new TimeStep();
		step = new TimeStep();
	}
}
