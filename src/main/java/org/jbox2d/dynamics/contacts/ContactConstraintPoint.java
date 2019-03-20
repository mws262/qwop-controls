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

import org.jbox2d.common.Vec2;

//Updated to rev 89->139 of b2ContactSolver.h/.cpp
public class ContactConstraintPoint {
    final Vec2 localAnchor1 = new Vec2();
    final Vec2 localAnchor2 = new Vec2();
    final Vec2 r1 = new Vec2();
    final Vec2 r2 = new Vec2();
    public float normalImpulse;
    public float tangentImpulse;
    float positionImpulse;
    float normalMass;
    float tangentMass;
    float equalizedMass;
    public float separation;
    float velocityBias;

    ContactConstraintPoint() {}
    
    public void set(final ContactConstraintPoint cp){
    	localAnchor1.set(cp.localAnchor1);
    	localAnchor2.set(cp.localAnchor2);
    	r1.set(cp.r1);
    	r2.set(cp.r2);
    	normalImpulse = cp.normalImpulse;
    	tangentImpulse = cp.tangentImpulse;
    	positionImpulse = cp.positionImpulse;
    	normalMass = cp.normalMass;
    	tangentMass = cp.tangentMass;
        equalizedMass = cp.equalizedMass;
        separation = cp.separation;
        velocityBias = cp.velocityBias;
    }
}
