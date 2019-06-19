package distributions;

import game.action.Action;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

public class Distribution_NormalTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void randOnDistribution() {
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(10, true, false, true, false);
        Action a3 = new Action(4, false, false, true, true);
        Action a4 = new Action(6, false, false, false, true);

        List<Action> actionList = new ArrayList<>();
        actionList.add(a1);
        actionList.add(a2);
        actionList.add(a3);
        actionList.add(a4);

        // Distribution with 0 standard deviation should always produce the same value.
        Distribution<Action> distributionZeroStdev = new Distribution_Normal(5, 0);

        // See if it finds the action with duration equalling the mean.
        Action selectedAction = distributionZeroStdev.randOnDistribution(actionList);
        Assert.assertEquals(a1, selectedAction);

        // Remove the precisely equal one. It should find the first of the two nearest.
        actionList.remove(a1);
        selectedAction = distributionZeroStdev.randOnDistribution(actionList);
        Assert.assertEquals(a3, selectedAction);

        // Remove the next-closest.
        actionList.remove(a3);
        selectedAction = distributionZeroStdev.randOnDistribution(actionList);
        Assert.assertEquals(a4, selectedAction);

        // Only one remaining.
        actionList.remove(a4);
        selectedAction = distributionZeroStdev.randOnDistribution(actionList);
        Assert.assertEquals(a2, selectedAction);
    }

    @Test
    public void constructorIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);

        new Distribution_Normal(5, -1); // Only non-negative standard deviation.
    }

    @Test
    public void randOnDistributionIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);

        Distribution<Action> distribution = new Distribution_Normal(5, 1);
        distribution.randSample(new ArrayList<>()); // Empty list should not be tolerated.
    }
}