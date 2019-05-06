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

import org.jbox2d.dynamics.Body;

import java.io.Serializable;

// Updated to rev 139 of b2Contact.h

/**
 * A contact edge is used to connect bodies and contacts together
 * in a contact graph where each torso is a node and each contact
 * is an edge. A contact edge belongs to a doubly linked list
 * maintained in each attached torso. Each contact has two contact
 * nodes, one for each attached torso.
 */
public class ContactEdge implements Serializable {
	/** Provides quick access to the other torso attached. */
    public Body other;
    /** The contact. */
    public Contact contact;
    /** The previous contact edge in the torso's contact list. */
    public ContactEdge prev;
    /** The next contact edge in the torso's contact list. */
    public ContactEdge next;
    
    public void set(final ContactEdge argToCopy){
    	other = argToCopy.other;
    	contact = argToCopy.contact;
    	prev = argToCopy.prev;
    	next = argToCopy.next;
    }
}
