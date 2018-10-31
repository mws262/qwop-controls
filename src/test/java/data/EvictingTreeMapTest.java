package data;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EvictingTreeMapTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void put() {

        // Single element.
        EvictingTreeMap<Float, String> tmap = new EvictingTreeMap<>(1);
        tmap.put(1.f, "A");
        Assert.assertEquals(1, tmap.size());
        Assert.assertEquals("A", tmap.get(1.f));
        tmap.put(0.1f, "B");
        Assert.assertEquals(1, tmap.size());
        Assert.assertEquals("B", tmap.get(0.1f));

        // Multi-element.
        tmap = new EvictingTreeMap<>(3);
        tmap.put(1.f, "A");
        Assert.assertEquals(1, tmap.size());
        Assert.assertEquals("A", tmap.get(1.f));
        tmap.put(0.1f, "B");
        Assert.assertEquals(2, tmap.size());
        Assert.assertEquals("B", tmap.get(0.1f));
        tmap.put(3f, "C");
        Assert.assertEquals(3, tmap.size());
        Assert.assertEquals("C", tmap.get(3f));

        // Rejects a value too big when full.
        tmap.put(4f, "D");
        Assert.assertEquals(3, tmap.size());
        Assert.assertFalse(tmap.containsKey(4f));

        // Kicks out an old, bigger element.
        tmap.put(2f, "E");
        Assert.assertEquals(3, tmap.size());
        Assert.assertTrue(tmap.containsKey(2f));
        Assert.assertTrue(tmap.containsKey(1f));
        Assert.assertTrue(tmap.containsKey(0.1f));
        Assert.assertFalse(tmap.containsKey(3f));

        // Throws when trying to make with a capacity of less than 1.
        exception.expect(IllegalArgumentException.class);
        new EvictingTreeMap<>(0);
    }
}