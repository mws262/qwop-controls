package game.state;

import game.state.StateVariable;
import game.state.StateVariable.StateName;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

public class StateVariableTest {

    private float[] f1 = new float[] {-0.5f, 0.6f, 0.11f, 2f, -9f, 0f};
    private float[] f2 = new float[] {0.7f, 1.5f, -5.5f, 0f, 0.4f, -10f};
    private StateVariable svar1 = new StateVariable(f1);
    private StateVariable svar2 = new StateVariable(f2);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void otherConstructors() {
        List<Float> flist1 = new ArrayList<>();
        for (float f : f1) {
            flist1.add(f);
        }
        StateVariable svarOther1 = new StateVariable(flist1);
        int idx = 0;
        Assert.assertEquals(f1[idx++], svarOther1.getX(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther1.getY(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther1.getTh(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther1.getDx(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther1.getDy(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther1.getDth(), 1e-12f);

        StateVariable svarOther2 = new StateVariable(f1[0], f1[1], f1[2], f1[3], f1[4], f1[5]);
        idx = 0;
        Assert.assertEquals(f1[idx++], svarOther2.getX(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther2.getY(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther2.getTh(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther2.getDx(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther2.getDy(), 1e-12f);
        Assert.assertEquals(f1[idx++], svarOther2.getDth(), 1e-12f);
    }

    @Test
    public void badListLength() {
        List<Float> flist1 = new ArrayList<>();
        for (float f : f1) {
            flist1.add(f);
        }
        flist1.add(10f);
        exception.expect(IndexOutOfBoundsException.class);
        new StateVariable(flist1);
    }

    @Test
    public void badArrayLength() {
        exception.expect(IndexOutOfBoundsException.class);
        new StateVariable(new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f});
    }

    @Test
    public void getVars() {
        int idx = 0;
        Assert.assertEquals(f1[idx++], svar1.getX(), 1e-12f);
        Assert.assertEquals(f1[idx++], svar1.getY(), 1e-12f);
        Assert.assertEquals(f1[idx++], svar1.getTh(), 1e-12f);
        Assert.assertEquals(f1[idx++], svar1.getDx(), 1e-12f);
        Assert.assertEquals(f1[idx++], svar1.getDy(), 1e-12f);
        Assert.assertEquals(f1[idx++], svar1.getDth(), 1e-12f);

        idx = 0;
        Assert.assertEquals(f2[idx++], svar2.getX(), 1e-12f);
        Assert.assertEquals(f2[idx++], svar2.getY(), 1e-12f);
        Assert.assertEquals(f2[idx++], svar2.getTh(), 1e-12f);
        Assert.assertEquals(f2[idx++], svar2.getDx(), 1e-12f);
        Assert.assertEquals(f2[idx++], svar2.getDy(), 1e-12f);
        Assert.assertEquals(f2[idx++], svar2.getDth(), 1e-12f);
    }

    @Test
    public void getStateByName() {
        Assert.assertEquals(f1[0], svar1.getStateByName(StateName.X), 1e-6);
        Assert.assertEquals(f1[1], svar1.getStateByName(StateName.Y), 1e-6);
        Assert.assertEquals(f1[2], svar1.getStateByName(StateName.TH), 1e-6);
        Assert.assertEquals(f1[3], svar1.getStateByName(StateName.DX), 1e-6);
        Assert.assertEquals(f1[4], svar1.getStateByName(StateName.DY), 1e-6);
        Assert.assertEquals(f1[5], svar1.getStateByName(StateName.DTH), 1e-6);
    }

    @Test
    public void add() {
        StateVariable svResult = svar1.add(svar2);

        Assert.assertEquals(svar1.getX() + svar2.getX(), svResult.getX(), 1e-6f);
        Assert.assertEquals(svar1.getY() + svar2.getY(), svResult.getY(), 1e-6f);
        Assert.assertEquals(svar1.getTh() + svar2.getTh(), svResult.getTh(), 1e-6f);
        Assert.assertEquals(svar1.getDx() + svar2.getDx(), svResult.getDx(), 1e-6f);
        Assert.assertEquals(svar1.getDy() + svar2.getDy(), svResult.getDy(), 1e-6f);
        Assert.assertEquals(svar1.getDth() + svar2.getDth(), svResult.getDth(), 1e-6f);
    }

    @Test
    public void subtract() {
        StateVariable svResult = svar1.subtract(svar2);

        Assert.assertEquals(svar1.getX() - svar2.getX(), svResult.getX(), 1e-6f);
        Assert.assertEquals(svar1.getY() - svar2.getY(), svResult.getY(), 1e-6f);
        Assert.assertEquals(svar1.getTh() - svar2.getTh(), svResult.getTh(), 1e-6f);
        Assert.assertEquals(svar1.getDx() - svar2.getDx(), svResult.getDx(), 1e-6f);
        Assert.assertEquals(svar1.getDy() - svar2.getDy(), svResult.getDy(), 1e-6f);
        Assert.assertEquals(svar1.getDth() - svar2.getDth(), svResult.getDth(), 1e-6f);
    }

    @Test
    public void multiply() {
        StateVariable svResult = svar1.multiply(svar2);

        Assert.assertEquals(svar1.getX() * svar2.getX(), svResult.getX(), 1e-6f);
        Assert.assertEquals(svar1.getY() * svar2.getY(), svResult.getY(), 1e-6f);
        Assert.assertEquals(svar1.getTh() * svar2.getTh(), svResult.getTh(), 1e-6f);
        Assert.assertEquals(svar1.getDx() * svar2.getDx(), svResult.getDx(), 1e-6f);
        Assert.assertEquals(svar1.getDy() * svar2.getDy(), svResult.getDy(), 1e-6f);
        Assert.assertEquals(svar1.getDth() * svar2.getDth(), svResult.getDth(), 1e-6f);
    }

    @Test
    public void divide() {
        StateVariable svResult = svar1.divide(svar2);

        Assert.assertEquals(svar1.getX() / (svar2.getX() == 0 ? 1f : svar2.getX()), svResult.getX(), 1e-6f);
        Assert.assertEquals(svar1.getY() / (svar2.getY() == 0 ? 1f : svar2.getY()), svResult.getY(),1e-6f);
        Assert.assertEquals(svar1.getTh() / (svar2.getTh() == 0 ? 1f : svar2.getTh()), svResult.getTh(),1e-6f);
        Assert.assertEquals(svar1.getDx() / (svar2.getDx() == 0 ? 1f : svar2.getDx()), svResult.getDx(),1e-6f);
        Assert.assertEquals(svar1.getDy() / (svar2.getDy() == 0 ? 1f : svar2.getDy()), svResult.getDy(),1e-6f);
        Assert.assertEquals(svar1.getDth() / (svar2.getDth() == 0 ? 1f : svar2.getDth()), svResult.getDth(),1e-6f);
    }

    @Test
    public void testEquals() {
        StateVariable st1 = new StateVariable(0, 1, 2, 3, 4, 5);
        StateVariable st2 = new StateVariable(0, 1, 2, 3, 4, 5);
        StateVariable st3 = new StateVariable(0, 1, 2, 3, 4, 10);

        Assert.assertEquals(st1, st2);
        Assert.assertNotEquals(st1, st3);
    }

    @Test
    public void testHashCode() {
        StateVariable st1 = new StateVariable(0, 1, 2, 3, 4, 5);
        StateVariable st2 = new StateVariable(0, 1, 2, 3, 4, 5);
        StateVariable st3 = new StateVariable(0, 1, 2, 3, 4, 10);

        Assert.assertEquals(st1.hashCode(), st2.hashCode());
        Assert.assertNotEquals(st1.hashCode(), st3.hashCode());
    }
}