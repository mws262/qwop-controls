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

package org.jbox2d.common;

import java.io.Serializable;

/**
 * Primarily for internal use.
 * <BR><BR>
 * Describes the motion of a body/shape for TOI computation.
 * Shapes are defined with respect to the body origin, which may
 * not coincide with the center of mass. However, to support dynamics
 * we must interpolate the center of mass position.
 * 
 */
public class Sweep implements Serializable {

	/** Local center of mass position */
	public final Vec2 localCenter; 
	/** Center world positions */
	public final Vec2 c0, c;
	/** World angles */
	public float a0, a; 
	/** Time interval = [t0,1], where t0 is in [0,1] */
	public float t0;

	public Sweep() {
		localCenter = new Vec2();
		c0 = new Vec2();
		c = new Vec2();
	}
	
	public Sweep set(Sweep argCloneFrom){
		localCenter.set(argCloneFrom.localCenter);
		c0.set( argCloneFrom.c0);
		c.set( argCloneFrom.c);
		a0 = argCloneFrom.a0;
		a = argCloneFrom.a;
		t0 = argCloneFrom.t0;
		return this;
	}

	/**
	 * Get the interpolated transform at a specific time.
	 * @param xf the result is placed here - must not be null
	 * @param t the normalized time in [0,1].
	 */
	public void getXForm(XForm xf, float t) {
		assert(xf != null);
		if (1.0f - t0 > Settings.EPSILON) {
			float alpha = (t - t0) / (1.0f - t0);
			xf.position.x = (1.0f - alpha) * c0.x + alpha * c.x;
			xf.position.y = (1.0f - alpha) * c0.y + alpha * c.y;
			float angle = (1.0f - alpha) * a0 + alpha * a;
			xf.R.set(angle);
		} else {
			xf.position.set(c);
			xf.R.set(a);
		}

		// Shift to origin
		xf.position.x -= xf.R.col1.x * localCenter.x + xf.R.col2.x * localCenter.y;
		xf.position.y -= xf.R.col1.y * localCenter.x + xf.R.col2.y * localCenter.y;
	}

	/** 
	 * Advance the sweep forward, yielding a new initial state.
	 * @param t the new initial time.
	 */
	public void advance(float t) {
		if (t0 < t && 1.0f - t0 > Settings.EPSILON) {
			float alpha = (t - t0) / (1.0f - t0);
			c0.x = (1.0f - alpha) * c0.x + alpha * c.x;
			c0.y = (1.0f - alpha) * c0.y + alpha * c.y;
			a0 = (1.0f - alpha) * a0 + alpha * a;
			t0 = t;
		}
	}
}
