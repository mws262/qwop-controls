package distributions;

import game.action.Action;
import game.qwop.CommandQWOP;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Distribution_EqualTest {

    @Test
    public void randOnDistribution() {

        Action a1 = new Action(10, CommandQWOP.NONE);
        Action a2 = new Action(11, CommandQWOP.W);
        Action a3 = new Action(12, CommandQWOP.O);
        Action a4 = new Action(13, CommandQWOP.P);
        Action a5 = new Action(14, CommandQWOP.Q);

        List<Action> actList = new ArrayList<>();
        actList.add(a1);
        actList.add(a2);
        actList.add(a3);
        actList.add(a4);
        actList.add(a5);

        Distribution<Action> distribution = new Distribution_Equal();

        List<Action> trackList = new ArrayList<>(actList);

        // Make sure that all of the game.command in the set eventually show up.
        int count = 5000;
        while (count > 0 && !trackList.isEmpty()){
            Action act = distribution.randOnDistribution(actList);
            trackList.remove(act);
            count--;
        }

        Assert.assertTrue(count > 0);
    }

    @Test
    public void equalsTest() {
        Distribution_Equal distEq1 = new Distribution_Equal();
        Distribution_Equal distEq2 = new Distribution_Equal();

        Distribution_Normal distNorm = new Distribution_Normal(5,1);

        Object object = new Object();

        Assert.assertEquals(distEq1, distEq1);
        Assert.assertEquals(distEq1, distEq2);
        Assert.assertNotEquals(distEq1, distNorm);
        Assert.assertNotEquals(distEq1, object);
    }
}