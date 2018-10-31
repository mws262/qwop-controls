package data;

import org.jcodec.common.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LIFOFixedSizeTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    // All other methods are identical to their deque equivalents.
    @Test
    public void push() {

        // Only 1 element allowed.
        LIFOFixedSize<Integer> lifo = new LIFOFixedSize<>(1);
        lifo.push(1);

        Assert.assertEquals(1, lifo.peek());
        Assert.assertEquals(1, lifo.poll());
        Assert.assertTrue(lifo.isEmpty());

        // Push several elements.
        lifo.push(1);
        lifo.push(2);

        Assert.assertEquals(1, lifo.size());
        Assert.assertEquals(2, lifo.poll());

        // Make a several-element deque.
        lifo = new LIFOFixedSize<>(3);
        lifo.push(1);
        Assert.assertEquals(1, lifo.size());
        Assert.assertEquals(1, lifo.peek());
        lifo.push(2);
        Assert.assertEquals(2, lifo.size());
        Assert.assertEquals(2, lifo.peek());
        lifo.push(3);
        Assert.assertEquals(3, lifo.size());
        Assert.assertEquals(3, lifo.peek());

        lifo.push(4);
        Assert.assertEquals(3, lifo.size());
        Assert.assertEquals(4, lifo.peek());


        // Errors out with less than 1 element allowed.
        exception.expect(IllegalArgumentException.class);
        new LIFOFixedSize<>(0);
    }
}