package game;

import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This is the contact listener which has been broken out of the original GameThreadSafe. I think this change worsens
 * the structure of the code, but I couldn't get the proxy class version to serialize correctly with FST, and this is
 * the workaround.
 */
public class GameThreadSafeContactListener implements ContactListener, Serializable {
    private Set<Body> contactingBodies = new HashSet<>();
    private boolean changeSinceLastCheck = false;

    @Override
    public void add(ContactPoint point) {
        contactingBodies.add(point.shape1.getBody());
        contactingBodies.add(point.shape2.getBody());
        changeSinceLastCheck = true;

    }

    @Override
    public void persist(ContactPoint point) {}

    @Override
    public void remove(ContactPoint point) {
        contactingBodies.remove(point.shape1.getBody());
        contactingBodies.remove(point.shape2.getBody());
        changeSinceLastCheck = true;
    }

    @Override
    public void result(ContactResult point) {}

    public boolean isContacting(Body body) {
        return contactingBodies.contains(body);
    }

    public boolean hasAnythingChanged() {
        boolean change = changeSinceLastCheck;
        changeSinceLastCheck = false;
        return change;
    }
}
