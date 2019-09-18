package game.state.transform;

import game.qwop.StateQWOP;
import game.state.IState;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Transform_IdentityTest {

    @Test
    public void transform() {
        ITransform tform = new Transform_Identity();

        float[] s1 = new float[72];
        float[] s2 = new float[72];
        float[] s3 = new float[72];

        for (int i = 0; i < s1.length; i++) {
            s1[i] = i;
            s2[i] = i/2f;
            s3[i] = i * 3f;
        }

        StateQWOP state1 = new StateQWOP(s1, false);
        StateQWOP state2 = new StateQWOP(s2, false);
        StateQWOP state3 = new StateQWOP(s3, false);

        List<IState> slist = new ArrayList<>();
        slist.add(state1);
        slist.add(state2);
        slist.add(state3);

        List<float[]> tformedStates = tform.transform(slist);
        Assert.assertArrayEquals(s1, tformedStates.get(0), 1e-10f);
        Assert.assertArrayEquals(s2, tformedStates.get(1), 1e-10f);
        Assert.assertArrayEquals(s3, tformedStates.get(2), 1e-10f);

    }

    @Test
    public void untransform() {

        ITransform<StateQWOP> tform = new Transform_Identity<>();

        float[] s1 = new float[StateQWOP.STATE_SIZE];
        float[] s2 = new float[StateQWOP.STATE_SIZE];
        float[] s3 = new float[StateQWOP.STATE_SIZE];

        for (int i = 0; i < s1.length; i++) {
            s1[i] = i;
            s2[i] = i/2f;
            s3[i] = i * 3f;
        }

        // Single state
        List<float[]> slist = new ArrayList<>();
        slist.add(s1);
        List<float[]> tformState = tform.untransform(slist);
        float[] state_values = tformState.get(0);

        Assert.assertArrayEquals(s1, state_values, 1e-10f);

        // Several states
        slist.clear();
        slist.add(s1);
        slist.add(s2);
        slist.add(s3);

        List<float[]> tformStateMulti = tform.untransform(slist);
        Assert.assertEquals(3, tformStateMulti.size());
        Assert.assertArrayEquals(s1, tformStateMulti.get(0),1e-10f);
        Assert.assertArrayEquals(s2, tformStateMulti.get(1),1e-10f);
        Assert.assertArrayEquals(s3, tformStateMulti.get(2),1e-10f);
    }

    @Test
    public void compressAndDecompress() {
        StateQWOP st1 = new StateQWOP(new float[72], false);
        StateQWOP st2 = new StateQWOP(new float[72], false);

        List<StateQWOP> slist = new ArrayList<>();
        slist.add(st1);
        slist.add(st2);

        ITransform<StateQWOP> tform = new Transform_Identity<>();
        List<float[]> slistReturn = tform.compressAndDecompress(slist);
        Assert.assertEquals(2, slistReturn.size());
        Assert.assertArrayEquals(slist.get(0).flattenState(), slistReturn.get(0), 1e-12f);
        Assert.assertArrayEquals(slist.get(1).flattenState(), slistReturn.get(1), 1e-12f);

    }

    @Test
    public void getOutputStateSize() {
        ITransform tform = new Transform_Identity();
        Assert.assertEquals(72, tform.getOutputSize());
    }

    @Test
    public void getName() {
        ITransform tform = new Transform_Identity();
        Assert.assertEquals("identity", tform.getName());
    }
}