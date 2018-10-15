package distributions;

import actions.Action;
import org.jcodec.common.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Distribution_EqualTest {

    @Test
    public void randOnDistribution() {

        Action a1 = new Action(10, false, false, false, false);
        Action a2 = new Action(11, false, true, false, false);
        Action a3 = new Action(12, false, false, true, false);
        Action a4 = new Action(13, false, false, false, true);
        Action a5 = new Action(14, true, false, false, false);

        List<Action> actList = new ArrayList<>();
        actList.add(a1);
        actList.add(a2);
        actList.add(a3);
        actList.add(a4);
        actList.add(a5);

        Distribution<Action> distribution = new Distribution_Equal();

        List<Action> trackList = new ArrayList<>(actList);

        // Make sure that all of the actions in the set eventually show up.
        int count = 5000;
        while (count > 0 && !trackList.isEmpty()){
            Action act = distribution.randOnDistribution(actList);
            trackList.remove(act);
            count--;
        }

        Assert.assertTrue(count > 0);
    }
}