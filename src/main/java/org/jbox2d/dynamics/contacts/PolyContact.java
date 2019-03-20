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

package org.jbox2d.dynamics.contacts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jbox2d.collision.ContactID;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.ManifoldPoint;
import org.jbox2d.collision.shapes.CollidePoly;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.ContactListener;

//Updated to rev 142 of b2PolyContact.h/cpp
public class PolyContact extends Contact implements Serializable {

	private final Manifold m_manifold;
	private final ArrayList<Manifold> manifoldList = new ArrayList<>();
	private final CollidePoly collidePoly = new CollidePoly();

	protected PolyContact(final Shape s1, final Shape s2) {
		super(s1, s2);
		assert (m_shape1.getType() == ShapeType.POLYGON_SHAPE);
		assert (m_shape2.getType() == ShapeType.POLYGON_SHAPE);

		m_manifold = new Manifold();
		m_manifoldCount = 0;
		manifoldList.add(m_manifold);

	}

	PolyContact() {
		super();
		m_manifold = new Manifold();
		m_manifoldCount = 0;
		manifoldList.add(m_manifold);

	}

	@Override
	public Contact clone() {
		final PolyContact newC = new PolyContact(this.m_shape1, this.m_shape2);
		if (this.m_manifold != null){
			newC.m_manifold.set(this.m_manifold);
		}
		newC.m_manifoldCount = this.m_manifoldCount;
		// The parent world.
		newC.m_world = this.m_world;

		newC.m_toi = this.m_toi;

		// World pool and list pointers.
		newC.m_prev = this.m_prev;
		newC.m_next = this.m_next;

		// Nodes for connecting bodies.
		newC.m_node1.set(m_node1);
		newC.m_node2.set(m_node2);

		// Combined friction
		newC.m_friction = this.m_friction;
		newC.m_restitution = this.m_restitution;

		newC.m_flags = this.m_flags;

		return newC;
	}

	@Override
	public List<Manifold> getManifolds() {
		return manifoldList;
	}

	public static Contact create(final Shape shape1, final Shape shape2) {
		return new PolyContact(shape1, shape2);
	}

	public void dumpManifoldPoints() {
		for (int i=0; i<m_manifold.pointCount; ++i) {
			final ManifoldPoint mp = m_manifold.points[i];
			System.out.println("Manifold point dump: " + mp.normalImpulse + " " + mp.tangentImpulse);
		}
	}

	// Locally reused stuff.
    private final Manifold m0 = new Manifold();
	private final Vec2 v1 = new Vec2();
	private final ContactPoint cp = new ContactPoint();

	@Override
	public void evaluate(final ContactListener listener) {
		final Body b1 = m_shape1.getBody();
		final Body b2 = m_shape2.getBody();

		m0.set(m_manifold);

		collidePoly.collidePolygons(m_manifold, (PolygonShape) m_shape1,b1.getMemberXForm(),(PolygonShape) m_shape2,
                b2.getMemberXForm());

		final boolean[] persisted = {false, false};

		cp.shape1 = m_shape1;
		cp.shape2 = m_shape2;
		cp.friction = m_friction;
		cp.restitution = m_restitution;

		// Match contact ids to facilitate warm starting.
		if (m_manifold.pointCount > 0) {
			// Match old contact ids to new contact ids and copy the
			// stored impulses to warm start the solver.
			for (int i = 0; i < m_manifold.pointCount; ++i) {
				final ManifoldPoint mp = m_manifold.points[i];
				mp.normalImpulse = 0.0f;
				mp.tangentImpulse = 0.0f;
				boolean found = false;
				final ContactID id = mp.id;

				for (int j = 0; j < m0.pointCount; ++j) {
					if (persisted[j]) {
						continue;
					}

					final ManifoldPoint mp0 = m0.points[j];

					if (mp0.id.isEqual(id)) {
						persisted[j] = true;
						mp.normalImpulse = mp0.normalImpulse;
						mp.tangentImpulse = mp0.tangentImpulse;

						// A persistent point.
						found = true;

						// Report persistent point.
						if (listener != null) {
							b1.getWorldLocationToOut(mp.localPoint1, cp.position);
							b1.getLinearVelocityFromLocalPointToOut(mp.localPoint1, v1);
							b2.getLinearVelocityFromLocalPointToOut(mp.localPoint2, cp.velocity);
							cp.velocity.subLocal(v1);

							cp.normal.set(m_manifold.normal);
							cp.separation = mp.separation;
							cp.id.set(id);
							listener.persist(cp);
						}
						break;
					}
				}

				// Report added point.
				if (!found && listener != null) {
					b1.getWorldLocationToOut(mp.localPoint1, cp.position);
					b1.getLinearVelocityFromLocalPointToOut(mp.localPoint1, v1);
					b2.getLinearVelocityFromLocalPointToOut(mp.localPoint2, cp.velocity);
					cp.velocity.subLocal(v1);

					cp.normal.set(m_manifold.normal);
					cp.separation = mp.separation;
					cp.id.set(id);
					listener.add(cp);
				}
			}

			m_manifoldCount = 1;
		} else {
			m_manifoldCount = 0;
		}

		if (listener == null) {
			return;
		}

		// Report removed points.
		for (int i = 0; i < m0.pointCount; ++i) {
			if (persisted[i]) {
				continue;
			}

			final ManifoldPoint mp0 = m0.points[i];
			b1.getWorldLocationToOut(mp0.localPoint1, cp.position);
			b1.getLinearVelocityFromLocalPointToOut(mp0.localPoint1, v1);
			b2.getLinearVelocityFromLocalPointToOut(mp0.localPoint2, cp.velocity);
			cp.velocity.subLocal(v1);

			cp.normal.set(m_manifold.normal);
			cp.separation = mp0.separation;
			cp.id.set(mp0.id);

			listener.remove(cp);
		}
	}
}
